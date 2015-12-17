# Protocol documentation

This documents the GCM protocol used for Frogjump.

## Common message attributes

* `a`: Action (message type). This is case sensitive in 
* `v`: Client version (used for client to server messages only). This corresponds to the build number in use on the client. This is used for backwards compatibility -- this means the server can respond with an earlier protocol level where it is backwards incompatible.

All locations are specified in *decimal degrees* multiplied by 10^6. The co-ordinates use the WGS84 CRS, north and east are positive, south and west are negative.

## Client to server messages

These are listed by their `a` value (action).  They all have the common attributes above.

### `knock` (Request to join group)

* `g`: Group ID to join.

Server should respond with a `join` or `nojoin` message.

### `create` (Create new group)

This has no extra parameters. The client will automatically be added to the group created.

### `share` (Share location with group)

* `g`: Group ID. This must match the group ID the client has joined, otherwise the message is rejected.
* `x`: Longitude of the location.
* `y`: Latitude of the location.

Server should respond with a `goto` message directed at all clients in the group, if successful.

### `peek` (Get last location)

* `g`: Group ID. This must match the group ID the client has joined, otherwise the message is rejected.

Server should respond to this query with a unicast `goto` with `d=1`.

### `part` (Leave group)

This has no extra parameters. The client will be removed from any group they are a member of.

The server should respond with a `kick` message.

## Server to client messages

### `join` (Client has been added to a group)

* `g`: Group ID. This must be an integer.

Note: older versions of the protocol call this parameter `i`, and include additional parameter `k` for communicating with the older Endpoints API.

### `goto` (Navigate to the given location)

* `x`: Longitude of the destination.
* `y`: Latitude of the destination.
* `d`: If >= 1, then don't activate auto-navigation on the client. This is used to populate UI lists in response to a `peek` message. If this parameter is missing, it is assumed to be 0.

### `nojoin` (Client could not be added to a group)

* `g`: Group ID that the client attempted to join.


