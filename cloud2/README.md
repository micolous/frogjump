# Frogjump cloud2 server

This is a prototype of rebuilding the Frogjump Cloud components to use XMPP communications only with GCM instead of App Engine and GCM.

This means all the API messages get passed over this server interface instead of Cloud Endpoints.

It runs as a Twisted application service, which has an XMPP client for communicating with GCM.  In the `cloud2` protocol version of Frogjump, the only API calls used are for version checks.

The advantage of this is it helps ties messages more strongly between clients' GCM authentication details and the server side.

For protocol details, see [protocol.md](../protocol.md).

## Configuration parameters

```ini
[gcm]
; Use the production GCM service?
prod = yes

; Sender ID to use, excluding the domain segment.
sender_id = 1234567890

; Secret key to use
key = ABCDEFABCDEF

[db]
; Database path
path = frogjump2.db3
```

