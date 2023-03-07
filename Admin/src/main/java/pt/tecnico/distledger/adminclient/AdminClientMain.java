package pt.tecnico.distledger.adminclient;

import lombok.CustomLog;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;

@CustomLog(topic = "AdminClientMain")
public class AdminClientMain {
    public static void main(String[] args) {
        Logger.setDebug(System.getProperty("debug") != null);

        // receive and print arguments
        log.debug("Received %d arguments", args.length);
        for (int i = 0; i < args.length; i++) {
            log.debug("arg[%d] = %s", i, args[i]);
        }

        // check arguments
        if (args.length != 2) {
            log.error("Argument(s) missing!");
            log.error("Usage: mvn exec:java -Dexec.args=<host> <port>");
            System.exit(1);
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);

        try (var adminService = new AdminService(host, port)) {
            CommandParser parser = new CommandParser(adminService);
            parser.parseInput();
        }

    }
}
