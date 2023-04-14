package pt.tecnico.distledger.server.domain;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.VisibleForTesting;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

@RequiredArgsConstructor
@CustomLog(topic = "Ledger")
public class Ledger {

    private final Predicate<Operation> stable;
    private final Consumer<Operation> executorCallback;

    private final List<Operation> ledger = new CopyOnWriteArrayList<>();
    private final Set<VectorClock> operationIdList = ConcurrentHashMap.newKeySet();
    private int stableOperationCount = 0;

    private final ReentrantReadWriteLock moveLock = new ReentrantReadWriteLock();

    /**
     * Add a single (unstable) operation to the ledger. It will be added to the end of the ledger. Then, try to
     * stabilize as many operations as possible.
     *
     * @param operation The operation to add to the ledger.
     * @throws IllegalArgumentException If the operation is already stable (its attribute 'stable' is true) or if it
     *                                  already in the ledger.
     */
    public void addUnstable(Operation operation) {
        if (operation.isStable()) {
            throw new IllegalArgumentException("Operation to add to ledger must be unstable");
        }
        if (!operationIdList.add(operation.getUniqueTimestamp())) {
            throw new IllegalArgumentException("Operation already in ledger");
        }
        if (this.stable.test(operation)) {
            this.moveLock.writeLock().lock();
            try {
                this.ledger.add(this.stableOperationCount, operation);
                operation.setStable(true);
                this.executorCallback.accept(operation);
                this.stableOperationCount++;
            } finally {
                this.moveLock.writeLock().unlock();
            }
            log.debug(
                    "The %s operation with timestamp %s has now been stabilized",
                    operation.getType(),
                    operation.getUniqueTimestamp()
            );
        } else {
            // The underlying structure already gives us synchronization guarantees
            this.ledger.add(operation);
        }
    }

    /**
     * Add a list of (unstable) operations to the ledger. They will be added to the end of the ledger. Operations
     * already in the ledger will be silently ignored. Then, try to stabilize as many operations as possible.
     *
     * @param operations The operations to add to the ledger.
     * @throws IllegalArgumentException If any of the given operations is already stable (its attribute 'stable' is
     *                                  true).
     */
    public void addAllUnstable(Collection<Operation> operations) {
        if (operations.stream().anyMatch(Operation::isStable)) {
            throw new IllegalArgumentException("All operations to add to ledger must be unstable");
        }

        operations.forEach(operation -> {
            if (operationIdList.add(operation.getUniqueTimestamp())) {
                this.ledger.add(operation);
            }
        });
        stabilizeOperations();
    }

    public void operateOverLedger(OperationVisitor visitor) {
        this.moveLock.readLock().lock();
        try {
            this.ledger.forEach(operation -> operation.accept(visitor));
        } finally {
            this.moveLock.readLock().unlock();
        }
    }

    public void operateOverLedger(OperationVisitor visitor, Predicate<Operation> filter) {
        this.moveLock.readLock().lock();
        try {
            this.ledger.stream()
                    .filter(filter)
                    .forEach(operation -> operation.accept(visitor));
        } finally {
            this.moveLock.readLock().unlock();
        }
    }

    @VisibleForTesting
    public int size() {
        // No need for locks, the underlying structure provides synchornization guarantees.
        return this.ledger.size();
    }

    /**
     * Try to mark operations as stable whenever possible.
     */
    private void stabilizeOperations() {
        this.moveLock.readLock().lock();
        try {
            for (int i = this.stableOperationCount; i < this.ledger.size(); i++) {
                Operation operation = this.ledger.get(i);
                if (this.stable.test(operation)) {
                    this.moveLock.readLock().unlock();
                    this.moveLock.writeLock().lock();
                    try {
                        // Check if another thread got the lock and stabilized this operation meanwhile
                        if (!operation.isStable()) {
                            Collections.swap(this.ledger, this.stableOperationCount, i);
                            operation.setStable(true);
                            this.executorCallback.accept(operation);
                            i = this.stableOperationCount;
                            this.stableOperationCount++;
                        }
                        this.moveLock.readLock().lock();
                    } finally {
                        this.moveLock.writeLock().unlock();
                    }
                    log.debug(
                            "The %s operation with timestamp %s has now been stabilized",
                            operation.getType(),
                            operation.getUniqueTimestamp()
                    );
                }
            }
        } finally {
            this.moveLock.readLock().unlock();
        }
    }

}
