import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.exceptions.AccountNotFoundException;
import pt.tecnico.distledger.server.exceptions.InsufficientFundsException;
import pt.tecnico.distledger.server.exceptions.ServerUnavailableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransferToTest {

    private static ServerState state;

    private static final String brokerId = "broker";


    @BeforeEach
    public void setUp() {
        state = new ServerState();
    }

    @Test
    @SneakyThrows
    public void TransferToUser() {
        final String userId = "user1";
        state.createAccount(userId);
        state.transferTo(brokerId, userId, 10);
        assertEquals(10, state.getAccounts().get(userId).getBalance());
        assertEquals(990, state.getAccounts().get(brokerId).getBalance());
        assertEquals(2, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void transferFromUser() {
        final String userId = "user1";
        state.createAccount(userId);
        state.transferTo(brokerId, userId, 10);

        state.transferTo(userId, brokerId, 5);
        assertEquals(5, state.getAccounts().get(userId).getBalance());
        assertEquals(995, state.getAccounts().get(brokerId).getBalance());
        assertEquals(3, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void fromUserDoesNotExist() {
        final String userId = "user1";
        assertThrows(AccountNotFoundException.class, () -> state.transferTo(brokerId, userId, 10));
        assertEquals(0, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void toUserDoesNotExist() {
        final String userId = "user1";
        assertThrows(AccountNotFoundException.class, () -> state.transferTo(userId, brokerId, 10));
        assertEquals(0, state.getLedger().size());

    }

    @Test
    @SneakyThrows
    public void invalidAmount() {
        final String userId = "user1";
        state.createAccount(userId);
        // TODO
        // assertThrows(InvalidAmountException.class, () -> state.transferTo(brokerId, userId, -69));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void insufficientAmount() {
        final String userId = "user1";
        state.createAccount(userId);
        assertThrows(InsufficientFundsException.class, () -> state.transferTo(brokerId, userId, 10000));
        assertEquals(1, state.getLedger().size());
    }

    @Test
    @SneakyThrows
    public void deactivateServer() {
        final String userId = "user1";
        state.createAccount(userId);
        state.deactivate();
        assertThrows(ServerUnavailableException.class, () -> state.transferTo(brokerId, userId, 10));
        assertEquals(0, state.getAccounts().get(userId).getBalance());
        assertEquals(1000, state.getAccounts().get(brokerId).getBalance());
        assertEquals(1, state.getLedger().size());
    }

}
