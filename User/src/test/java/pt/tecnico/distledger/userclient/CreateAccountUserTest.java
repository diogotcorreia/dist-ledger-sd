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


class CreateAccountUserTest {

    private static UserService service;
    private static CommandParser client;

    private static ByteArrayInputStream inputStream;
    private static ByteArrayOutputStream outputStream;

    private static final String LOCALHOST = "localhost";

    private static final String createAccountCommand = "createAccount A user1\n";

    private static final String exitCommand = "exit\n";

    @BeforeEach
    void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        service = new UserService(LOCALHOST, GrpcMock.getGlobalPort());
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }


    @Test
    void createAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .withRequest(UserDistLedger.CreateAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(response(UserDistLedger.CreateAccountResponse.newBuilder().build()))
        );

        final String command = createAccountCommand + exitCommand;
        inputStream = new ByteArrayInputStream(command.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals(outputStream.toString(), """
                > Account 'user1' has been created
                >\s""");
    }
}
