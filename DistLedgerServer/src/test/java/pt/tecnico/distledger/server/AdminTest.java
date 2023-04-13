package pt.tecnico.distledger.server;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTest {

    private final String brokerId = "broker";
    private static ServerState state;

    @BeforeEach
    public void setup() {
        state = new ServerState();
    }

    @Test
    void changeServerAvailability() {
        assertTrue(state.getActive().get());
        state.deactivate();
        assertFalse(state.getActive().get());
        state.activate();
        assertTrue(state.getActive().get());
    }

    @Test
    void changeServerToCurrentAvailability() {
        state.activate();
        assertTrue(state.getActive().get());
        state.deactivate();
        assertFalse(state.getActive().get());
        state.deactivate();
        assertFalse(state.getActive().get());
    }

    @Test
    void emptyLedgerStream() {
        assertEquals(0, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    void nonEmptyLedgerStream() {
        final String userId = "user1";
        state.createAccount(userId, new VectorClock());
        assertEquals(1, state.getLedger().size());
        state.transferTo(brokerId, userId, 10, new VectorClock());
        assertEquals(2, state.getLedger().size());
        state.transferTo(userId, brokerId, 10, new VectorClock());
        assertEquals(3, state.getLedger().size());
        state.deactivate();
        assertEquals(3, state.getLedger().size());
        state.activate();
        assertEquals(3, state.getLedger().size());
        state.getBalance(userId, new VectorClock());
        assertEquals(3, state.getLedger().size());
    }

}
