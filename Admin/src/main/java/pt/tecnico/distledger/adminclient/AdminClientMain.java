package pt.tecnico.distledger.adminclient;

import lombok.CustomLog;
import lombok.val;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.connection.CachedServerResolver;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

@CustomLog(topic = "AdminClientMain")
public class AdminClientMain {
    public static void main(String[] args) {
        Logger.setDebug(System.getProperty("debug") != null);

        // check arguments
        if (args.length != 0) {
            log.error("The admin client does not accept any arguments.");
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }

        val resolver = new CachedServerResolver<>(AdminServiceGrpc::newBlockingStub);
        try (var adminService = new AdminService(resolver)) {
            CommandParser parser = new CommandParser(adminService);
            parser.parseInput();
        }

    }
}
