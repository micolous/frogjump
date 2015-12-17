from protorpc import messages

class ProductVersionRequest(messages.Message):
	version_code = messages.IntegerField(1, required=True)

class ProductVersionResponse(messages.Message):
	new_version = messages.BooleanField(1, required=True)
