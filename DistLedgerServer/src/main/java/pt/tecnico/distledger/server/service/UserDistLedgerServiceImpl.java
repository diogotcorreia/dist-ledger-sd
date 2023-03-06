package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.CannotRemoveProtectedAccountException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;


public class UserDistLedgerServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState serverState;

    public UserDistLedgerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }

    @Override
    public void balance(
            BalanceRequest request,
            StreamObserver<BalanceResponse> responseObserver
    ) {
        try {
            final int balance = serverState.getBalance(request.getUserId());
            final BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (AccountNotFoundException | ServerUnavailableException e) {
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void createAccount(
            CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver
    ) {
        try {
            serverState.createAccount(request.getUserId());
            responseObserver.onNext(CreateAccountResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (AccountAlreadyExistsException | ServerUnavailableException e) {
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void deleteAccount(
            DeleteAccountRequest request,
            StreamObserver<DeleteAccountResponse> responseObserver
    ) {
        try {
            serverState.deleteAccount(request.getUserId());
            responseObserver.onNext(DeleteAccountResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (AccountNotFoundException | CannotRemoveProtectedAccountException | ServerUnavailableException e) {
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void transferTo(
            TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver
    ) {
        try {
            serverState.transferTo(request.getAccountFrom(), request.getAccountTo(), request.getAmount());
            responseObserver.onNext(TransferToResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (AccountNotFoundException | InsufficientFundsException | ServerUnavailableException e) {
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

}
