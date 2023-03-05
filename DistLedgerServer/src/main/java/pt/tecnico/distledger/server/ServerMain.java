package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.service.AdminDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.CrossServerDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.UserDistLedgerServiceImpl;

import java.util.OptionalInt;

public class ServerMain {

    public static void main(String[] args) throws Exception {

        // Check arguments
        if (args.length != 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=\"<port> <server qualifier>\"");
            return;
        }

        final OptionalInt portOpt = parsePort(args[0]);
        if (portOpt.isEmpty()) {
            System.err.println("Port must be a number between 1024 and 65535");
            System.exit(1);
        }
        final int port = portOpt.getAsInt();

        // Ignore the second argument (for now)
        // final String serverQualifier = args[1];

        ServerState serverState = new ServerState();

        final BindableService userImpl = new UserDistLedgerServiceImpl(serverState);
        final BindableService adminImpl = new AdminDistLedgerServiceImpl(serverState);
        final BindableService crossServerImpl = new CrossServerDistLedgerServiceImpl(serverState);

        Server server = ServerBuilder.forPort(port)
                .addService(userImpl)
                .addService(adminImpl)
                .addService(crossServerImpl)
                .build();

        server.start();

        System.out.println("Server started, listening on port " + port);

        server.awaitTermination();
    }

    /**
     * Parses a string as a valid non-priviledged port number (1024-65535).
     *
     * @param portStr A string containing the port number.
     * @return An empty optional if the port is invalid, or an optional wrapping the parsed port number.
     */
    private static OptionalInt parsePort(String portStr) {
        try {
            final int port = Integer.parseInt(portStr);

            if (port < 1024 || port > 65535) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(port);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
