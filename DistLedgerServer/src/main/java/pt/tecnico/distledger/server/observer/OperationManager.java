package pt.tecnico.distledger.server.observer;

import org.jetbrains.annotations.NotNull;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.visitor.ExecuteOperationVisitor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class OperationManager implements ObserverManager {
    private final @NotNull List<Observer> observers = new CopyOnWriteArrayList<>();
    private final @NotNull VectorClock valueTimestamp;
    private final @NotNull VectorClock replicaTimestamp;
    private final @NotNull ExecuteOperationVisitor executeOperationVisitor;

    public OperationManager(
            @NotNull Map<String, Account> accounts,
            @NotNull VectorClock valueTimestamp,
            @NotNull VectorClock replicaTimestamp
    ) {
        this.executeOperationVisitor = new ExecuteOperationVisitor(accounts);
        this.valueTimestamp = valueTimestamp;
        this.replicaTimestamp = replicaTimestamp;
    }

    @Override
    public void registerObserver(@NotNull Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(@NotNull Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        boolean changed;
        do {
            changed = false;
            for (Observer observer : observers) {
                if (observer.update(executeOperationVisitor, valueTimestamp)) {
                    valueTimestamp.updateVectorClock(replicaTimestamp);
                    changed = true;
                    removeObserver(observer);

                    // TODO somehow move operation
                    break;
                }
            }
        } while (changed);
    }
}
