package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.CustomLog;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.tecnico.distledger.namingserver.service.NamingServerServiceImpl;

@CustomLog
public class NamingServerMain {

    private static final int PORT = 5001;

    public static void main(String[] args) throws Exception {
        Logger.setDebug(System.getProperty("debug") != null);

        if (args.length != 0) {
            log.error("The Naming Server does not accept any arguments.");
            log.error("The Server is located at localhost:" + PORT);
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }


        final NamingServer namingServer = new NamingServer();
        final BindableService namingServerService = new NamingServerServiceImpl(namingServer);

        Server server = ServerBuilder.forPort(PORT)
                .addService(namingServerService)
                .build();

        server.start();

        log.info("Naming Server started, listening on port " + PORT);
        log.debug("Debug mode is active");

        server.awaitTermination();
    }

}
