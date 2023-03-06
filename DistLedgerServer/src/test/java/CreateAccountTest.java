import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateAccountTest {

    private static ServerState state;

    @BeforeEach
    public void setUp() {
        state = new ServerState();
    }

    @Test
    @SneakyThrows
    public void createBroker() {
        final String brokerId = "broker";
        assertEquals(1, state.getAccounts().size());
        assertEquals(1000, state.getBalance(brokerId));
    }

    @Test
    @SneakyThrows
    public void createAccount() {
        final String userId = "user1";
        state.createAccount(userId);

        assertEquals(2, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void createAccounts() {
        final String userId1 = "user1";
        final String userId2 = "user2";
        state.createAccount(userId1);
        state.createAccount(userId2);

        assertEquals(3, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId1));
        assertEquals(0, state.getBalance(userId2));
        assertEquals(2, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    public void createAccountTwice() {
        final String userId = "user1";
        state.createAccount(userId);
        assertThrows(AccountAlreadyExistsException.class, () -> state.createAccount(userId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(1, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    public void cannotCreateAccount() {
        state.deactivate();
        final String userId = "user1";
        assertThrows(ServerUnavailableException.class, () -> state.createAccount(userId));
        assertEquals(1, state.getAccounts().size());
        assertEquals(0, state.getLedger().size());
    }

}
