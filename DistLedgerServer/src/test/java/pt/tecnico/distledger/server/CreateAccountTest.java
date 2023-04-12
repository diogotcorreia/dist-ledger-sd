package pt.tecnico.distledger.server;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateAccountTest {

    private static ServerState state;
    private static final String userId = "user1";

    @BeforeEach
    void setup() {
        state = new ServerState();
    }

    @Test
    @SneakyThrows
    void createBroker() {
        final String brokerId = "broker";
        assertEquals(1, state.getAccounts().size());
        val result = state.getBalance(brokerId, new VectorClock());
        assertEquals(1000, result.value());
        // TODO test vector clock
    }

    @Test
    @SneakyThrows
    void createAccount() {
        state.createAccount(userId, new VectorClock());

        assertEquals(2, state.getAccounts().size());
        val result = state.getBalance(userId, new VectorClock());
        assertEquals(0, result.value());
        // TODO test vector clock
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void createAccounts() {
        state.createAccount(userId, new VectorClock());
        final String userId2 = "user2";
        state.createAccount(userId2, new VectorClock());

        assertEquals(3, state.getAccounts().size());
        val result1 = state.getBalance(userId, new VectorClock());
        assertEquals(0, result1.value());
        // TODO test vector clock
        val result2 = state.getBalance(userId2, new VectorClock());
        assertEquals(0, result2.value());
        // TODO test vector clock
        assertEquals(2, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    void createAccountTwice() {
        state.createAccount(userId, new VectorClock());

        assertThrows(AccountAlreadyExistsException.class, () -> state.createAccount(userId, new VectorClock()));
        assertEquals(2, state.getAccounts().size());
        assertEquals(1, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    void cannotCreateAccount() {
        state.deactivate();

        assertThrows(ServerUnavailableException.class, () -> state.createAccount(userId, new VectorClock()));
        assertEquals(1, state.getAccounts().size());
        assertEquals(0, state.getLedger().size());
    }

}
