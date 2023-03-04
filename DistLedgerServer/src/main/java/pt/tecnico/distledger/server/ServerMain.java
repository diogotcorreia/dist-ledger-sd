package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerMain {

    // Server host port
    private static int port;

    public static void main(String[] args) {

        // Check arguments
        if (args.length != 2) {
            // TODO: change to constant
            System.err.println("Number of arguments must be 2");
            System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        if (port < 1024 || port > 65535) {
            System.err.println("Port must be between 1024 and 65535");
            return;
        }

        // Ignore the second argument (for now)

        final BindableService impl = new DistLedgerImpl();

        Server server = ServerBuilder().forPort(port).addService(impl).build();

        server.start();

        System.out.println("Server started, listening on " + port);

        server.awaitTermination();
    }
}
