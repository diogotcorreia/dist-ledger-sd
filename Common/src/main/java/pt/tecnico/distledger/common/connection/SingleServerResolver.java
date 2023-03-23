package pt.tecnico.distledger.common.connection;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractBlockingStub;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A server resolver that always resolves to the same server, independently of the given qualifier.
 * Mainly used for testing.
 *
 * @param <T> The type of the stub.
 */
@RequiredArgsConstructor
@CustomLog(topic = "SingleServerResolver")
public class SingleServerResolver<T extends AbstractBlockingStub<T>> implements ServerResolver<T> {

    private final String host;
    private final int port;
    private final Function<ManagedChannel, T> stubCreator;

    private ManagedChannel channel;
    private T stub;

    @Override
    public @NotNull T resolveStub(@NotNull String qualifier) {
        if (channel != null && !channel.isShutdown()) {
            if (stub != null) {
                return stub;
            }
            channel.shutdown();
        }

        log.debug("Creating new channel and stub for server %s:%d", this.host, this.port);
        this.channel = ManagedChannelBuilder.forAddress(this.host, this.port)
                .usePlaintext()
                .build();
        this.stub = stubCreator.apply(this.channel);
        log.debug("Created channel and stub for server %s:%d", this.host, this.port);

        return this.stub;
    }

    @Override
    public void closeAllChannels() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
        this.channel = null;
        this.stub = null;
    }
}
