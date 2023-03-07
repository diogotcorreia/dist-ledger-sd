import org.grpcmock.GrpcMock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.adminclient.CommandParser;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.tecnico.distledger.userclient.grpc.UserService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminTest {

    private static AdminService service;
    private static CommandParser client;

    private static ByteArrayInputStream inputStream;
    private static ByteArrayOutputStream outputStream;

    private static final String LOCALHOST = "localhost";
    private static final String MAIN_SERVER = "A";

    @BeforeEach
    void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        service = new AdminService(LOCALHOST, GrpcMock.getGlobalPort());
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        try (UserService userService = new UserService(LOCALHOST, GrpcMock.getGlobalPort())) {
            // initial operations in the server
            userService.createAccount("diogo-gaspar");
            userService.createAccount("diogo-correia");
            userService.createAccount("tomas-esteves");
            userService.deleteAccount("tomas-esteves");
            userService.transferTo("broker", "diogo-gaspar", 500);
            userService.transferTo("diogo-gaspar", "diogo-correia", 100);
        }
    }

    // FIXME: couldn't think of a better name
    void perform(final @NotNull String command) {
        inputStream = new ByteArrayInputStream(command.getBytes());
        System.setIn(inputStream);

        client.parseInput();
    }

    @Test
    void help() {
        perform("help\nexit\n");

        assertEquals(outputStream.toString(), """
                > Usage:
                - activate <server>
                - deactivate <server>
                - getLedgerState <server>
                - gossip <server>
                - exit

                >\s""");
    }

    @Test
    void activate() {
        perform("activate " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                > OK
                """);
    }

    @Test
    void deactivate() {
        perform("deactivate " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                > OK
                """);
    }

    @Test
    void getLedgerState() {
        perform("getLedgerState " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                OK
                ledger {
                  type: OP_CREATE_ACCOUNT
                  userId: "diogo-gaspar"
                }
                ledger {
                  type: OP_CREATE_ACCOUNT
                  userId: "diogo-correia"
                }
                ledger {
                  type: OP_CREATE_ACCOUNT
                  userId: "tomas-esteves"
                }
                ledger {
                  type: OP_DELETE_ACCOUNT
                  userId: "tomas-esteves"
                }
                ledger {
                  type: OP_TRANSFER_TO
                  userId: "broker"
                  destUserId: "diogo-gaspar"
                  amount: 500
                }
                ledger {
                  type: OP_TRANSFER_TO
                  userId: "diogo-gaspar"
                  destUserId: "diogo-correia"
                  amount: 100
                }


                >\s
                """);
    }

    // TODO: add gossip tests whenever method is implemented

}
