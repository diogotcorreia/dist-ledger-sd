import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountNotEmptyException;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.AccountProtectedException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoveAccountTest {

    private static ServerState state;

    private static final String brokerId = "broker";

    private final String userId = "user1";

    @BeforeEach
    @SneakyThrows
    void setUp() {
        state = new ServerState();
        state.createAccount(userId);
    }

    @Test
    @SneakyThrows
    void RemoveAccount() {
        state.deleteAccount(userId);

        assertEquals(1, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void deleteAccounts() {
        final String userId2 = "user2";
        state.createAccount(userId2);

        state.deleteAccount(userId2);
        assertEquals(2, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId));

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
        state.transferTo(brokerId, userId, 50);

        assertThrows(AccountNotEmptyException.class, () -> state.deleteAccount(userId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void cannotDeleteBroker() {
        state.transferTo(brokerId, userId, 1000);

        assertEquals(0, state.getAccounts().get(brokerId).getBalance());
        assertThrows(AccountProtectedException.class, () -> state.deleteAccount(brokerId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void UnavailableServer() {
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
