#!/bin/sh
# Generate the API documents for Android
GCLOUD="`which gcloud`"

if [ "x$GCLOUD" = "x" ] ; then
	echo 'gcloud is not in your PATH.  Cannot continue.'
	exit 1
fi

# Now locate the root of gcloud's installation
GCLOUD_PATH="`dirname $(dirname ${GCLOUD})`"

# Now look for endpointscfg.py
ENDPOINTSCFG="${GCLOUD_PATH}/platform/google_appengine/endpointscfg.py"

# Check that this is a real executable
if [ ! -x "${ENDPOINTSCFG}" ] ; then
	echo 'endpointscfg.py seems to be missing. Make sure you have gcloud App Engine components available.'
	echo 'Cannot continue.'
	exit 1
fi

${ENDPOINTSCFG} get_client_lib java -bs gradle frogjump_api.FrogjumpApi

