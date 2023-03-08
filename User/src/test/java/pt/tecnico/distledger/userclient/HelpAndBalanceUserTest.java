package pt.tecnico.distledger.userclient;

import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;


class HelpAndBalanceUserTest {

    private static UserService service;
    private static CommandParser client;

    private static ByteArrayInputStream inputStream;
    private static ByteArrayOutputStream outputStream;

    private static final String LOCALHOST = "localhost";
    private static final String balanceCommand = "balance A user1\nexit\n";

    @BeforeEach
    void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        service = new UserService(LOCALHOST, GrpcMock.getGlobalPort());
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void help() {
        final String helpCommand = """
                help
                exit
                """;
        inputStream = new ByteArrayInputStream(helpCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals(outputStream.toString(), """
                > Usage:
                - createAccount <server> <username>
                - deleteAccount <server> <username>
                - balance <server> <username>
                - transferTo <server> <username_from> <username_to> <amount>
                - exit

                >\s""");
    }

    @Test
    void balance() {
        stubFor(
                unaryMethod(UserServiceGrpc.getBalanceMethod())
                        .willReturn(response(UserDistLedger.BalanceResponse.newBuilder().setValue(20).build()))
        );

        inputStream = new ByteArrayInputStream(balanceCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals(outputStream.toString(), """
                > OK
                20

                >\s""");
    }

    @Test
    void emptyBalance() {
        stubFor(
                unaryMethod(UserServiceGrpc.getBalanceMethod())
                        .willReturn(response(UserDistLedger.BalanceResponse.newBuilder().setValue(0).build()))
        );

        inputStream = new ByteArrayInputStream(balanceCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals(outputStream.toString(), """
                > OK
                0

                >\s""");
    }


}
