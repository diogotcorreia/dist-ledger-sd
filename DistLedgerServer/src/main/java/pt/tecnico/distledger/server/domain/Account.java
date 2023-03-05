package pt.tecnico.distledger.server.domain;

import lombok.Getter;

@Getter
public class Account {

    private String userId;

    private int balance;

    public Account(String userId) {
        this.userId = userId;
        this.balance = 0;
    }

    public void increaseBalance(int amount) {
        this.balance += amount;
    }

    public void decreaseBalance(int amount) {
        this.balance -= amount;
    }

    @Override
    public String toString() {
        return "Account{" + "userId='" + userId + '\'' + ", balance=" + balance + '}';
    }

}
