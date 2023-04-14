package pt.tecnico.distledger.userclient.grpc;

import io.grpc.Status;
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
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceBlockingStub;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;

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

    public int balance(String qualifier, String username) throws StatusRuntimeException, ServerUnresolvableException {
        log.debug("Sending request to get balance for '%s'", username);
        logTimestamps();
        AtomicReference<BalanceResponse> response = new AtomicReference<>();
        List<Callable<Void>> tasks = List.of(
                () -> {
                    Thread.sleep(1000);

                    log.info("Query is taking too long... Press ENTER to cancel.");

                    // noinspection ResultOfMethodCallIgnored
                    System.in.read();
                    return null;
                },
                () -> {
                    try {
                        response.set(
                                serverResolver.resolveStub(qualifier)
                                        .balance(
                                                BalanceRequest.newBuilder()
                                                        .setUserId(username)
                                                        .putAllPrevTimestamp(vectorClock.getTimestamps())
                                                        .build()
                                        )
                        );
                        log.debug(
                                "[Server '%s'] Received response to get balance for '%s' (value: %d)",
                                qualifier,
                                username,
                                response.get().getValue()
                        );
                        vectorClock.updateVectorClock(new VectorClock(response.get().getNewTimestampMap()));
                    } catch (StatusRuntimeException e) {
                        if (e.getStatus().getCode() != Status.Code.CANCELLED) {
                            log.error("Error while performing request: %s", e);
                        }
                    } catch (Exception e) {
                        log.error("Error while performing request: %s", e);
                    }
                    return null;
                }
        );

        ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        tasks.forEach(completionService::submit);

        try {
            // Wait for the threads to finish, returning the first one that finishes
            completionService.take().get();
        } catch (Exception e) {
            log.error("Error while waiting for threads to finish", e);
        } finally {
            executorService.shutdownNow();
        }

        if (response.get() == null) {
            log.debug("Request successfully cancelled");
            return -1;
        }

        logTimestamps();
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
