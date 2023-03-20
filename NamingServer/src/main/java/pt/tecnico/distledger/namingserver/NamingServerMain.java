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

    public static void main(String[] args) throws Exception {
        Logger.setDebug(System.getProperty("debug") != null);

        if (args.length != 0) {
            log.error("Argument(s) present!");
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }

        final int namingServerPort = 5001;

        final NamingServer namingServer = new NamingServer();
        final BindableService namingServerService = new NamingServerServiceImpl(namingServer);

        Server server = ServerBuilder.forPort(namingServerPort)
                .addService(namingServerService)
                .build();

        server.start();

        log.info("Naming Server started, listening on port " + namingServerPort);
        log.debug("Debug mode is active");

        server.awaitTermination();
    }
}