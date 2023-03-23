package pt.tecnico.distledger.userclient;


import lombok.CustomLog;
import lombok.val;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.common.connection.CachedServerResolver;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

@CustomLog(topic = "UserClientMain")
public class UserClientMain {
    public static void main(String[] args) {
        Logger.setDebug(System.getProperty("debug") != null);

        // check arguments
        if (args.length != 0) {
            log.error("The Client does not accept any arguments.");
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }

        val serverResolver = new CachedServerResolver<>(UserServiceGrpc::newBlockingStub);
        try (var userService = new UserService(serverResolver)) {
            CommandParser parser = new CommandParser(userService);
            parser.parseInput();
        }

    }
}
