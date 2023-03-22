package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.CustomLog;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

@CustomLog(topic = "Service")
public class UserService implements AutoCloseable {

    private final static String HOST = "localhost";
    private final static int PORT = 5001;
    private final static String SERVER_SERVICE_NAME = "DistLedger";
    private final ManagedChannel namingChannel;
    private ManagedChannel userChannel;
    // blocking userStub (for now)
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserService() {
        namingChannel = ManagedChannelBuilder.forAddress(HOST, PORT)
                .usePlaintext()
                .build();
        namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);
    }

    public void createAccount(String qualifier, String username) throws StatusRuntimeException {
        connectToServer(qualifier);
        log.debug("Sending request to create account for '%s'", username);
        //noinspection ResultOfMethodCallIgnored
        userStub.createAccount(
                CreateAccountRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to create account for '%s'", username);
    }

    public void deleteAccount(String qualifier, String username) throws StatusRuntimeException {
        connectToServer(qualifier);
        log.debug("Sending request to delete account for '%s'", username);
        //noinspection ResultOfMethodCallIgnored
        userStub.deleteAccount(
                DeleteAccountRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to delete account for '%s'", username);
    }

    public int balance(String qualifier, String username) throws StatusRuntimeException {
        connectToServer(qualifier);
        log.debug("Sending request to get balance for '%s'", username);
        final BalanceResponse response = userStub.balance(
                BalanceRequest.newBuilder()
                        .setUserId(username)
                        .build()
        );
        log.debug("Received response to get balance for '%s' (value: %d)", username, response.getValue());
        return response.getValue();
    }


    public void transferTo(String qualifier, String from, String to, Integer amount) throws StatusRuntimeException {
        connectToServer(qualifier);
        log.debug("Sending request to create transfer of %d coin(s) from '%s' to '%s'", amount, from, to);
        //noinspection ResultOfMethodCallIgnored
        userStub.transferTo(
                TransferToRequest.newBuilder()
                        .setAccountFrom(from)
                        .setAccountTo(to)
                        .setAmount(amount)
                        .build()
        );
        log.debug("Received response to create transfer of %d coin(s) from '%s' to '%s'", amount, from, to);
    }

    private void connectToServer(String qualifier) {
        establishConnection(
                namingServerStub.lookupServer(
                        LookupServerRequest.newBuilder()
                                .setServiceName(SERVER_SERVICE_NAME)
                                .setQualifier(qualifier)
                                .build()
                )
        );
    }

    private void establishConnection(LookupServerResponse response) {
        if (response.getServerInfoCount() == 0) {
            throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Server not found"));
        }

        final ServerInfo serverInfo = response.getServerInfo(0);
        final ServerAddress serverAddress = serverInfo.getAddress();
        final String host = serverAddress.getHost();
        final int port = serverAddress.getPort();

        log.debug("Connecting to server '%s' at %s:%d", serverInfo.getQualifier(), host, port);
        userChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        userStub = UserServiceGrpc.newBlockingStub(userChannel);
        log.debug("Connected to server '%s' at %s:%d", serverInfo.getQualifier(), host, port);
    }

    @Override
    public void close() {
        userChannel.shutdown();
    }
}
