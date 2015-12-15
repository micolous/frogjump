from protorpc import messages

class CreateGroupRequest(messages.Message):
	gcm_token = messages.StringField(1, required=True)

class GroupResponse(messages.Message):
	success = messages.BooleanField(2)

class JoinGroupRequest(messages.Message):
	gcm_token = messages.StringField(1, required=True)
	group_id = messages.StringField(2, required=True)

class PartGroupRequest(messages.Message):
	gcm_token = messages.StringField(1, required=True)

class PostMessageRequest(messages.Message):
	gcm_token = messages.StringField(1, required=True)
	group_key = messages.StringField(2, required=True)
	latE6 = messages.IntegerField(3, required=True)
	lngE6 = messages.IntegerField(4, required=True)

class ProductVersionRequest(messages.Message):
	version_code = messages.IntegerField(1, required=True)

class ProductVersionResponse(messages.Message):
	new_version = messages.BooleanField(1, required=True)
