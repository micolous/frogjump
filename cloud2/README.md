# Frogjump cloud2 server

This is a prototype of rebuilding the Frogjump Cloud components to use XMPP communications only with GCM instead of App Engine and GCM.

This means all the API messages get passed over this server interface instead of Cloud Endpoints.

The advantage of this is it helps ties messages more strongly between clients' GCM authentication details and the server side.

## Configuration parameters

```ini
[frogjump2]
prod = yes
sender_id = 1234567890
key = ABCDEFABCDEF
```

