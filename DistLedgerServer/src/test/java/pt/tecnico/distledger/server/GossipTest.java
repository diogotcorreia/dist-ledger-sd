package pt.tecnico.distledger.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.visitor.OperationVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test propagation of the gossip algorithm.
 */
public class GossipTest {

    private static final String SERVER_A = "A";
    private static final String SERVER_B = "B";
    private static final String SERVER_C = "C";

    private static final String ACCOUNT_1 = "user1";
    private static final String ACCOUNT_2 = "user2";
    private static final String ACCOUNT_3 = "user3";

    private static ServerState state1;
    private static ServerState state2;
    private static ServerState state3;

    @BeforeEach
    void setup() {
        state1 = new ServerState(SERVER_A);
        state2 = new ServerState(SERVER_B);
        state3 = new ServerState(SERVER_C);
    }

    @Test
    @SneakyThrows
    public void orderOfHappensBeforeIsRespectedBetween2ReplicasWith1User() {
        // ACCOUNT_1 happens before ACCOUNT_2

        val userClock = new VectorClock();
        val response1 = state1.createAccount(ACCOUNT_1, userClock.clone());
        userClock.updateVectorClock(response1.vectorClock());

        assertEquals(clock(1, 0, 0), userClock);

        val response2 = state2.createAccount(ACCOUNT_2, userClock.clone());
        userClock.updateVectorClock(response2.vectorClock());

        assertEquals(clock(1, 1, 0), userClock);

        // Gossip from replica A to B

        propagateGossip(state1, state2);

        val ledger = getLedgerOfReplica(state2);
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
        val response = state2.createAccount(ACCOUNT_1, user1Clock.clone());
        user1Clock.updateVectorClock(response.vectorClock());

        assertEquals(clock(0, 1, 0), user1Clock);

        val user2Clock = new VectorClock();
        val response2 = state1.createAccount(ACCOUNT_2, user2Clock.clone());
        user2Clock.updateVectorClock(response2.vectorClock());

        assertEquals(clock(1, 0, 0), user2Clock);

        val response3 = state2.createAccount(ACCOUNT_3, user2Clock.clone());
        user2Clock.updateVectorClock(response3.vectorClock());

        assertEquals(clock(1, 2, 0), user2Clock);

        // Gossip from replica A to B
        propagateGossip(state1, state2);

        val ledgerB = getLedgerOfReplica(state2);
        assertEquals(3, ledgerB.size());

        assertTrue(ledgerB.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerB.get(0).getAccount());
        assertTrue(ledgerB.get(0).isStable());
        // ACCOUNT_2 could be here or on index 0, but our implementation places ACCOUNT_1 first.
        // Replica A will have the inverse order
        assertTrue(ledgerB.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerB.get(1).getAccount());
        assertTrue(ledgerB.get(1).isStable());
        assertTrue(ledgerB.get(2) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerB.get(2).getAccount());
        assertTrue(ledgerB.get(2).isStable());

        // Gossip from replica B to A

        propagateGossip(state2, state1);

        val ledgerA = getLedgerOfReplica(state1);
        assertEquals(3, ledgerA.size());

        assertTrue(ledgerA.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerA.get(0).getAccount());
        assertTrue(ledgerB.get(0).isStable());
        // ACCOUNT_1 could be here or on index 0, but our implementation places ACCOUNT_2 first.
        // Replica B will have the inverse order
        assertTrue(ledgerA.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerA.get(1).getAccount());
        assertTrue(ledgerB.get(1).isStable());
        assertTrue(ledgerA.get(2) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerA.get(2).getAccount());
        assertTrue(ledgerB.get(2).isStable());
    }

