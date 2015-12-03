# Frogjump Cloud

This is the source code for the application running at https://frogjump-cloud.appspot.com/

It is an App Engine application written in Python, which uses Cloud Messaging, Datastore, Endpoints and Urlfetch to do it's dirty work.

The job of this is to present an Endpoints service which Frogjump for Android can communicate with.  All the outgoing messages from the client are API calls.

