package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.CustomLog;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.service.AdminDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.CrossServerDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.UserDistLedgerServiceImpl;

import java.util.OptionalInt;

@CustomLog
public class ServerMain {

    public static void main(String[] args) throws Exception {
        Logger.setDebug(System.getProperty("debug") != null);

        // Check arguments
        if (args.length != 2) {
            log.error("Argument(s) missing!");
            log.error("Usage: mvn exec:java -Dexec.args=\"<port> <server qualifier>\"");
            System.exit(1);
        }

        final OptionalInt portOpt = parsePort(args[0]);
        if (portOpt.isEmpty()) {
            log.error("Port must be a number between 1024 and 65535 ('%s' was given)", args[0]);
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

        log.info("Server started, listening on port " + port);
        log.debug("Debug mode is active");

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