    @Test
    @SneakyThrows
    public void correctGossipMessagesAreSentBetween3Replicas() {
        // Single user per action (empty vector clock)
        // ACCOUNT_1 created on A
        // ACCOUNT_2 created on B
        // ACCOUNT_3 created on C
        // Gossip from B to A
        // Gossip from C to B
        // Gossip from B to A

        val user1Clock = new VectorClock();
        val response = state1.createAccount(ACCOUNT_1, user1Clock.clone());
        user1Clock.updateVectorClock(response.vectorClock());

        assertEquals(clock(1, 0, 0), user1Clock);

        val user2Clock = new VectorClock();
        val response2 = state2.createAccount(ACCOUNT_2, user2Clock.clone());
        user2Clock.updateVectorClock(response2.vectorClock());

        assertEquals(clock(0, 1, 0), user2Clock);

        val user3Clock = new VectorClock();
        val response3 = state3.createAccount(ACCOUNT_3, user3Clock.clone());
        user3Clock.updateVectorClock(response3.vectorClock());

        assertEquals(clock(0, 0, 1), user3Clock);

        // First gossip from replica B to A

        val toSendBtoA1 = getOperationsToSendThroughGossip(state2, SERVER_A);
        assertEquals(1, toSendBtoA1.size());
        assertTrue(toSendBtoA1.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_2, toSendBtoA1.get(0).getAccount());

        propagateGossip(state2, state1);
        state2.updateGossipTimestamp(SERVER_A, state2.getReplicaTimestamp().clone());

        val ledgerA1 = getLedgerOfReplica(state1);
        assertEquals(2, ledgerA1.size());

        assertTrue(ledgerA1.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerA1.get(0).getAccount());
        assertTrue(ledgerA1.get(0).isStable());
        assertTrue(ledgerA1.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerA1.get(1).getAccount());
        assertTrue(ledgerA1.get(1).isStable());

        // Gossip from replica C to B

        val toSendCtoB = getOperationsToSendThroughGossip(state3, SERVER_B);
        assertEquals(1, toSendCtoB.size());
        assertTrue(toSendCtoB.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_3, toSendCtoB.get(0).getAccount());

        propagateGossip(state3, state2);
        state3.updateGossipTimestamp(SERVER_B, state3.getReplicaTimestamp().clone());

        val ledgerB = getLedgerOfReplica(state2);
        assertEquals(2, ledgerB.size());

        assertTrue(ledgerB.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerB.get(0).getAccount());
        assertTrue(ledgerB.get(0).isStable());
        assertTrue(ledgerB.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerB.get(1).getAccount());
        assertTrue(ledgerB.get(1).isStable());

        // Second gossip from replica B to A

        val toSendBtoA2 = getOperationsToSendThroughGossip(state2, SERVER_A);
        assertEquals(1, toSendBtoA2.size());
        assertTrue(toSendBtoA2.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_3, toSendBtoA2.get(0).getAccount());

        propagateGossip(state2, state1);
        state2.updateGossipTimestamp(SERVER_A, state2.getReplicaTimestamp().clone());

        val ledgerA2 = getLedgerOfReplica(state1);
        assertEquals(3, ledgerA2.size());

        assertTrue(ledgerA2.get(0) instanceof CreateOp);
        assertEquals(ACCOUNT_1, ledgerA2.get(0).getAccount());
        assertTrue(ledgerA2.get(0).isStable());
        assertTrue(ledgerA2.get(1) instanceof CreateOp);
        assertEquals(ACCOUNT_2, ledgerA2.get(1).getAccount());
        assertTrue(ledgerA2.get(1).isStable());
        assertTrue(ledgerA2.get(2) instanceof CreateOp);
        assertEquals(ACCOUNT_3, ledgerA2.get(2).getAccount());
        assertTrue(ledgerA2.get(2).isStable());
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

    /**
     * Simulate gossip propagation between two replicas. Skips gRPC for simplicity.
     *
     * @param replicaFrom The replica sending the gossip message.
     * @param replicaTo   The replica receiving the operations.
     */
    @SneakyThrows
    private void propagateGossip(ServerState replicaFrom, ServerState replicaTo) {
        val visitor = new ClonedOperationVisitor(false);
        replicaFrom.getLedger().operateOverLedger(visitor);
        replicaTo.addToLedger(visitor.getOperations());
    }

    /**
     * Get operations of the given replica's ledger. Preserves "stable" attribute of operations.
     *
     * @param replica The replica to get the ledger of.
     * @return The operations in the ledger of the replica.
     */
    private List<Operation> getLedgerOfReplica(ServerState replica) {
        val visitor = new ClonedOperationVisitor(true);
        replica.getLedger().operateOverLedger(visitor);
        return visitor.getOperations();
    }

    /**
     * Get operations that would be sent between two replicas if a gossip were to happen.
     *
     * @param replica              The replica to get the operations from.
     * @param destinationQualifier The qualifier of the destination replica.
     * @return The operations that would be sent between the two replicas.
     */
    private List<Operation> getOperationsToSendThroughGossip(ServerState replica, String destinationQualifier) {
        val visitor = new ClonedOperationVisitor(false);
        replica.operateOverLedgerToPropagateToReplica(visitor, destinationQualifier);
        return visitor.getOperations();
    }

    @RequiredArgsConstructor
    static class ClonedOperationVisitor extends OperationVisitor {
        private final boolean keepStable;

        @Getter
        private final List<Operation> operations = new ArrayList<>();

        @Override
        public void visit(CreateOp operation) {
            operations.add(
                    new CreateOp(
                            operation.getAccount(),
                            operation.getPrevTimestamp(),
                            operation.getUniqueTimestamp(),
                            keepStable && operation.isStable()
                    )
            );
        }


        @Override
        public void visit(TransferOp operation) {
            operations.add(
                    new TransferOp(
                            operation.getAccount(),
                            operation.getDestAccount(),
                            operation.getAmount(),
                            operation.getPrevTimestamp(),
                            operation.getUniqueTimestamp(),
                            keepStable && operation.isStable()
                    )
            );
        }
    }

}
