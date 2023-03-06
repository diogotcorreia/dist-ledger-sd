import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdminTest {

    private final String brokerId = "broker";
    private static ServerState state;

    @BeforeEach
    public void setUp() {
        state = new ServerState();
    }

    @Test
    void changeServerAvailability() {
        assertEquals(true, state.isActive());
        state.deactivate();
        assertEquals(false, state.isActive());
        state.activate();
        assertEquals(true, state.isActive());
    }

    @Test
    void changeServerToCurrentAvailability() {
        state.activate();
        assertEquals(true, state.isActive());
        state.deactivate();
        assertEquals(false, state.isActive());
        state.deactivate();
        assertEquals(false, state.isActive());
    }

    @Test
    void gossip() {
        state.gossip(); // does nothing for now
    }

    @Test
    void emptyLedgerStream() {
        assertEquals(0, state.getLedgerStream().count());
    }

    @Test
    @SneakyThrows
    void nonEmptyLedgerStream() {
        final String userId = "user1";
        state.createAccount(userId);
        assertEquals(1, state.getLedgerStream().count());
        state.transferTo(brokerId, userId, 10);
        assertEquals(2, state.getLedgerStream().count());
        state.transferTo(userId, brokerId, 10);
        assertEquals(3, state.getLedgerStream().count());
        state.deleteAccount(userId);
        assertEquals(4, state.getLedgerStream().count());
    }

}
