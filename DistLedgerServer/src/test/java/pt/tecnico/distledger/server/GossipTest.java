package pt.tecnico.distledger.server;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test propagation of the gossip algorithm.
 */
public class GossipTest {

    private static final String ACCOUNT_1 = "user1";
    private static final String ACCOUNT_2 = "user2";
    private static final String ACCOUNT_3 = "user3";

    private static ServerState state1;
    private static ServerState state2;
    private static ServerState state3;

    @BeforeEach
    void setup() {
        state1 = new ServerState("A");
        state2 = new ServerState("B");
        state3 = new ServerState("C");
    }

    @Test
    @SneakyThrows
    public void orderOfHappensBeforeIsRespectedBetween2ReplicasWith1User() {
        // ACCOUNT_1 happens before ACCOUNT_2

        val userClock = new VectorClock();
        val response1 = state1.createAccount(ACCOUNT_1, userClock);
        userClock.updateVectorClock(response1.vectorClock());

        assertEquals(clock(1, 0, 0), userClock);

        val response2 = state2.createAccount(ACCOUNT_2, userClock);
        userClock.updateVectorClock(response2.vectorClock());

        assertEquals(clock(1,1,0), userClock);

        // TODO investigate what vector clock needs to be sent here
        // FIXME operations should be cloned because 'stable' attribute is mutable; not a problem right now since we're not using it
        state2.addToLedger(state1.getLedger(), new VectorClock());

        val ledger = state2.getLedger();
        assertEquals(2, ledger.size());

        assertTrue(ledger.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledger.get(0).getAccount());
        assertTrue(ledger.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledger.get(1).getAccount());
    }

    @Test
    @SneakyThrows
    public void orderOfHappensBeforeIsRespectedBetween2ReplicasWith2Users() {
        // ACCOUNT_2 happens before ACCOUNT_3
        // ACCOUNT_1 happens before ACCOUNT_3

        val user1Clock = new VectorClock();
        val response = state2.createAccount(ACCOUNT_1, user1Clock);
        user1Clock.updateVectorClock(response.vectorClock());

        assertEquals(clock(0, 1, 0), user1Clock);

        val user2Clock = new VectorClock();
        val response2 = state1.createAccount(ACCOUNT_2, user2Clock);
        user2Clock.updateVectorClock(response2.vectorClock());

        assertEquals(clock(1, 0, 0), user2Clock);

        val response3 = state2.createAccount(ACCOUNT_3, user2Clock);
        user2Clock.updateVectorClock(response3.vectorClock());

        assertEquals(clock(1,2,0), user2Clock);

        // Gossip from replica A to B

        // TODO investigate what vector clock needs to be sent here
        // FIXME operations should be cloned because 'stable' attribute is mutable; not a problem right now since we're not using it
        state2.addToLedger(state1.getLedger(), new VectorClock());

        val ledgerB = state2.getLedger();
        assertEquals(3, ledgerB.size());

        assertTrue(ledgerB.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerB.get(0).getAccount());
        // ACCOUNT_2 could be here or on index 0, but our implementation places ACCOUNT_1 first.
        // Replica A will have the inverse order
        assertTrue(ledgerB.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerB.get(1).getAccount());
        assertTrue(ledgerB.get(2) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerB.get(2).getAccount());

        // Gossip from replica B to A

        // TODO investigate what vector clock needs to be sent here
        // FIXME operations should be cloned because 'stable' attribute is mutable; not a problem right now since we're not using it
        state1.addToLedger(state2.getLedger(), new VectorClock());

        val ledgerA = state2.getLedger();
        assertEquals(3, ledgerA.size());

        assertTrue(ledgerA.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerA.get(0).getAccount());
        // ACCOUNT_1 could be here or on index 0, but our implementation places ACCOUNT_2 first.
        // Replica B will have the inverse order
        assertTrue(ledgerA.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerA.get(1).getAccount());
        assertTrue(ledgerA.get(2) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerA.get(2).getAccount());
    }

    /**
     * Utility function to create a vector clock for 3 replicas, A, B and C.
     *
     * @param clockA Timestamp value for replica A.
     * @param clockB Timestamp value for replica B.
     * @param clockC Timestamp value for replica C.
     * @return The constructed vector clock.
     */
    private VectorClock clock(int clockA, int clockB, int clockC) {
        val clock = new VectorClock();
        clock.setValue("A", clockA);
        clock.setValue("B", clockB);
        clock.setValue("C", clockC);
        return clock;
    }

}

