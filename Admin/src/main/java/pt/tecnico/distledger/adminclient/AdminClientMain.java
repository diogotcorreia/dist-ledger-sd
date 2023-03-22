package pt.tecnico.distledger.adminclient;

import lombok.CustomLog;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;

@CustomLog(topic = "AdminClientMain")
public class AdminClientMain {
    public static void main(String[] args) {
        Logger.setDebug(System.getProperty("debug") != null);

        // check arguments
        if (args.length != 0) {
            log.error("Argument(s) present!");
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }

        try (var adminService = new AdminService()) {
            CommandParser parser = new CommandParser(adminService);
            parser.parseInput();
        }

    }
}
