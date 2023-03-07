package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

@CustomLog(topic = "Service")
public class UserService extends UserServiceGrpc.UserServiceImplBase implements AutoCloseable {

    private final ManagedChannel channel;
    // blocking stub (for now)
    private final UserServiceGrpc.UserServiceBlockingStub stub;

    public UserService(String host, int port) {
        channel = ManagedChannelBuilder.forTarget(host + ":" + port)
                .usePlaintext()
                .build();
        stub = UserServiceGrpc.newBlockingStub(channel);
        log.debug("Connected to %s:%d", host, port);
    }

    public void createAccount(String username) throws StatusRuntimeException {
        log.debug("Sending request to create account for '%s'", username);
        //noinspection ResultOfMethodCallIgnored
        stub.createAccount(
                CreateAccountRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to create account for '%s'", username);
    }

    public void deleteAccount(String username) throws StatusRuntimeException {
        log.debug("Sending request to delete account for '%s'", username);
        //noinspection ResultOfMethodCallIgnored
        stub.deleteAccount(
                DeleteAccountRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to delete account for '%s'", username);
    }

    public int balance(String username) throws StatusRuntimeException {
        log.debug("Sending request to get balance for '%s'", username);
        final BalanceResponse response = stub.balance(
                BalanceRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to get balance for '%s' (value: %d)", username, response.getValue());
        return response.getValue();
    }


    public void transferTo(String from, String to, Integer amount) throws StatusRuntimeException {
        log.debug("Sending request to create transfer of %d coin(s) from '%s' to '%s'", amount, from, to);
        //noinspection ResultOfMethodCallIgnored
        stub.transferTo(
                TransferToRequest.newBuilder()
                        .setAccountFrom(from)
                        .setAccountTo(to)
                        .setAmount(amount)
                        .build()
        );
        log.debug("Received response to create transfer of %d coin(s) from '%s' to '%s'", amount, from, to);
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}
