package pt.tecnico.distledger.server.domain;

public class Account {

    private String userId;

    private int balance;

    public Account(String userId) {
        this.userId = userId;
        this.balance = 0;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void addBalance(int amount) {
        this.balance += amount;
    }

    public void removeBalance(int amount) {
        this.balance -= amount;
    }

    @Override
    public String toString() {
        return "Account{" +
                "userId='" + userId + '\'' +
                ", balance=" + balance +
                '}';
    }
}
