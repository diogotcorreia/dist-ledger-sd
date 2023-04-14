package pt.tecnico.distledger.userclient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.tecnico.distledger.common.exceptions.ServerUnresolvableException;
import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String CREATE_ACCOUNT_ALIAS = "c";
    private static final String TRANSFER_TO = "transferTo";
    private static final String TRANSFER_TO_ALIAS = "t";
    private static final String BALANCE = "balance";
    private static final String BALANCE_ALIAS = "b";
    private static final String HELP = "help";
    private static final String HELP_ALIAS = "h";
    private static final String EXIT = "exit";
    private static final String EXIT_ALIAS = "e";

    private final UserService userService;

    private final Thread mainThread;
    private Thread runningTask;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CommandParser(UserService userService) {
        this.userService = userService;
        this.mainThread = Thread.currentThread();
    }

    void parseInput() {
        boolean exit = false;
        Scanner scanner = new Scanner(System.in);

        System.out.print("> ");
        while (!exit) {
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (running.get()) {
                // There is a running task in the foreground, kill it
                runningTask.interrupt();
                if (line.isBlank()) {
                    continue;
                }
            }
            String cmd = line.split(SPACE)[0];

            switch (cmd) {
                case CREATE_ACCOUNT, CREATE_ACCOUNT_ALIAS -> runCancellableCommand(() -> this.createAccount(line));
                case TRANSFER_TO, TRANSFER_TO_ALIAS -> runCancellableCommand(() -> this.transferTo(line));
                case BALANCE, BALANCE_ALIAS -> runCancellableCommand(() -> this.balance(line));
                case HELP, HELP_ALIAS -> this.printUsage();
                case EXIT, EXIT_ALIAS -> exit = true;
                default -> {
                    if (!line.isBlank()) {
                        log.error("Command '%s' does not exist%n%n", cmd);
                        this.printUsage();
                    } else {
                        System.out.print("> ");
                    }

                }
            }

            if (!exit && running.get()) {
                try {
                    // Wait for a bit before telling the user their action can be cancelled.
                    // If the action finished before the sleep, this thread will be interrupted and the message
                    // below will not be sent.
                    Thread.sleep(1000);
                    log.info("Command is taking longer than expected... Press ENTER to cancel.");
                } catch (InterruptedException ignore) {
                }
            }
        }
    }


    private void runCancellableCommand(Callable<Void> handler) {
        running.set(true);
        this.runningTask = new Thread(() -> {
            try {
                handler.call();
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.CANCELLED) {
                    log.info("Cancelled gRPC request");
                    return;
                }
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
            } catch (Throwable e) {
                log.error(e.getMessage());
                e.printStackTrace();
            } finally {
                running.set(false);
                System.out.print("> ");
                // Avoid sending the "this is taking too long" message
                this.mainThread.interrupt();
            }
        });
        this.runningTask.start();
    }

    private Void createAccount(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return null;
        }

        String server = split[1];
        String username = split[2];

        userService.createAccount(server, username);
        log.debug("Account '%s' has been created%n", username);
        log.info("OK%n");

        return null;
    }

    private Void balance(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 3) {
            this.printUsage();
            return null;
        }
        final String server = split[1];
        final String username = split[2];

        final int balance = userService.balance(server, username);

        log.debug("Balance of user '%s' is %d%n", username, balance);
        log.info("OK");
        if (balance > 0) {
            log.info("%d", balance);
        }
        log.info("");

        return null;
    }

    private Void transferTo(String line) throws ServerUnresolvableException {
        String[] split = line.split(SPACE);

        if (split.length != 5) {
            this.printUsage();
            return null;
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

        return null;
    }

    private void printUsage() {
        log.info(
                """
                        Usage:
                        - createAccount <server> <username>
                        - balance <server> <username>
                        - transferTo <server> <username_from> <username_to> <amount>
                        - exit
                        """
        );
        System.out.print("> ");
    }
}
