import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateAccountTest {

    private static ServerState state;
    private static final String userId = "user1";

    @BeforeEach
    void setUp() {
        state = new ServerState();
    }

    @Test
    @SneakyThrows
    void createBroker() {
        final String brokerId = "broker";
        assertEquals(1, state.getAccounts().size());
        assertEquals(1000, state.getBalance(brokerId));
    }

    @Test
    @SneakyThrows
    void createAccount() {
        state.createAccount(userId);

        assertEquals(2, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void createAccounts() {
        state.createAccount(userId);
        final String userId2 = "user2";
        state.createAccount(userId2);

        assertEquals(3, state.getAccounts().size());
        assertEquals(0, state.getBalance(userId));
        assertEquals(0, state.getBalance(userId2));
        assertEquals(2, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    void createAccountTwice() {
        state.createAccount(userId);

        assertThrows(AccountAlreadyExistsException.class, () -> state.createAccount(userId));
        assertEquals(2, state.getAccounts().size());
        assertEquals(1, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    void cannotCreateAccount() {
        state.deactivate();

        assertThrows(ServerUnavailableException.class, () -> state.createAccount(userId));
        assertEquals(1, state.getAccounts().size());
        assertEquals(0, state.getLedger().size());
    }

}
