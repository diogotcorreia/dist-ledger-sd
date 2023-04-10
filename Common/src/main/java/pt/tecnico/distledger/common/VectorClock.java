package pt.tecnico.distledger.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Class abtracting the logic of a vector clock.
 * <p>
 * The internal implementation uses a Map to avoid having a canonic order of the servers.
 */
@Getter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class VectorClock {

    private @NotNull Map<String, Integer> timestamps = new HashMap<>();

    /**
     * Get the value of the clock for a certain server. If the server does not exist in the vector clock, zero is
     * returned.
     *
     * @param serverId The server to get the timestamp for.
     * @return The timestamp or 0 if the server is not in the vector clock.
     */
    public int getValue(@NotNull String serverId) {
        return timestamps.getOrDefault(serverId, 0);
    }

    /**
     * Increment the server's timestamp by 1. If the server does not exist in the vector clock, its timestamp is set to
     * 1.
     *
     * @param serverId The server to increment the timestamp of.
     */
    public void incrementClock(@NotNull String serverId) {
        timestamps.merge(serverId, 1, Integer::sum);
        timestamps.compute(serverId, (k, v) -> v == null ? 1 : v + 1);
    }

    /**
     * For each entry of the given vector clock, if greater than the timestamp in this vector clock, replace it with the
     * new one.
     *
     * @param newVectorClock The new vector clock to use to update this one.
     */
    public void updateVectorClock(@NotNull VectorClock newVectorClock) {
        newVectorClock.timestamps.forEach((key, value) -> timestamps.merge(key, value, Integer::max));
    }

    /**
     * Compare two vector clocks.
     *
     * @param otherVectorClock The vector clock to compare to.
     * @return True if all timestamps of this vector clock are greater or equals to the timestamps of the given vector
     *         clock.
     */
    public boolean isNewerThanOrEqualTo(@NotNull VectorClock otherVectorClock) {
        return otherVectorClock.timestamps
                .entrySet()
                .stream()
                .allMatch((entry) -> this.timestamps.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

}
