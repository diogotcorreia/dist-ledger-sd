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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        CreateAccountResponse response = serverResolver.resolveStub(qualifier)
                .createAccount(
                        CreateAccountRequest.newBuilder()
                                .setUserId(username)
                                .putAllPrevTimestamp(vectorClock.getTimestamps())
                                .build()
                );
        log.debug("[Server '%s'] Received response to create account for '%s'", qualifier, username);
        vectorClock.updateVectorClock(new VectorClock(response.getNewTimestampMap()));
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

        // since this operation may take a while, if the gossip process is slow, we allow the user to cancel it (as it's blocking)
        log.info("This operation is cancellable. Press ENTER to cancel this request.");

        AtomicReference<BalanceResponse> response = new AtomicReference<>();
        List<Thread> threads = List.of(new Thread(() -> {
            try {
                System.in.read();
            } catch (Exception e) {
                log.error("Error while waiting for user input", e);
            }
        }),
                new Thread(() -> {
                    try {
                        response.set(serverResolver.resolveStub(qualifier)
                                .balance(
                                        BalanceRequest.newBuilder()
                                                .setUserId(username)
                                                .putAllPrevTimestamp(vectorClock.getTimestamps())
                                                .build()
                                ));
                        log.debug(
                                "[Server '%s'] Received response to get balance for '%s' (value: %d)",
                                qualifier,
                                username,
                                response.get().getValue()
                        );
                        vectorClock.updateVectorClock(new VectorClock(response.get().getNewTimestampMap()));
                    } catch (Exception e) {
                        log.error("Error while performing request", e);
                    }
                })
        );
        threads.forEach(Thread::start);

        // we wait for either the user to press ENTER or for the request to finish
        Thread thread1 = threads.get(0);
        Thread thread2 = threads.get(1);
        try {
            thread1.join();
        } catch (InterruptedException e) {
            log.error("Error while waiting for user input", e);
        }

        // if the user pressed ENTER, we cancel the request
        if (thread1.isInterrupted()) {
            thread2.interrupt();
            log.info("Request cancelled");
            return 0;
        }
        return response.get().getValue();
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
    }

    @Override
    public void close() {
        serverResolver.close();
    }
}
