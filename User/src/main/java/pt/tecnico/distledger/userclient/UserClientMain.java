package pt.tecnico.distledger.userclient;


import lombok.CustomLog;
import pt.tecnico.distledger.common.Logger;
import pt.tecnico.distledger.userclient.grpc.UserService;

@CustomLog(topic = "UserClientMain")
public class UserClientMain {
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
            return;
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);

        try (var userService = new UserService(host, port)) {
            CommandParser parser = new CommandParser(userService);
            parser.parseInput();
        }

    }
}
