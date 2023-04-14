# DistLedger

Distributed Systems Project 2022/2023

## Authors

**Group A04**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name          | User                               | Email                                            |
|--------|---------------|------------------------------------|--------------------------------------------------|
| 99207  | Diogo Gaspar  | <https://github.com/randomicecube> | <mailto:diogo.marques.gaspar@tecnico.ulisboa.pt> |
| 99211  | Diogo Correia | <https://github.com/diogotcorreia> | <mailto:diogo.t.correia@tecnico.ulisboa.pt>      |
| 99341  | Tomás Esteves | <https://github.com/Pesteves2002>  | <mailto:tomasesteves2002@tecnico.ulisboa.pt>     |

## Getting Started

The overall system is made up of several modules. The server's logic is implemented in _DistLedgerServer_. The clients are the _User_ 
and the _Admin_. The definition of messages and services is in the _Contract_. The naming server
is the _NamingServer_. There's also a _Common_ module, where our custom Logger and Server Resolvers
are implemented, along with a Vector Clock implementation.
Note that the _DistLedgerServer_ also contains the logic for the _ServerCoordinator_, which is responsible for
coordinating all the server replicas, ensuring proper information replication with the gossip architecture.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

### Running

Run each module (one of user, admin, server and name server) by going into its directory and then running:

```s
mvn exec:java
```

Optionally, you can customize the given arguments (note that some modules don't accept them) and turn on debug messages:

```s
mvn exec:java -Dexec.args="<args here>" -Ddebug
```

#### Running tests

To run tests, run the following in the project root directory, or alternatively on one of the modules:

```s
mvn test
```

### Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) to ensure code formatting rules
are followed.

It is possible to check if the code adheres to the formatting rules by running:

```s
mvn spotless:check
```

Or, alternatively, format the code:

```s
mvn spotless:apply
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
