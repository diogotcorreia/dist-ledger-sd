package pt.tecnico.distledger.userclient;

import io.grpc.Status;
import lombok.SneakyThrows;
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


class DeleteAccountUserTest {

    private static UserService service;
    private static CommandParser client;

    private static ByteArrayInputStream inputStream;
    private static ByteArrayOutputStream outputStream;

    private static final String deleteAccountCommand = "deleteAccount A user1\nexit\n";

    @BeforeEach
    void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        service = new UserService();
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }

    @Test
    void deleteAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(UserDistLedger.DeleteAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(response(UserDistLedger.DeleteAccountResponse.newBuilder().build()))
        );

        inputStream = new ByteArrayInputStream(deleteAccountCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > OK

                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void deleteDuplicatedAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(UserDistLedger.DeleteAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.INVALID_ARGUMENT.toStatus()
                                                .withDescription("Account 'user1' not found")
                                )
                        )
        );

        inputStream = new ByteArrayInputStream(deleteAccountCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account 'user1' not found
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void deleteBrokerAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(UserDistLedger.DeleteAccountRequest.newBuilder().setUserId("broker").build())
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.PERMISSION_DENIED.toStatus()
                                                .withDescription("Account broker is protected. It cannot be removed")
                                )
                        )
        );

        final String deleteBrokerCommand = "deleteAccount A broker\nexit\n";
        inputStream = new ByteArrayInputStream(deleteBrokerCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account broker is protected. It cannot be removed
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void deleteNonEmptyAccount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getDeleteAccountMethod())
                        .withRequest(UserDistLedger.DeleteAccountRequest.newBuilder().setUserId("user1").build())
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.FAILED_PRECONDITION.toStatus()
                                                .withDescription(
                                                        "Account user1 cannot be removed, as its balance 10 is not zero."
                                                )
                                )
                        )
        );

        inputStream = new ByteArrayInputStream(deleteAccountCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account user1 cannot be removed, as its balance 10 is not zero.
                >\s""", outputStream.toString());
    }
}
