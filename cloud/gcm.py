from google.appengine.api import urlfetch
import json

GCM_API_URL = 'https://gcm-http.googleapis.com/gcm/send'

class GcmException(Exception): pass


class GcmApi(object):
	def __init__(self, api_key):
		self._api_key = api_key

	def send(self, device_tokens, data, collapse_key=None, delay_while_idle=None, ttl=None):
		if isinstance(device_tokens, str) or isinstance(device_tokens, unicode):
			device_tokens = [device_tokens]

		assert len(device_tokens) >= 1, 'need at least one target'
		o = dict(data=data)

		if len(device_tokens) == 1:
			o['to'] = device_tokens[0]
		else:
			o['registration_ids'] = device_tokens
		if collapse_key is not None:
			o['collapse_key'] = collapse_key
		if delay_while_idle is not None:
			o['delay_while_idle'] = delay_while_idle
		if ttl is not None:
			o['time_to_live'] = ttl

		o = json.dumps(o)
		headers = {
			'Authorization': 'key=' + self._api_key,
			'Content-Type': 'application/json'
		}

		result = urlfetch.fetch(url=GCM_API_URL, payload=o, method=urlfetch.POST, headers=headers)
		if result.status_code != 200:
			raise GcmException, ('Non-200 response code (%d)' % (result.status_code))

		response = json.loads(result.content)
		#print 'gcm response = %r' % (response,)
		if response['success'] == 0:
			raise GcmException, 'No successful notifications sent'

