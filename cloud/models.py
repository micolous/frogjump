
from google.appengine.ext import ndb
from random import randint
from datetime import datetime, timedelta

MIN_GROUP_ID = 1
MAX_GROUP_ID = 999999999
GROUP_LIFE = timedelta(hours=6)
GROUP_TRIES = 10

class Group(ndb.Model):
	# This is the 9-digit "public" identifier for the record
	group_id = ndb.IntegerProperty(required=True)

	# When this group should expire. This is 1 hour after the last message sent.
	# This value is represented in UTC.
	expiry = ndb.DateTimeProperty(required=True)

	# What GCM device identifiers are a member of this group?
	members = ndb.StringProperty(repeated=True)

	@classmethod
	def create(cls, gcm_token):
		"""
		Create a new group with a random group ID, and add a given GCM token to
		it.

		Returns: tuple of (key, group)
		"""
		# Find an available group_id
		group_id = None
		ttl = GROUP_TRIES
		while group_id is None:
			group_id = randint(MIN_GROUP_ID, MAX_GROUP_ID)
			if cls.query(cls.group_id == group_id).iter(keys_only=True).has_next():
				# group_id is already in use, roll again
				group_id = None
			ttl -= 1
			if ttl <= 0:
				raise Exception, 'Failed to find an available group ID'

		group = cls(group_id=group_id, expiry=datetime.utcnow() + GROUP_LIFE, members=[gcm_token])
		return group.put(), group

	@classmethod
	def cleanup(cls):
		"""
		Delete old group IDs
		"""
		query = cls.query(cls.expiry <= datetime.utcnow()).iter(keys_only=True)
		for key in query:
			key.delete()

	@classmethod
	def remove_by_gcm_token(cls, gcm_token):
		for group in cls.query(cls.members == gcm_token):
			try:
				group.members.remove(gcm_token)
			except ValueError:
				# not in group, shouldn't get here...
				continue

			group.put()

	@classmethod
	def get_for_member(cls, key, gcm_token):
		#print 'key = %s' % key
		#print 'gcm_token = %s' % gcm_token
		group = cls.get_by_id(key)
		if group is None:
			#print 'invalid group'
			return None

		# Validate that the group is not expired
		if group.expiry <= datetime.utcnow():
			#print 'expired group'
			return None

		# Check that we are a member of this group
		if gcm_token not in group.members:
			#print 'not a member of group'
			return None

		# Extend the expiry and return the object
		group.expiry = datetime.utcnow() + GROUP_LIFE
		group.put()
		return group

	@classmethod
	def add_to_group(cls, gcm_token, group_id):
		# Check that there is a valid group ID available
		group = cls.query(cls.group_id == long(group_id), cls.expiry >= datetime.utcnow()).get()
		if group == None:
			return None

		# We have a group, add the member and renew it's lifetime.
		group.members.append(gcm_token)
		group.expiry = datetime.utcnow() + GROUP_LIFE
		group.put()

		return group

