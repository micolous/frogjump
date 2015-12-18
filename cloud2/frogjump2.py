#!/usr/bin/env python
import sys, json

from argparse import ArgumentParser, FileType
from configparser import ConfigParser
from models import FrogjumpDB
from OpenSSL import SSL, crypto
from random import randint
from traceback import print_exc
from twisted.internet import ssl
from twisted.internet.defer import Deferred
from twisted.internet.task import react
from twisted.names.srvconnect import SRVConnector
from twisted.words.xish import domish
from twisted.words.protocols.jabber import xmlstream, client
from twisted.words.protocols.jabber.jid import JID

GCM_PROD = ('gcm-xmpp.googleapis.com', 5235)
GCM_DEV = ('gcm-preprod.googleapis.com', 5236)

class Client(object):
	def __init__(self, reactor, jid, secret, server, db):
		self.reactor = reactor
		self.db = db
		f = client.XMPPClientFactory(jid, secret)
		f.addBootstrap(xmlstream.STREAM_CONNECTED_EVENT, self.connected)
		f.addBootstrap(xmlstream.STREAM_END_EVENT, self.disconnected)
		f.addBootstrap(xmlstream.STREAM_AUTHD_EVENT, self.authenticated)
		f.addBootstrap(xmlstream.INIT_FAILED_EVENT, self.init_failed)
		print 'about to connect'
		ctx = ssl.ClientContextFactory()

		reactor.connectSSL(server[0], server[1], f, ctx)

		self.finished = Deferred()


	def rawDataIn(self, buf):
		print "RECV: %s" % unicode(buf, 'utf-8').encode('ascii', 'replace')


	def rawDataOut(self, buf):
		print "SEND: %s" % unicode(buf, 'utf-8').encode('ascii', 'replace')


	def connected(self, xs):
		print 'Connected.'
		self.xmlstream = xs
		xs.addObserver('/message', self.message)
		
		# Log all traffic
		xs.rawDataInFn = self.rawDataIn
		xs.rawDataOutFn = self.rawDataOut

	def sendMessage(self, to, payload):
		message_id = str(randint(0, sys.maxint))
		body = json.dumps(dict(
			to=to,
			message_id=message_id,
			data=payload,
		))
		root = domish.Element((None, 'message'))
		root.addElement('gcm', content=body)['xmlns'] = 'google:mobile:data'
		self.xmlstream.send(root)


	def message(self, msg):
		msg = json.loads(str(msg.gcm))
		print msg

		# Lets handle the message
		if 'from' not in msg or 'data' not in msg or 'v' not in msg['data']:
			# loopback
			return

		try:
			client_version = msg['data']['v'] = int(msg['data']['v'])
			if client_version < 5 or client_version > 6:
				print 'bad client version (%d)' % client_version
				return

			action = msg['data']['a'].lower()
			if action == 'share':
				self.cmd_share(msg['from'], msg['data'])
			elif action == 'knock':
				self.cmd_knock(msg['from'], msg['data'])
			elif action == 'create':
				self.cmd_create(msg['from'])
			elif action == 'part':
				self.cmd_part(msg['from'])
			elif action == 'peek':
				self.cmd_peek(msg['from'], msg['data'])
		except:
			print 'Caught exception in message handler'
			print_exc()

	def cmd_knock(self, sender, payload):
		print 'knock: %r, %r' % (sender, payload,)
		self.db.remove_from_group(sender)
		group_id = int(payload['g'])
		if self.db.add_to_group(group_id, sender):
			self.sendMessage(sender, dict(a='join', g=str(group_id)))
		else:
			self.sendMessage(sender, dict(a='nojoin', g=str(group_id)))
	
	def cmd_share(self, sender, payload):
		print 'share: %r, %r' % (sender, payload,)
		# Validate input
		latE6 = int(payload['y'])
		lngE6 = int(payload['x'])
		if latE6 < -90000000 or latE6 > 90000000 or lngE6 < -180000000 or lngE6 > 180000000:
			# Invalid location
			print 'invalid location'
			return
		
		# Are we a member?
		group_id = int(payload['g'])
		if not self.db.is_member(group_id, sender):
			print 'not a member'
			return
	
		# Send the message
		members = self.db.get_group_members(group_id)
		print 'broadcasting to %d members' % (len(members),)
		for member in members:
			self.sendMessage(member, dict(a='goto', y=str(latE6), x=str(lngE6), d='0'))
		# Record last destination for Peek
		self.db.set_group_latlng(group_id, latE6, lngE6)

		

	def cmd_create(self, sender):
		print 'create: %r' % (sender,)
		self.db.remove_from_group(sender)
		group_id = self.db.create_group(sender)
		self.sendMessage(sender, dict(a='join', g=str(group_id)))

	def cmd_part(self, sender):
		print 'part: %r' % (sender,)
		self.db.remove_from_group(sender)

	def cmd_peek(self, sender, payload):
		print 'peek: %r, %r' % (sender, payload,)
		group_id = int(payload['g'])
		if self.db.is_member(group_id, sender):
			pos = self.db.get_group_latlng(group_id)

			if pos is None:
				# no data
				print 'no data'
				return

			# There is data
			self.sendMessage(sender, dict(a='goto', y=str(pos[0]), x=str(pos[1]), d='1'))

	def disconnected(self, xs):
		print 'Disconnected.'
		self.db.close()
		self.finished.callback(None)


	def authenticated(self, xs):
		print "Authenticated."

		presence = domish.Element((None, 'presence'))
		xs.send(presence)

		#self.reactor.callLater(5, xs.sendFooter)


	def init_failed(self, failure):
		print "Initialization failed."
		print failure

		self.xmlstream.sendFooter()



def boot(reactor, config):
	"""
	Connect to the given Jabber ID and return a L{Deferred} which will be
	called back when the connection is over.

	@param reactor: The reactor to use for the connection.
	@param config: ConfigParser containing configuration for the application
	"""
	jid = config.get('gcm', 'sender_id') + '@gcm.googleapis.com'
	secret = config.get('gcm', 'key')
	server = GCM_PROD if config.getboolean('gcm', 'prod') else GCM_DEV

	# Database
	db_filename = config.get('db', 'path')
	db = FrogjumpDB(db_filename)

	print 'getting client'
	return Client(reactor, JID(jid), secret, server, db).finished


def main():
	parser = ArgumentParser()
	parser.add_argument('-c', '--config',
		required=True,
		type=FileType('rb'))

	options = parser.parse_args()
	
	# read the config file
	config = ConfigParser()
	config.read_dict({
		'gcm': {
			'prod': 'false'
		}
	})
	config.readfp(options.config)

	react(boot, [config])

if __name__ == '__main__':
	main()

