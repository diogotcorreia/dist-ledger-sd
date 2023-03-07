package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String ACTIVATE_ALIAS = "a";
    private static final String DEACTIVATE = "deactivate";
    private static final String DEACTIVATE_ALIAS = "d";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GET_LEDGER_STATE_ALIAS = "ls";
    private static final String GOSSIP = "gossip";
    private static final String GOSSIP_ALIAS = "g";
    private static final String HELP = "help";
    private static final String HELP_ALIAS = "h";
    private static final String EXIT = "exit";
    private static final String EXIT_ALIAS = "e";

    private final AdminService adminService;

    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }

    void parseInput() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            switch (cmd) {
                case ACTIVATE, ACTIVATE_ALIAS -> this.activate(line);
                case DEACTIVATE, DEACTIVATE_ALIAS -> this.deactivate(line);
                case GET_LEDGER_STATE, GET_LEDGER_STATE_ALIAS -> this.dump(line);
                case GOSSIP, GOSSIP_ALIAS -> this.gossip(line);
                case HELP, HELP_ALIAS -> this.printUsage();
                case EXIT, EXIT_ALIAS -> exit = true;
                default -> {
                    System.err.printf("Command '%s' does not exist%n%n", cmd);
                    this.printUsage();
                }
            }

        }
    }

    private void activate(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.activate(server);
        System.out.printf("Server '%s' has been activated%n", server);
    }

    private void deactivate(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.deactivate(server);
        System.out.printf("Server '%s' has been deactivated%n", server);
    }

    private void dump(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String server = split[1];

        final LedgerState ledgerState = adminService.getLedgerState(server);
        System.out.println(ledgerState);
    }

    @SuppressWarnings("unused")
    private void gossip(String line) {
        /* TODO Phase-3 */
        adminService.gossip();
    }

    private void printUsage() {
        System.out.println(
                """
                        Usage:
                        - activate <server>
                        - deactivate <server>
                        - getLedgerState <server>
                        - gossip <server>
                        - exit
                        """
        );
    }

}
