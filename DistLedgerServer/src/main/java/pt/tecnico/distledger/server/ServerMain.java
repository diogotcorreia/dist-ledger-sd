package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.service.AdminDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.CrossServerDistLedgerServiceImpl;
import pt.tecnico.distledger.server.service.UserDistLedgerServiceImpl;

public class ServerMain {

    public static void main(String[] args) throws Exception {

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

        final BindableService userImpl = new UserDistLedgerServiceImpl();
        final BindableService adminImpl = new AdminDistLedgerServiceImpl();
        final BindableService crossServerImpl = new CrossServerDistLedgerServiceImpl();

        Server server = ServerBuilder.forPort(port)
                .addService(userImpl)
                .addService(adminImpl)
                .addService(crossServerImpl)
                .build();

        server.start();

        System.out.println("Server started, listening on " + port);

        server.awaitTermination();
    }
}
