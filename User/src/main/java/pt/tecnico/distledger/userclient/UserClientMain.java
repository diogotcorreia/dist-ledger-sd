package pt.tecnico.distledger.userclient;


import lombok.CustomLog;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.userclient.grpc.UserService;

@CustomLog(topic = "UserClientMain")
public class UserClientMain {
    public static void main(String[] args) {
        Logger.setDebug(System.getProperty("debug") != null);

        // check arguments
        if (args.length != 0) {
            log.error("Argument(s) present!");
            log.error("Usage: mvn exec:java");
            System.exit(1);
        }

        try (var userService = new UserService()) {
            CommandParser parser = new CommandParser(userService);
            parser.parseInput();
        }

    }
}
