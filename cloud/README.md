# Frogjump Cloud

This is the source code for the application running at https://frogjump-cloud.appspot.com/

It is an App Engine application written in Python, which uses Cloud Messaging, Datastore, Endpoints and Urlfetch to do it's dirty work.

The job of this is to present an Endpoints service which Frogjump for Android can communicate with.  All the outgoing messages from the client are API calls.

## Setting up the Cloud SDK

Install gcloud.  Then setup the App Engine SDK with:

```
gcloud components install app-engine-python
```

This will set up the bits you need to deploy the application.

When running with the Free Tier, you must deploy using `appcfg` rather than `gcloud preview app`.

## Configuring secrets

This is done in the file `secrets.yaml`.  This is excluded from the git repository.  You should have a file like this:

```yaml
env_variables:
  DEBUG_CLIENT_ID: 'xxxxx.apps.googleusercontent.com'
  RELEASE_CLIENT_ID: 'xxxxx.apps.googleusercontent.com'
  GCM_API_KEY: 'xxxxx'
```