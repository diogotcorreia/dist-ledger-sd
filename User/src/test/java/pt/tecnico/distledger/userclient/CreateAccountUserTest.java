package pt.tecnico.distledger.userclient;

import io.grpc.Status;
import lombok.SneakyThrows;
import lombok.val;
import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.connection.SingleServerResolver;
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
    private static final String createAccountCommand = "createAccount A user1\nexit\n";

    @BeforeEach
    public void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        val resolver =
                new SingleServerResolver<>(LOCALHOST, GrpcMock.getGlobalPort(), UserServiceGrpc::newBlockingStub);
        service = new UserService(resolver);
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }

    @AfterEach
    public void destroy() {
        service.close();
    }

    @Test
    public void createAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .withRequest(UserDistLedger.CreateAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(response(UserDistLedger.CreateAccountResponse.newBuilder().build()))
        );

        inputStream = new ByteArrayInputStream(createAccountCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > OK

                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void createDuplicatedAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getCreateAccountMethod())
                        .withRequest(UserDistLedger.CreateAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.FAILED_PRECONDITION.toStatus()
                                                .withDescription("Account 'user1' already exists")
                                )
                        )
        );

        inputStream = new ByteArrayInputStream(createAccountCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account 'user1' already exists
                >\s""", outputStream.toString());
    }
}
