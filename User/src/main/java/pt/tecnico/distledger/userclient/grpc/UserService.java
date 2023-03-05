package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserService extends UserServiceGrpc.UserServiceImplBase implements AutoCloseable {

    private final ManagedChannel channel;
    // blocking stub (for now)
    private final UserServiceGrpc.UserServiceBlockingStub stub;

    public UserService(String host, int port) {
        channel = ManagedChannelBuilder.forTarget(host + ":" + port)
                .usePlaintext()
                .build();
        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public void createAccount(String username) throws StatusRuntimeException {
        //noinspection ResultOfMethodCallIgnored
        stub.createAccount(CreateAccountRequest.newBuilder()
                .setUserId(username)
                .build());
    }

    public void deleteAccount(String username) throws StatusRuntimeException {
        //noinspection ResultOfMethodCallIgnored
        stub.deleteAccount(DeleteAccountRequest.newBuilder()
                .setUserId(username)
                .build()
        );
    }

    public int balance(String username) throws StatusRuntimeException {
        final BalanceResponse response = stub.balance(BalanceRequest.newBuilder()
                .setUserId(username)
                .build()
        );
        return response.getValue();
    }


    public void transferTo(String username, String to, Integer amount) throws StatusRuntimeException {
        //noinspection ResultOfMethodCallIgnored
        stub.transferTo(TransferToRequest.newBuilder()
                .setAccountFrom(username)
                .setAccountTo(to)
                .setAmount(amount)
                .build()
        );
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}