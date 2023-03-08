package pt.tecnico.distledger.adminclient;

import org.grpcmock.GrpcMock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.adminclient.CommandParser;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.grpcmock.GrpcMock.response;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
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
        System.setErr(new PrintStream(outputStream));
    }

    void parseInput(final @NotNull String command) {
        inputStream = new ByteArrayInputStream(command.getBytes());
        System.setIn(inputStream);

        client.parseInput();
    }

    @Test
    void help() {
        parseInput("help\nexit\n");

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
        stubFor(
                unaryMethod(AdminServiceGrpc.getActivateMethod())
                        .withRequest(AdminDistLedger.ActivateRequest.getDefaultInstance())
                        .willReturn(response(AdminDistLedger.ActivateResponse.getDefaultInstance()))
        );

        parseInput("activate " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                > OK

                >\s""");
    }

    @Test
    void deactivate() {
        stubFor(
                unaryMethod(AdminServiceGrpc.getDeactivateMethod())
                        .withRequest(AdminDistLedger.DeactivateRequest.getDefaultInstance())
                        .willReturn(response(AdminDistLedger.DeactivateResponse.getDefaultInstance()))
        );
        parseInput("deactivate " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                > OK

                >\s""");
    }

    @Test
    void getLedgerState() {
        final List<Operation> operations = List.of(
                Operation.newBuilder()
                        .setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("diogo-gaspar")
                        .build(),
                Operation.newBuilder()
                        .setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("diogo-correia")
                        .build(),
                Operation.newBuilder()
                        .setType(OperationType.OP_CREATE_ACCOUNT)
                        .setUserId("tomas-esteves")
                        .build(),
                Operation.newBuilder()
                        .setType(OperationType.OP_DELETE_ACCOUNT)
                        .setUserId("tomas-esteves")
                        .build(),
                Operation.newBuilder()
                        .setType(OperationType.OP_TRANSFER_TO)
                        .setUserId("broker")
                        .setDestUserId("diogo-gaspar")
                        .setAmount(100)
                        .build()
        );

        stubFor(
                unaryMethod(AdminServiceGrpc.getGetLedgerStateMethod())
                        .withRequest(GetLedgerStateRequest.getDefaultInstance())
                        .willReturn(
                                response(
                                        GetLedgerStateResponse.newBuilder()
                                                .setLedgerState(
                                                        LedgerState.newBuilder()
                                                                .addAllLedger(operations)
                                                                .build()
                                                )
                                                .build()
                                )
                        )
        );

        parseInput("getLedgerState " + MAIN_SERVER + "\nexit\n");

        assertEquals(outputStream.toString(), """
                > OK
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
                  amount: 100
                }


                >\s""");
    }

    // TODO: add gossip tests whenever method is implemented

}
