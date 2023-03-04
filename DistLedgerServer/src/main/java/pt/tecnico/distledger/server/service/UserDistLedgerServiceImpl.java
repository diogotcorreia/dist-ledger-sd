package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.DistLedgerExceptions;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import static io.grpc.Status.INVALID_ARGUMENT;


public class UserDistLedgerServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState serverState;

    public UserDistLedgerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void balance(
            UserDistLedger.BalanceRequest request,
            StreamObserver<UserDistLedger.BalanceResponse> responseObserver
    ) {
        try {
            UserDistLedger.BalanceResponse response = UserDistLedger.BalanceResponse.newBuilder()
                    .setValue(serverState.getBalance(request.getUserId()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }


    @Override
    public void createAccount(
            UserDistLedger.CreateAccountRequest request,
            StreamObserver<UserDistLedger.CreateAccountResponse> responseObserver
    ) {
        try {
            serverState.createAccount(request.getUserId());
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }

    }

    @Override
    public void deleteAccount(
            UserDistLedger.DeleteAccountRequest request,
            StreamObserver<UserDistLedger.DeleteAccountResponse> responseObserver
    ) {
        try {
            serverState.deleteAccount(request.getUserId());
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void transferTo(
            UserDistLedger.TransferToRequest request,
            StreamObserver<UserDistLedger.TransferToResponse> responseObserver
    ) {
        try {
            serverState.transferTo(request.getAccountFrom(), request.getAccountTo(), request.getAmount());
            responseObserver.onCompleted();
        } catch (DistLedgerExceptions e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
