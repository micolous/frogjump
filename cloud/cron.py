
from models import Group
import webapp2

class CleanupPage(webapp2.RequestHandler):
	def get(self):
		self.response.headers['Content-Type'] = 'text/plain'
		Group.cleanup()
		self.response.write('OK')

app = webapp2.WSGIApplication([
	('/cron/cleanup', CleanupPage),
])

