package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@RequiredArgsConstructor
@ToString
public class Account {

    private final Lock lock = new ReentrantLock();

    private final String userId;

    private int balance = 0;

    public void increaseBalance(int amount) {
        this.balance += amount;
    }

    public void decreaseBalance(int amount) {
        this.balance -= amount;
    }

}
