package pt.tecnico.distledger.server.domain;

import lombok.CustomLog;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.InvalidAmountException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;
import pt.tecnico.distledger.server.exceptions.TransferBetweenSameAccountException;
import pt.tecnico.distledger.server.visitor.ExecuteOperationVisitor;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@CustomLog(topic = "Server State")
public class ServerState {

    public static final String BROKER_ID = "broker";
    public static final int BROKER_INITIAL_AMOUNT = 1000;
    private final Ledger ledger;
    private final Map<String, Account> accounts;
    private final AtomicBoolean active;
    private final String qualifier;

    private final VectorClock replicaTimestamp = new VectorClock();
    private final VectorClock valueTimestamp = new VectorClock();
    private final Map<String, VectorClock> gossipTimestampMap = new HashMap<>();

    @VisibleForTesting
    public ServerState() {
        this("TEST");
    }

    public ServerState(String qualifier) {
        this.accounts = new ConcurrentHashMap<>();
        this.ledger = new Ledger(this::canOperationBeStable, this::executeOperation);
        this.active = new AtomicBoolean(true);
        createBroker();
        this.qualifier = qualifier;
    }

    public OperationResult<Integer> getBalance(
            String userId,
            VectorClock prevTimestamp
    ) throws AccountNotFoundException, ServerUnavailableException {
        // We check if the client's previous timestamp is consistent with the server's -- that is, if this replica still
        // needs to wait for operations to be propagated in order to return a consistent result.
        // The condition verified is: !valueTimestamp.isNewerThanOrEqualTo(prevTimestamp) -- if this is true, we need to keep waiting
        // For this, we use the wait/notify mechanism
        ensureServerIsActive();
        synchronized (this.valueTimestamp) {
            while (!this.valueTimestamp.isNewerThanOrEqualTo(prevTimestamp)) {
                try {
                    this.valueTimestamp.wait();
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for value timestamp to be updated", e);
                }
            }

            ensureServerIsActive();
            return new OperationResult<>(
                    getAccount(userId)
                            .orElseThrow(() -> new AccountNotFoundException(userId))
                            .getBalance(),
                    this.valueTimestamp
            );
        }
    }

    public OperationResult<Void> createAccount(
            @NotNull String userId,
            VectorClock prevTimestamp
    ) throws ServerUnavailableException {
        ensureServerIsActive();

        int currentReplicaClockValue;
        synchronized (this.replicaTimestamp) {
            this.replicaTimestamp.incrementClock(qualifier);
            currentReplicaClockValue = this.replicaTimestamp.getValue(this.qualifier);
        }
        VectorClock uniqueTimestamp = prevTimestamp.clone();
        uniqueTimestamp.setValue(qualifier, currentReplicaClockValue);

        CreateOp pendingOperation = new CreateOp(userId, prevTimestamp, uniqueTimestamp, false);
        ledger.addUnstable(pendingOperation);

        log.debug("Replica's current timestamp: %s", replicaTimestamp);
        return new OperationResult<>(null, uniqueTimestamp);
    }

    public OperationResult<Void> transferTo(
            @NotNull String fromUserId,
            @NotNull String toUserId,
            int amount,
            VectorClock prevTimestamp
    ) throws ServerUnavailableException, InvalidAmountException, TransferBetweenSameAccountException {
        ensureServerIsActive();

        if (fromUserId.equals(toUserId)) {
            throw new TransferBetweenSameAccountException(fromUserId, toUserId);
        }

        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }

        int currentReplicaClockValue;
        synchronized (this.replicaTimestamp) {
            this.replicaTimestamp.incrementClock(qualifier);
            currentReplicaClockValue = this.replicaTimestamp.getValue(this.qualifier);
        }
        VectorClock uniqueTimestamp = prevTimestamp.clone();
        uniqueTimestamp.setValue(qualifier, currentReplicaClockValue);

        TransferOp pendingOperation =
                new TransferOp(fromUserId, toUserId, amount, prevTimestamp, uniqueTimestamp, false);
        ledger.addUnstable(pendingOperation);
        log.debug("Replica's current timestamp: %s", this.replicaTimestamp);
        return new OperationResult<>(null, uniqueTimestamp);
    }

    public void activate() {
        this.active.set(true);
    }

    public void deactivate() {
        this.active.set(false);
    }

    /**
     * Get operations to be sent to another replica with the given qualifier.
     *
     * @param visitor   The visitor to be called with every operation to be sent to the replica.
     * @param qualifier The qualifier of the replica to send operations to.
     */
    public void operateOverLedgerToPropagateToReplica(OperationVisitor visitor, String qualifier) {
        final VectorClock lastTimestamp = gossipTimestampMap.getOrDefault(qualifier, new VectorClock());

        ledger.operateOverLedger(
                visitor,
                operation -> !lastTimestamp.isNewerThanOrEqualTo(operation.getUniqueTimestamp())
        );
    }

    /**
     * Save the timestamp of the last operation propagated to this replica.
     *
     * @param qualifier The qualifier of the replica to save this timestamp of.
     * @param timestamp The timestamp to save.
     */
    public void updateGossipTimestamp(String qualifier, VectorClock timestamp) {
        gossipTimestampMap.put(qualifier, timestamp);
    }

    public void operateOverLedger(OperationVisitor visitor) {
        ledger.operateOverLedger(visitor);
    }

    public synchronized void addToLedger(List<Operation> newOperations) throws ServerUnavailableException {
        ensureServerIsActive();
        newOperations.forEach(operation -> replicaTimestamp.updateVectorClock(operation.getUniqueTimestamp()));
        ledger.addAllUnstable(newOperations);
    }

    private boolean canOperationBeStable(Operation operation) {
        return this.valueTimestamp.isNewerThanOrEqualTo(operation.getPrevTimestamp());
    }

    private void executeOperation(Operation operation) {
        operation.accept(new ExecuteOperationVisitor(this.accounts));
        synchronized (valueTimestamp) {
            this.valueTimestamp.updateVectorClock(operation.getUniqueTimestamp());
            valueTimestamp.notifyAll(); // Notifies all threads waiting for an update to the value timestamp
        }
        log.debug("Value's timestamp: %s", valueTimestamp);
    }

    /**
     * Get an account by its ID.
     *
     * @param userId The ID of the account to get.
     * @return An optional with the Account, or an empty optional if the account cannot be found.
     */
    private Optional<Account> getAccount(String userId) {
        return Optional.ofNullable(accounts.get(userId));
    }

    private void createBroker() {
        Account broker = new Account(BROKER_ID);
        broker.increaseBalance(BROKER_INITIAL_AMOUNT);
        accounts.put(BROKER_ID, broker);
    }

    private void ensureServerIsActive() throws ServerUnavailableException {
        if (!active.get()) {
            throw new ServerUnavailableException(qualifier);
        }
    }
}
