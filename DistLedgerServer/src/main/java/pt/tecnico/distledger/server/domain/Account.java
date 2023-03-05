package pt.tecnico.distledger.server.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class Account {

    private final String userId;

    private int balance = 0;

    public void increaseBalance(int amount) {
        this.balance += amount;
    }

    public void decreaseBalance(int amount) {
        this.balance -= amount;
    }

}
