package pt.tecnico.distledger.userclient;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

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
                    case CREATE_ACCOUNT -> this.createAccount(line);
                    case DELETE_ACCOUNT -> this.deleteAccount(line);
                    case TRANSFER_TO -> this.transferTo(line);
                    case BALANCE -> this.balance(line);
                    case HELP -> this.printUsage();
                    case EXIT -> exit = true;
                    default -> {
                        System.err.printf("Command '%s' does not exist%n%n", cmd);
                        this.printUsage();
                    }
                }
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getDescription() != null) {
                    System.err.println("Error: " + e.getStatus().getDescription());
                } else {
                    System.err.println("Error: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void createAccount(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        userService.createAccount(username);
        System.out.printf("Account '%s' has been created%n", username);
    }

    private void deleteAccount(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        userService.deleteAccount(username);
        System.out.printf("Account '%s' has been deleted%n", username);
    }


    private void balance(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        final int balance = userService.balance(username);
        System.out.printf("Balance of user '%s' is %d%n", username, balance);
    }

    private void transferTo(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 5) {
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        userService.transferTo(from, dest, amount);
        if (amount == 1) {
            System.out.printf("1 coin has been transfered from account '%s' account '%s'%n", from, dest);
        } else {
            System.out.printf("%d coins have been transfered from account '%s' account '%s'%n", amount, from, dest);
        }
    }

    private void printUsage() {
        System.out.println(
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
