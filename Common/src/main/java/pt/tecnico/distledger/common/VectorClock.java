package pt.tecnico.distledger.common;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Class abstracting the logic of a vector clock.
 * <p>
 * The internal implementation uses a Map to avoid having a canonical order of the servers.
 */
@ToString
@RequiredArgsConstructor
public class VectorClock {

    private final @NotNull Map<String, Integer> timestamps = new ConcurrentHashMap<>();

    /**
     * Create a new VectorClock based on a timestamp map. The map can be used afterward without affecting the internal
     * state of this vector clock.
     *
     * @param timestamps The timestamp map to clone from.
     */
    public VectorClock(Map<String, Integer> timestamps) {
        this.timestamps.putAll(timestamps);
    }

    /**
     * Get the timestamps of this vector clock. The returned map is immutable.
     *
     * @return An unmodifiable map containing the timestamps.
     */
    public @NotNull Map<String, Integer> getTimestamps() {
        return Collections.unmodifiableMap(this.timestamps);
    }

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
     * Set the value of the clock for a certain server. If the server does not exist in the vector clock, it is added.
     *
     * @param serverId The server to set the timestamp for.
     * @param value    The new value of the timestamp.
     */
    public void setValue(@NotNull String serverId, int value) {
        timestamps.put(serverId, value);
    }

    /**
     * Increment the server's timestamp by 1. If the server does not exist in the vector clock, its timestamp is set to
     * 1.
     *
     * @param serverId The server to increment the timestamp of.
     */
    public void incrementClock(@NotNull String serverId) {
        timestamps.merge(serverId, 1, Integer::sum);
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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull VectorClock clone() {
        return new VectorClock(this.timestamps);
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
                .allMatch(entry -> this.timestamps.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VectorClock that = (VectorClock) o;

        return Stream.concat(this.timestamps.keySet().stream(), that.timestamps.keySet().stream())
                .distinct()
                .allMatch(
                        key -> Objects.equals(
                                this.timestamps.getOrDefault(key, 0),
                                that.timestamps.getOrDefault(key, 0)
                        )
                );
    }

    @Override
    public int hashCode() {
        // FIXME hashCode might not be the same for {A=1,B=0} and {A=1}
        // Idea: avoid having 0 values here at all
        return timestamps.hashCode();
    }
}
