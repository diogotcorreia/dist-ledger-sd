package pt.tecnico.distledger.server.observer;

import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.Account;
import pt.tecnico.distledger.server.visitor.ExecuteOperationVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OperationManager implements ObserverManager {
    private final List<Observer> observers = new ArrayList<>();
    private final VectorClock valueTimestamp;
    private final ExecuteOperationVisitor executeOperationVisitor;

    public OperationManager(Map<String, Account> accounts, VectorClock valueTimestamp) {
        this.executeOperationVisitor = new ExecuteOperationVisitor(accounts);
        this.valueTimestamp = valueTimestamp;
    }

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            if (observer.update(executeOperationVisitor, valueTimestamp)) {
                removeObserver(observer);
                notifyObservers(); // allows for a previous operation to be executed if a following operation which it depends on is resolved
                break;
            }
        }
    }
}
