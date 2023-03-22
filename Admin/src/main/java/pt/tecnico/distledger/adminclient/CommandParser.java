package pt.tecnico.distledger.adminclient;

import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

import java.util.Scanner;

@CustomLog
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
            log.info("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try {
                switch (cmd) {
                    case ACTIVATE, ACTIVATE_ALIAS -> this.activate(line);
                    case DEACTIVATE, DEACTIVATE_ALIAS -> this.deactivate(line);
                    case GET_LEDGER_STATE, GET_LEDGER_STATE_ALIAS -> this.dump(line);
                    case GOSSIP, GOSSIP_ALIAS -> this.gossip(line);
                    case HELP, HELP_ALIAS -> this.printUsage();
                    case EXIT, EXIT_ALIAS -> exit = true;
                    default -> {
                        log.error("Command '%s' does not exist%n", cmd);
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
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
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
        log.debug("Server '%s' has been activated%n", server);
        log.info("OK%n");
    }

    private void deactivate(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String server = split[1];

        adminService.deactivate(server);
        log.debug("Server '%s' has been deactivated%n", server);
        log.info("OK%n");
    }

    private void dump(String line) {
        String[] split = line.split(SPACE);

        if (split.length != 2) {
            this.printUsage();
            return;
        }
        String server = split[1];

        final GetLedgerStateResponse response = adminService.getLedgerState(server);
        log.info("OK%n%s", response);
    }

    @SuppressWarnings("unused")
    private void gossip(String line) {
        /* TODO Phase-3 */
        adminService.gossip();
    }

    private void printUsage() {
        log.info(
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
