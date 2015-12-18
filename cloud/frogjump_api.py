
import endpoints

from protorpc import remote
from frogjump_api_messages import *
from os import environ

if 'DEBUG_CLIENT_ID' in environ:
	CLIENT_IDS = [environ['DEBUG_CLIENT_ID'], environ['RELEASE_CLIENT_ID'], endpoints.API_EXPLORER_CLIENT_ID]
else:
	# For endpoints generator.
	CLIENT_IDS = []
	GCM_API_KEY = None

@endpoints.api(name='frogjump', version='v1',
	description='Frogjump backend service API',
	allowed_client_ids = CLIENT_IDS)
class FrogjumpApi(remote.Service):
	@endpoints.method(ProductVersionRequest, ProductVersionResponse,
		path='version', http_method='POST',
		name='version')
	def version(self, request):
		# This is a legacy endpoint, always say we need a new version.
		return ProductVersionResponse(new_version=True)

APPLICATION = endpoints.api_server([FrogjumpApi], restricted=False)

