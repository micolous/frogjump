
import endpoints

from protorpc import remote
from frogjump_api_messages import *
from models import Group
from gcm import GcmApi, GcmException
from os import environ

if 'GCM_API_KEY' in environ:
	CLIENT_IDS = [environ['DEBUG_CLIENT_ID'], environ['RELEASE_CLIENT_ID'], endpoints.API_EXPLORER_CLIENT_ID]
	GCM_API_KEY = environ['GCM_API_KEY']
else:
    # For endpoints generator.
	CLIENT_IDS = []
	GCM_API_KEY = None

@endpoints.api(name='frogjump', version='v1',
	description='Frogjump backend service API',
	allowed_client_ids = CLIENT_IDS)
class FrogjumpApi(remote.Service):

	@endpoints.method(CreateGroupRequest, GroupResponse,
		path='group/create', http_method='POST',
		name='group.create')
	def group_create(self, request):
		# We should now validate the token given
		gcm = GcmApi(GCM_API_KEY)
		try:
			gcm.send(request.gcm_token, dict(a='Ping'))
		except GcmException:
			# Probably an invalid token, bail
			return GroupResponse(success=False)

		Group.remove_by_gcm_token(request.gcm_token)

		# We got to here, so the GCM token is good, lets create a channel in
		# datastore with a mapping of GroupID => SecretID
		key, group = Group.create(request.gcm_token)

		# Send a GCM message to the client asking them to join a specified group
		gcm.send(request.gcm_token, dict(a='Join', k=key.id(), i=group.group_id))

		# Lets then send the GroupID and SecretID to the client
		return GroupResponse(success=True)

	@endpoints.method(JoinGroupRequest, GroupResponse,
		path='group/join', http_method='POST',
		name='group.join')
	def group_join(self, request):
		# We should now validate the token given
		gcm = GcmApi(GCM_API_KEY)
		try:
			gcm.send(request.gcm_token, dict(a='Ping'))
		except GcmException:
			# Probably an invalid token, bail
			return GroupResponse(success=False)

		Group.remove_by_gcm_token(request.gcm_token)
		group = Group.add_to_group(request.gcm_token, request.group_id)
		if group is None:
			# invalid group id
			return GroupResponse(success=False)

		# Send a GCM message to the client asking them to join the group
		gcm.send(request.gcm_token, dict(a='Join', k=group.key.id(), i=group.group_id))

		return GroupResponse(success=True)

	@endpoints.method(PartGroupRequest, GroupResponse,
		path='group/part', http_method='POST',
		name='group.part')
	def group_part(self, request):
		# Don't worry about validating the token.
		Group.remove_by_gcm_token(request.gcm_token)

		return GroupResponse(success=True)

	@endpoints.method(PostMessageRequest, GroupResponse,
		path='group/post', http_method='POST',
		name='group.post')
	def group_post(self, request):
		gcm = GcmApi(GCM_API_KEY)

		# Validate the lat/lng to make sure they are sensible
		if request.latE6 < -90000000 or request.latE6 > 90000000 or request.lngE6 < -180000000 or request.lngE6 > 180000000:
			return GroupResponse(success=False)

		# Make sure that this token is already a member of the group
		group = Group.get_for_member(int(request.group_key), request.gcm_token)
		if group is None:
			return GroupResponse(success=False)

		# Now post the broadcast to the group
		gcm.send(group.members, dict(a='Goto', y=request.latE6, x=request.lngE6))

		return GroupResponse(success=True)

	@endpoints.method(ProductVersionRequest, ProductVersionResponse,
		path='version', http_method='POST',
		name='version')
	def version(self, request):
		if request.version_code >= 3:
			return ProductVersionResponse(new_version=False)
		return ProductVersionResponse(new_version=True)

APPLICATION = endpoints.api_server([FrogjumpApi], restricted=False)

