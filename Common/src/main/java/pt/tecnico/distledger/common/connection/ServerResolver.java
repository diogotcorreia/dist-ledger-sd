package pt.tecnico.distledger.common.connection;

import io.grpc.stub.AbstractBlockingStub;
import org.jetbrains.annotations.NotNull;

/**
 * Manages channels and stubs transparently.
 *
 * @param <T> The type of the stub.
 */
public interface ServerResolver<T extends AbstractBlockingStub<T>> extends AutoCloseable {

    /**
     * Get the stub for the given qualifier.
     *
     * @param qualifier The qualifier of the server to connect to.
     * @return The stub.
     */
    @NotNull
    T resolveStub(@NotNull String qualifier);

    /**
     * Close all currently opened channels.
     */
    void closeAllChannels();

    /**
     * @see ServerResolver#closeAllChannels()
     */
    default void close() {
        this.closeAllChannels();
    }

}
