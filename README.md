# Frogjump #

**Frogjump** is a remote control program for Google Maps for Android.

It consists of an Android application, which can both send and recieve maps links, and an [App Engine (GAE)](https://cloud.google.com/appengine/) backend which acts as a broker for groups.  It uses [Google Cloud Messaging (GCM)](https://developers.google.com/cloud-messaging/) in order to broadcast messages to groups, and [Cloud Endpoints](https://cloud.google.com/endpoints/) to connect between the Android and GAE.

This allows (mostly) hands-free remote control of Google Maps for Android navigation, so that a driver can use their mobile phone with Google Maps while the passengers set the destination.

There is also support for as-the-crow-flies navigation with [GPS Status and Toolbox](https://play.google.com/store/apps/details?id=com.eclipsim.gpsstatus2).

## How it works ##

The Android application starts by registering to receive messages with GCM.

Whether an existing group ID is joined, or a new group is created, a REST request is made to the GAE backend, containing the GCM client identifier.  The group is looked up or created in GAE datastore, and the GCM client identifier is added to the group.

The GAE backend will then send a GCM message telling the client that it is now part of the group (Join).  The client responds by changing view, and offering options for changing navigation type (off, driving, cycling, walking, as the crow flies).

All clients will hook the `geo:` URL by way of intents, as well as Google Maps URLs.  When one of these links is opened on the device, the option is given to broadcast the target by Frogjump.  When a client broadcasts, it sends a message to the GAE backend, asking for that message to be broadcast to all GCM clients that are part of this group.

Clients then listen for an incoming GCM message (Goto), which contains latitude and longitude of the destination.  When a message is received, it is processed according to local navigation preferences, then an intent is launched to start up Google Maps with the appropriate modes.

If Google Maps already has navigation running, it will prompt the user to allow for the change of destination.

## Caveats ##

* Does not hook the "share" function inside Google Maps itself, because this doesn't use `geo:` URLs.
* It isn't possible to see recent messages sent to the group.
* It isn't possible to see who is in the group.
* Only supports Google Maps. Other apps can be implemented where they have similar hands-free intents.
* If navigation is in progress, Google Maps needs confirmation to change destination.
* GCM registration is fragile.

