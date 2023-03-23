package pt.tecnico.distledger.userclient;

import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.tecnico.distledger.common.exceptions.ServerUnresolvableException;
import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.Scanner;

@CustomLog
public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String CREATE_ACCOUNT_ALIAS = "c";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String DELETE_ACCOUNT_ALIAS = "d";
    private static final String TRANSFER_TO = "transferTo";
    private static final String TRANSFER_TO_ALIAS = "t";
    private static final String BALANCE = "balance";
    private static final String BALANCE_ALIAS = "b";
    private static final String HELP = "help";
    private static final String HELP_ALIAS = "h";
    private static final String EXIT = "exit";
    private static final String EXIT_ALIAS = "e";

    private final UserService userService;

    public CommandParser(UserService userService) {
        this.userService = userService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try {
                switch (cmd) {
                    case CREATE_ACCOUNT, CREATE_ACCOUNT_ALIAS -> this.createAccount(line);
                    case DELETE_ACCOUNT, DELETE_ACCOUNT_ALIAS -> this.deleteAccount(line);
                    case TRANSFER_TO, TRANSFER_TO_ALIAS -> this.transferTo(line);
                    case BALANCE, BALANCE_ALIAS -> this.balance(line);
                    case HELP, HELP_ALIAS -> this.printUsage();
                    case EXIT, EXIT_ALIAS -> exit = true;
                    default -> {
                        log.error("Command '%s' does not exist%n%n", cmd);
                        this.printUsage();
                    }
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getDescription() != null) {
                    log.error(e.getStatus().getDescription());
                } else {
                    log.error(e.getMessage());
                }
                if (e.getStatus().getCause() != null) {
                    e.getStatus().getCause().printStackTrace();
                }
            } catch (ServerUnresolvableException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void createAccount(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        userService.createAccount(server, username);
        log.debug("Account '%s' has been created%n", username);
        log.info("OK%n");
    }

    private void deleteAccount(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        userService.deleteAccount(server, username);
        log.debug("Account '%s' has been deleted%n", username);
        log.info("OK%n");
    }

    private void balance(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        final int balance = userService.balance(server, username);
        log.debug("Balance of user '%s' is %d%n", username, balance);
        log.info("OK");
        if (balance > 0) {
            log.info("%d", balance);
        }
        log.info("");
    }

    private void transferTo(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 5) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        userService.transferTo(server, from, dest, amount);
        if (amount == 1) {
            log.debug("1 coin has been transferred from account '%s' account '%s'%n", from, dest);
        } else {
            log.debug("%d coins have been transferred from account '%s' account '%s'%n", amount, from, dest);
        }
        log.info("OK%n");
    }

    private void printUsage() {
        log.info(
                """
                        Usage:
                        - createAccount <server> <username>
                        - deleteAccount <server> <username>
                        - balance <server> <username>
                        - transferTo <server> <username_from> <username_to> <amount>
                        - exit
                        """
        );
    }
}
