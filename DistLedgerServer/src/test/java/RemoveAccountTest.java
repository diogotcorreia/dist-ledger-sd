import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.CannotRemoveNotEmptyAccountException;
import pt.tecnico.distledger.server.exceptions.CannotRemoveProtectedAccountException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RemoveAccountTest {

    private static ServerState state;

    private static final String brokerId = "broker";

    @BeforeEach
    public void setUp() {
        state = new ServerState();
    }

    @Test
    @SneakyThrows
    public void RemoveAccount() {
        final String userId = "user1";
        state.createAccount(userId);
        state.deleteAccount(userId);

        assertEquals(1, state.getAccounts().size());
    }

    @Test
    @SneakyThrows
    public void deleteAccounts() {
        final String userId1 = "user1";
        final String userId2 = "user2";
        state.createAccount(userId1);
        state.createAccount(userId2);

        state.deleteAccount(userId2);
        assertEquals(2, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId1));

        state.deleteAccount(userId1);
        assertEquals(1, state.getAccounts().size());
    }

    @Test
    @SneakyThrows
    public void deleteAccountTwice() {
        final String userId = "user1";
        state.createAccount(userId);
        state.deleteAccount(userId);
        assertThrows(AccountNotFoundException.class, () -> state.deleteAccount(userId));
        assertEquals(1, state.getAccounts().size());
    }

    @Test
    @SneakyThrows
    public void deleteAccountWithMoney() {
        final String userId = "user1";
        state.createAccount(userId);
        state.transferTo(brokerId, userId, 50);
        assertThrows(CannotRemoveNotEmptyAccountException.class, () -> state.deleteAccount(userId));
        assertEquals(2, state.getAccounts().size());
    }

    @Test
    @SneakyThrows
    public void cannotDeleteBroker() {
        final String userId = "user1";
        state.createAccount(userId);
        state.transferTo(brokerId, userId, 1000);
        assertEquals(0, state.getAccounts().get(brokerId).getBalance());
        assertThrows(CannotRemoveProtectedAccountException.class, () -> state.deleteAccount(brokerId));
        assertEquals(2, state.getAccounts().size());
    }

    @Test
    @SneakyThrows
    public void UnavailableServer() {
        final String userId = "user2";
        state.createAccount(userId);
        state.deactivate();
        assertThrows(ServerUnavailableException.class, () -> state.deleteAccount(userId));
        assertEquals(2, state.getAccounts().size());
        state.activate();
        state.deleteAccount(userId);
        assertEquals(1, state.getAccounts().size());
    }
}
