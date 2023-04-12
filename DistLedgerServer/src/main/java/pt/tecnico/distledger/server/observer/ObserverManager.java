package pt.tecnico.distledger.server.observer;

public interface ObserverManager {
    void registerObserver(Observer observer);

    void removeObserver(Observer observer);

    void notifyObservers();
}
