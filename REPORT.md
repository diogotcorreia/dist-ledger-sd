# Submission Report - Final Delivery, Group A04

This document describes the work done by our group regarding the final submission for this year's Distributed Systems project.

## Implementation description

We opted for, per the project statement's suggestion, a simplified approach to the gossip architecture, with the gossiping
itself being handled as demanded by an `Admin`'s request, with the information being replicated across the servers
in a one-to-one fashion, rather than a broadcast.

The `ServerCoordinator` now not only handles the caching of known peers, but also deals with information propagation,
sending the information to the destination replica (using gossip, of course). The `ServerState` now holds its qualifier, instead
of whether it's the primary or backup server, and holds three timestamp-related utilities:

- `gossipTimestamps`, a table with the timestamps of the last known timestamps of each replica; it's updated by the `ServerCoordinator` when it receives a gossip request;
- `valueTimestamp`, a vector clock with its current timestamp (representing all the updates it knows about);
- `replicaTimestamp`, a vector clock updated with each operation that is performed on the ledger.

Each operation now holds a `stable` field, stating whether an operation is deemed stable or not, and a `uniqueTimestamp` field,
a timestamp incrementing the client's previous timestamp, the `vectorClock` field in `UserService`, with the newly calculated
`replicaTimestamp` (as per the course's slides). The gossip's logic itself is also implemented as per the course's slides.


## Implementation options

### Ledger

The ledger entity is now a full-blown class, rather than an array stored in each server. This allows for better code abstraction,
not only interface-wise but also to "hide" operation stability details from `ServerState`.

### Vector Clocks

Vector clocks are, of course, one of the building blocks of the gossip architecture, as they're the mechanism
utilized to ensure that the information is properly replicated across the servers. We decided to implement
it under the `Common` package, with its internals being a mapping of a replica's qualifier to its timestamp,
rather than a literal vector approach. This allows us to simplify processes, as there's no natural ordering
required for the vector clock.

### Blocking Wait User-Server

We opted to implement a synchronous, blocking approach for user-server interactions. The alternative, which
would be to implement an asynchronous approach, would require the user's client to be able to, among other
things, handle multiple requests at once (and print out information at seemingly random times), which would,
in our opinion, be far from an ideal user experience. As such, we implemented a synchronous approach, where
the user can cancel (by pressing the ENTER key) any request that is currently being processed by the
server. This logic was implemented in the `User`'s `CommandParser`, in order not to have I/O handling in
the `UserService`.
