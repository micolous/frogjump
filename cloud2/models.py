#!python
import sqlite3
from random import randint
from datetime import datetime, timedelta

MIN_GROUP_ID = 1
MAX_GROUP_ID = 999999999
GROUP_LIFE = timedelta(hours=6)
GROUP_TRIES = 10
DB_SCHEMA = """
CREATE TABLE IF NOT EXISTS groups (
	group_id integer unique,
	expiry timestamp,
	latE6 int default 0,
	lngE6 int default 0
);

CREATE TABLE IF NOT EXISTS groups_member (
	group_id integer,
	member text unique
);
"""

class FrogjumpDB(object):
	def __init__(self, db_filename):
		self._con = sqlite3.connect(db_filename, detect_types=sqlite3.PARSE_DECLTYPES|sqlite3.PARSE_COLNAMES)

		cur = self._con.cursor()
		cur.executescript(DB_SCHEMA)
		self._con.commit()

	def close(self):
		self._con.commit()
		self._con.close()
		self._con = None

	def cleanup(self):
		"""
		Delete old groups
		"""
		cur.execute('DELETE FROM groups WHERE expiry <= ?', (datetime.utcnow(),))
		cur.execute('DELETE FROM groups_member WHERE group_id NOT IN (SELECT group_id FROM groups)')
		self._con.commit()

	def create_group(self, gcm_token):
		"""
		Creates a new group with a random ID, and adds the given GCM token to it.

		Returns group_id.
		"""
		# Find an available group_id
		group_id = None
		ttl = GROUP_TRIES
		cur = self._con.cursor()
		while group_id is None:
			group_id = randint(MIN_GROUP_ID, MAX_GROUP_ID)
			cur.execute("SELECT 1 FROM groups WHERE group_id = ?", (group_id,))
			if len(cur.fetchall()):
				# group_id is already in use, roll again
				group_id = None
			ttl -= 1
			if ttl <= 0 and group_id is None:
				raise Exception, 'Failed to find an available group ID'

		cur.execute('INSERT INTO groups (group_id, expiry) VALUES (?, ?)', (group_id, datetime.utcnow() + GROUP_LIFE))
		cur.execute('INSERT INTO groups_member (group_id, member) VALUES (?, ?)', (group_id, gcm_token))
		self._con.commit()

		return group_id

	def remove_from_group(self, gcm_token):
		cur = self._con.cursor()
		cur.execute('DELETE FROM groups_member WHERE member = ?', (gcm_token,))
		self._con.commit()

	def set_group_latlng(self, group_id, latE6, lngE6):
		cur = self._con.cursor()
		cur.execute('UPDATE groups SET latE6 = ?, lngE6 = ? WHERE group_id = ?', (latE6, lngE6, group_id))
		self._con.commit()

	def get_group_latlng(self, group_id):
		"""
		Gets the last position sent to the group, as a tuple of latE6,lngE6.
		
		Returns None if no position was sent to the group, or if the group is
		unknown.
		"""
		
		cur = self._con.cursor()
		cur.execute('SELECT latE6, lngE6 FROM groups WHERE group_id = ? AND NOT (latE6 = 0 AND lngE6 = 0) LIMIT 1', (group_id,))
		for latE6, lngE6 in cur:
			return latE6, lngE6

		# Unknown
		return None

	def is_group(self, group_id, refresh_group=True):
		cur = self._con.cursor()
		cur.execute('SELECT 1 FROM groups WHERE group_id = ? AND expiry > ?', (group_id, datetime.utcnow()))
		if not len(cur.fetchall()):
			return False

		if refresh_group:
			cur.execute('UPDATE groups SET expiry = ? WHERE group_id = ?', (datetime.utcnow() + GROUP_LIFE, group_id))
			self._con.commit()

		return True

	def is_member(self, group_id, gcm_token, refresh_group=True):
		cur = self._con.cursor()
		cur.execute('SELECT 1 FROM groups_member WHERE group_id = ? AND member = ?', (group_id, gcm_token))
		if not len(cur.fetchall()):
			return False

		return self.is_group(group_id, refresh_group)

	def add_to_group(self, group_id, gcm_token):
		cur = self._con.cursor()
		if self.is_group(group_id):
			cur.execute('INSERT INTO groups_member (group_id, member) VALUES (?, ?)', (group_id, gcm_token))
			self._con.commit()
			return True

		return False

	def get_group_members(self, group_id, refresh_group=False):
		"""
		Gets a list of GCM members for a group.
		
		If the group has no members, does not exist or has expired, an empty
		list will be returned.
		
		This can renew the expiry on an existing group, but will not by default.
		"""
		if not self.is_group(group_id, refresh_group):
			print 'not a group'
			return []

		cur = self._con.cursor()
		cur.execute('SELECT member FROM groups_member WHERE group_id = ?', (group_id,))
		members = []
		for row in cur:
			members.append(row[0])

		return members

