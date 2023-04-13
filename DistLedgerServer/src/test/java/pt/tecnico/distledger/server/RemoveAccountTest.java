package pt.tecnico.distledger.server;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountNotEmptyException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.AccountProtectedException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
class RemoveAccountTest {

    private static ServerState state;

    private static final String brokerId = "broker";

    private final String userId = "user1";

    @BeforeEach
    @SneakyThrows
    void setup() {
        state = new ServerState();
        state.createAccount(userId, new VectorClock());
    }

    @Test
    @SneakyThrows
    void deleteAccount() {
        state.deleteAccount(userId);

        assertEquals(1, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void deleteAccounts() {
        final String userId2 = "user2";
        state.createAccount(userId2, new VectorClock());

        state.deleteAccount(userId2);
        assertEquals(2, state.getAccounts().size());
        val result = state.getBalance(userId, new VectorClock());
        assertEquals(0, result.value());

        state.deleteAccount(userId);
        assertEquals(1, state.getAccounts().size());
        assertEquals(4, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void deleteAccountTwice() {
        state.deleteAccount(userId);

        assertThrows(AccountNotFoundException.class, () -> state.deleteAccount(userId));
        assertEquals(1, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void deleteAccountWithMoney() {
        state.transferTo(brokerId, userId, 50, new VectorClock());

        assertThrows(AccountNotEmptyException.class, () -> state.deleteAccount(userId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void cannotDeleteBroker() {
        state.transferTo(brokerId, userId, 1000, new VectorClock());

        assertEquals(0, state.getAccounts().get(brokerId).getBalance());
        assertThrows(AccountProtectedException.class, () -> state.deleteAccount(brokerId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void unavailableServer() {
        state.deactivate();

        assertThrows(ServerUnavailableException.class, () -> state.deleteAccount(userId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(1, state.getLedger().size());

        state.activate();
        state.deleteAccount(userId);
        assertEquals(1, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }
}
