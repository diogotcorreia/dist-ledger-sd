package pt.tecnico.distledger.server;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.InvalidAmountException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferToTest {

    private static ServerState state;

    private static final String brokerId = "broker";

    private static final String userId = "user1";

    @BeforeEach
    @SneakyThrows
    void setup() {
        state = new ServerState();
        state.createAccount(userId);
    }

    @Test
    @SneakyThrows
    void transferToUser() {
        state.transferTo(brokerId, userId, 10);

        assertEquals(10, state.getAccounts().get(userId).getBalance());
        assertEquals(990, state.getAccounts().get(brokerId).getBalance());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void transferFromUser() {
        state.transferTo(brokerId, userId, 10);
        state.transferTo(userId, brokerId, 5);

        assertEquals(5, state.getAccounts().get(userId).getBalance());
        assertEquals(995, state.getAccounts().get(brokerId).getBalance());
        assertEquals(3, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void fromUserDoesNotExist() {
        final String userNotRegistered = "user37";

        assertThrows(AccountNotFoundException.class, () -> state.transferTo(userNotRegistered, brokerId, 10));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void toUserDoesNotExist() {
        final String userNotRegistered = "user37";

        assertThrows(AccountNotFoundException.class, () -> state.transferTo(brokerId, userNotRegistered, 10));
        assertEquals(1, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    void invalidAmount() {
        assertThrows(InvalidAmountException.class, () -> state.transferTo(brokerId, userId, -42));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void insufficientAmount() {
        assertThrows(InsufficientFundsException.class, () -> state.transferTo(brokerId, userId, 10000));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void deactivateServer() {
        state.deactivate();

        assertThrows(ServerUnavailableException.class, () -> state.transferTo(brokerId, userId, 10));
        assertEquals(0, state.getAccounts().get(userId).getBalance());
        assertEquals(1000, state.getAccounts().get(brokerId).getBalance());
        assertEquals(1, state.getLedger().size());
    }

}
