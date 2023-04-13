package pt.tecnico.distledger.server;

import lombok.val;
import org.junit.jupiter.api.Test;
import pt.tecnico.distledger.common.VectorClock;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VectorClockTest {

    @Test
    public void getNonExistentTimestamp() {
        val vectorClock = new VectorClock();

        assertEquals(0, vectorClock.getValue("A"));
        assertEquals(0, vectorClock.getValue("B"));
    }

    @Test
    public void createVectorClockFromMap() {
        val map = new HashMap<String, Integer>();
        map.put("A", 5);
        map.put("B", 7);

        val vectorClock = new VectorClock(map);
        assertNotSame(vectorClock.getTimestamps(), map);

        assertEquals(5, vectorClock.getValue("A"));
        assertEquals(7, vectorClock.getValue("B"));

        vectorClock.incrementClock("A");
        vectorClock.incrementClock("B");
        map.put("A", 10);
        map.put("B", 14);

        assertEquals(6, vectorClock.getValue("A"));
        assertEquals(8, vectorClock.getValue("B"));
        assertEquals(10, map.get("A"));
        assertEquals(14, map.get("B"));
    }

    @Test
    public void incrementEmptyVectorClock() {
        val vectorClock = new VectorClock();

        vectorClock.incrementClock("A");

        assertEquals(1, vectorClock.getValue("A"));
    }

    @Test
    public void incrementExistingTimestampInVectorClock() {
        val vectorClock = new VectorClock();

        vectorClock.setValue("A", 10);
        vectorClock.incrementClock("A");

        assertEquals(11, vectorClock.getValue("A"));
    }

    @Test
    public void timestampGetterReturnsImmutableMap() {
        val vectorClock = new VectorClock();

        vectorClock.setValue("A", 6);

        assertThrows(UnsupportedOperationException.class, () -> vectorClock.getTimestamps().put("A", 9));
        assertEquals(6, vectorClock.getValue("A"));
    }

    @Test
    public void getAndSetTimestamp() {
        val vectorClock = new VectorClock();

        vectorClock.setValue("A", 8);
        vectorClock.setValue("B", 10);

        assertEquals(8, vectorClock.getValue("A"));
        assertEquals(10, vectorClock.getValue("B"));
    }

    @Test
    public void cloneTimestamp() {
        val vectorClock = new VectorClock();

        vectorClock.setValue("A", 8);

        val otherVectorClock = vectorClock.clone();
        otherVectorClock.setValue("A", 15);
        otherVectorClock.setValue("B", 3);

        assertEquals(8, vectorClock.getValue("A"));
        assertEquals(0, vectorClock.getValue("B"));

        assertEquals(15, otherVectorClock.getValue("A"));
        assertEquals(3, otherVectorClock.getValue("B"));
    }

    @Test
    public void updateVectorClock() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 3);

        val vectorClock2 = new VectorClock();
        vectorClock2.setValue("B", 4);
        vectorClock2.setValue("C", 8);
        vectorClock2.setValue("D", 2);

        vectorClock1.updateVectorClock(vectorClock2);

        assertEquals(9, vectorClock1.getValue("A"));
        assertEquals(6, vectorClock1.getValue("B"));
        assertEquals(8, vectorClock1.getValue("C"));
        assertEquals(2, vectorClock1.getValue("D"));

        assertEquals(4, vectorClock2.getValue("B"));
        assertEquals(8, vectorClock2.getValue("C"));
        assertEquals(2, vectorClock2.getValue("D"));
    }

    @Test
    public void isNewerThanOrEqualsTo() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 3);

        val vectorClock2 = new VectorClock();
        vectorClock2.setValue("B", 4);
        vectorClock2.setValue("C", 2);

        assertTrue(vectorClock1.isNewerThanOrEqualTo(vectorClock2));
    }

    @Test
    public void isOlderThan() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 1);

        val vectorClock2 = new VectorClock();
        vectorClock2.setValue("B", 4);
        vectorClock2.setValue("C", 2);

        assertFalse(vectorClock1.isNewerThanOrEqualTo(vectorClock2));
    }

    @Test
    public void isOlderThanWithMissingTimestamp() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 4);

        val vectorClock2 = new VectorClock();
        vectorClock2.setValue("B", 4);
        vectorClock2.setValue("C", 2);
        vectorClock2.setValue("D", 3);

        assertFalse(vectorClock1.isNewerThanOrEqualTo(vectorClock2));
    }

    @Test
    public void areVectorClocksEqualWithAndWithoutZeroTimestamp() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 4);

        val vectorClock2 = new VectorClock();
        vectorClock2.setValue("A", 9);
        vectorClock2.setValue("B", 6);
        vectorClock2.setValue("C", 4);
        vectorClock2.setValue("D", 0);

        assertEquals(vectorClock1, vectorClock2);
    }

    @Test
    public void updateVectorClockWithZeroTimestamp() {
        val vectorClock1 = new VectorClock();
        vectorClock1.setValue("A", 9);
        vectorClock1.setValue("B", 6);
        vectorClock1.setValue("C", 1);
        vectorClock1.setValue("C", 0);

        // TODO: change assert in C
        assertEquals(9, vectorClock1.getValue("A"));
        assertEquals(6, vectorClock1.getValue("B"));
        assertEquals(0, vectorClock1.getValue("C"));
    }
}
