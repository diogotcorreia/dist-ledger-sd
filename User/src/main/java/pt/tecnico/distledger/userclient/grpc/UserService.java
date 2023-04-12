package pt.tecnico.distledger.userclient.grpc;

import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.common.connection.ServerResolver;
import pt.tecnico.distledger.common.exceptions.ServerUnresolvableException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceBlockingStub;

import static pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

@CustomLog(topic = "Service")
@RequiredArgsConstructor
public class UserService implements AutoCloseable {

    private final ServerResolver<UserServiceBlockingStub> serverResolver;

    private final VectorClock vectorClock = new VectorClock();

    public void createAccount(
            String qualifier,
            String username
    ) throws StatusRuntimeException, ServerUnresolvableException {
        log.debug("[Server '%s'] Sending request to create account for '%s'", qualifier, username);
        logTimestamps();
        CreateAccountResponse response = serverResolver.resolveStub(qualifier)
                .createAccount(
                        CreateAccountRequest.newBuilder()
                                .setUserId(username)
                                .putAllPrevTimestamp(vectorClock.getTimestamps())
                                .build()
                );
        log.debug("[Server '%s'] Received response to create account for '%s'", qualifier, username);
        vectorClock.updateVectorClock(new VectorClock(response.getNewTimestampMap()));
        logTimestamps();
    }

    public void deleteAccount(
            String qualifier,
            String username
    ) throws StatusRuntimeException, ServerUnresolvableException {
        log.debug("[Server '%s'] Sending request to delete account for '%s'", qualifier, username);
        // noinspection ResultOfMethodCallIgnored
        serverResolver.resolveStub(qualifier)
                .deleteAccount(
                        DeleteAccountRequest.newBuilder()
                                .setUserId(username)
                                .build()
                );
        log.debug("[Server '%s'] Received response to delete account for '%s'", qualifier, username);
    }

    public int balance(String qualifier, String username) throws StatusRuntimeException, ServerUnresolvableException {
        log.debug("Sending request to get balance for '%s'", username);
        logTimestamps();
        final BalanceResponse response = serverResolver.resolveStub(qualifier)
                .balance(
                        BalanceRequest.newBuilder()
                                .setUserId(username)
                                .putAllPrevTimestamp(vectorClock.getTimestamps())
                                .build()
                );
        log.debug(
                "[Server '%s'] Received response to get balance for '%s' (value: %d)",
                qualifier,
                username,
                response.getValue()
        );
        vectorClock.updateVectorClock(new VectorClock(response.getNewTimestampMap()));
        logTimestamps();
        return response.getValue();
    }


    public void transferTo(
            String qualifier,
            String from,
            String to,
            Integer amount
    ) throws StatusRuntimeException, ServerUnresolvableException {
        log.debug(
                "[Server '%s'] Sending request to create transfer of %d coin(s) from '%s' to '%s'",
                qualifier,
                amount,
                from,
                to
        );
        logTimestamps();
        TransferToResponse response = serverResolver.resolveStub(qualifier)
                .transferTo(
                        TransferToRequest.newBuilder()
                                .setAccountFrom(from)
                                .setAccountTo(to)
                                .setAmount(amount)
                                .putAllPrevTimestamp(vectorClock.getTimestamps())
                                .build()
                );
        log.debug(
                "[Server '%s'] Received response to create transfer of %d coin(s) from '%s' to '%s'",
                qualifier,
                amount,
                from,
                to
        );
        vectorClock.updateVectorClock(new VectorClock(response.getNewTimestampMap()));
        logTimestamps();
    }

    @Override
    public void close() {
        serverResolver.close();
    }

    public void logTimestamps() {
        log.debug("Vector clock is currently: %s", vectorClock.getTimestamps());
    }
}
