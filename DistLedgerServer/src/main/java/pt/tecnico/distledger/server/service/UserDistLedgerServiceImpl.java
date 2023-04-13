package pt.tecnico.distledger.server.service;

import io.grpc.stub.StreamObserver;
import lombok.CustomLog;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.ServerCoordinator;
import pt.tecnico.distledger.server.domain.OperationResult;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.AccountNotEmptyException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.AccountProtectedException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.InvalidAmountException;
import pt.tecnico.distledger.server.exceptions.PropagationException;
import pt.tecnico.distledger.server.exceptions.ReadOnlyException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.exceptions.TransferBetweenSameAccountException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

@CustomLog(topic = "User Service")
public class UserDistLedgerServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState serverState;

    public UserDistLedgerServiceImpl(ServerCoordinator serverCoordinator) {
        this.serverState = serverCoordinator.getServerState();
    }

    @Override
    public void balance(
            BalanceRequest request,
            StreamObserver<BalanceResponse> responseObserver
    ) {
        log.debug("Balance for account '%s' has been requested", request.getUserId());
        try {
            final OperationResult<Integer> result = serverState.getBalance(
                    request.getUserId(),
                    new VectorClock(request.getPrevTimestampMap())
            );
            final int balance = result.value();
            final VectorClock newTimestamp = result.vectorClock();

            log.debug("Account '%s' has a balance of %d coin(s)", request.getUserId(), balance);
            final BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance)
                    .putAllNewTimestamp(newTimestamp.getTimestamps())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (AccountNotFoundException | ServerUnavailableException e) {
            log.debug("Error getting balance: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void createAccount(
            CreateAccountRequest request,
            StreamObserver<CreateAccountResponse> responseObserver
    ) {
        log.debug("Creation of account '%s' has been requested", request.getUserId());
        try {
            final OperationResult<Void> result =
                    serverState.createAccount(request.getUserId(), new VectorClock(request.getPrevTimestampMap()));
            log.debug("Account '%s' has been created", request.getUserId());
            responseObserver.onNext(
                    CreateAccountResponse.newBuilder()
                            .putAllNewTimestamp(result.vectorClock().getTimestamps())
                            .build()
            );
            responseObserver.onCompleted();
        } catch (AccountAlreadyExistsException | ServerUnavailableException | PropagationException |
                 ReadOnlyException e) {
            log.debug("Error creating account: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void deleteAccount(
            DeleteAccountRequest request,
            StreamObserver<DeleteAccountResponse> responseObserver
    ) {
        log.debug("Deletion of account '%s' has been requested", request.getUserId());
        try {
            serverState.deleteAccount(request.getUserId());
            log.debug("Account '%s' has been deleted", request.getUserId());
            responseObserver.onNext(DeleteAccountResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (AccountNotEmptyException | AccountNotFoundException | AccountProtectedException |
                 ServerUnavailableException | PropagationException | ReadOnlyException e) {
            log.debug("Error deleting account: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void transferTo(
            TransferToRequest request,
            StreamObserver<TransferToResponse> responseObserver
    ) {
        log.debug(
                "Transfer of %d coin(s) from account '%s' to account '%s' has been requested",
                request.getAmount(),
                request.getAccountFrom(),
                request.getAccountTo()
        );
        try {
            OperationResult<Void> result = serverState.transferTo(
                    request.getAccountFrom(),
                    request.getAccountTo(),
                    request.getAmount(),
                    new VectorClock(request.getPrevTimestampMap())
            );
            log.debug(
                    "Created transfer of %d coin(s) from account '%s' to account '%s'",
                    request.getAmount(),
                    request.getAccountFrom(),
                    request.getAccountTo()
            );
            responseObserver.onNext(
                    TransferToResponse.newBuilder()
                            .putAllNewTimestamp(result.vectorClock().getTimestamps())
                            .build()
            );
            responseObserver.onCompleted();
        } catch (AccountNotFoundException | InsufficientFundsException | InvalidAmountException |
                 ServerUnavailableException | TransferBetweenSameAccountException | PropagationException |
                 ReadOnlyException e) {
            log.debug("Error creating transfer: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

}
