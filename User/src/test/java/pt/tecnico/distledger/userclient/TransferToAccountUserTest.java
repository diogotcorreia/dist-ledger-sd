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


class TransferToAccountUserTest {

    private static UserService service;
    private static CommandParser client;

    private static ByteArrayInputStream inputStream;
    private static ByteArrayOutputStream outputStream;

    private static final String LOCALHOST = "localhost";

    // Assuming user1 has 1000 and user2 has 500
    private static final String transferToCommand = "transferTo A user1 user2 125\nexit\n";

    @BeforeEach
    void setup() {
        GrpcMock.configureFor(GrpcMock.grpcMock(0).build().start());

        service = new UserService(LOCALHOST, GrpcMock.getGlobalPort());
        client = new CommandParser(service);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }

    @Test
    void transferTo() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user1")
                                        .setAccountTo("user2")
                                        .setAmount(125)
                                        .build()
                        )
                        .willReturn(response(UserDistLedger.TransferToResponse.newBuilder().build()))
        );

        inputStream = new ByteArrayInputStream(transferToCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > OK

                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void fromAccountDoesNotExist() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user34")
                                        .setAccountTo("user2")
                                        .setAmount(125)
                                        .build()
                        )
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.INVALID_ARGUMENT.toStatus()
                                                .withDescription("Account 'user34' not found")
                                )
                        )
        );

        final String noFromUserCommand = "transferTo A user34 user2 125\nexit\n";

        inputStream = new ByteArrayInputStream(noFromUserCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account 'user34' not found
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void ToAccountDoesNotExist() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user1")
                                        .setAccountTo("user34")
                                        .setAmount(125)
                                        .build()
                        )
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.INVALID_ARGUMENT.toStatus()
                                                .withDescription("Account 'user34' not found")
                                )
                        )
        );

        final String noFromUserCommand = "transferTo A user1 user34 125\nexit\n";

        inputStream = new ByteArrayInputStream(noFromUserCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account 'user34' not found
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void sameAccountTransfer() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user1")
                                        .setAccountTo("user1")
                                        .setAmount(125)
                                        .build()
                        )
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.FAILED_PRECONDITION.toStatus()
                                                .withDescription(
                                                        "It is not possible to create a transfer between the same account (from 'user1' to 'user1')."
                                                )
                                )
                        )
        );

        final String noToUserCommand = "transferTo A user1 user1 125\nexit\n";

        inputStream = new ByteArrayInputStream(noToUserCommand.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] It is not possible to create a transfer between the same account (from 'user1' to 'user1').
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void amountBelowZero() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user1")
                                        .setAccountTo("user2")
                                        .setAmount(-1)
                                        .build()
                        )
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.INVALID_ARGUMENT.toStatus()
                                                .withDescription(
                                                        "The given amount (-1) is a non-positive number"
                                                )
                                )
                        )
        );

        final String belowZeroAmount = "transferTo A user1 user2 -1\nexit\n";

        inputStream = new ByteArrayInputStream(belowZeroAmount.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] The given amount (-1) is a non-positive number
                >\s""", outputStream.toString());
    }

    @Test
    @SneakyThrows
    void insufficientAmount() {
        stubFor(
                unaryMethod(UserServiceGrpc.getTransferToMethod())
                        .withRequest(
                                UserDistLedger.TransferToRequest.newBuilder()
                                        .setAccountFrom("user1")
                                        .setAccountTo("user2")
                                        .setAmount(1001)
                                        .build()
                        )
                        .willReturn(
                                GrpcMock.statusException(
                                        Status.Code.FAILED_PRECONDITION.toStatus()
                                                .withDescription(
                                                        "Account 'user1' does not have enough funds (expected 1001 but only 1000 are available)"
                                                )
                                )
                        )
        );

        final String insufficientAmount = "transferTo A user1 user2 1001\nexit\n";

        inputStream = new ByteArrayInputStream(insufficientAmount.getBytes());
        System.setIn(inputStream);

        client.parseInput();

        assertEquals("""
                > [ERROR] Account 'user1' does not have enough funds (expected 1001 but only 1000 are available)
                >\s""", outputStream.toString());
    }
}
