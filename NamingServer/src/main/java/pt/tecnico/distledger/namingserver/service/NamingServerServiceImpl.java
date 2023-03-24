package pt.tecnico.distledger.namingserver.service;

import io.grpc.stub.StreamObserver;
import lombok.CustomLog;
import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.tecnico.distledger.namingserver.domain.ServerAddress;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.tecnico.distledger.namingserver.exceptions.AddressWithInvalidParametersException;
import pt.tecnico.distledger.namingserver.exceptions.ServerDoesNotExistException;
import pt.tecnico.distledger.namingserver.exceptions.ServerEntryAlreadyExistsException;
import pt.tecnico.distledger.namingserver.exceptions.ServerWithInvalidParametersException;
import pt.tecnico.distledger.namingserver.factory.AddressFactory;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.DeleteServerResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.LookupServerResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterServerRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.RegisterServerResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.ServerInfo;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceImplBase;

import java.util.List;

@CustomLog(topic = "Naming Server Service")
public class NamingServerServiceImpl extends NamingServerServiceImplBase {

    private final NamingServer namingServer;

    private final AddressFactory createAddressFactory = new AddressFactory();

    public NamingServerServiceImpl(NamingServer namingServer) {
        this.namingServer = namingServer;
    }

    @Override
    public void registerServer(RegisterServerRequest request, StreamObserver<RegisterServerResponse> responseObserver) {
        try {
            var address = createAddressFactory.createAddressFromGrpc(request.getAddress());
            log.debug("Registering server %s on service %s", address, request.getServiceName());
            namingServer.register(
                    request.getServiceName(),
                    address,
                    request.getQualifier()
            );
            log.debug("Server %s registered on service %s", address, request.getServiceName());
            responseObserver.onNext(RegisterServerResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ServerEntryAlreadyExistsException | AddressWithInvalidParametersException |
                 ServerWithInvalidParametersException e) {
            log.debug("Error registering server: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    @Override
    public void lookupServer(LookupServerRequest request, StreamObserver<LookupServerResponse> responseObserver) {
        log.debug(
                "Looking up servers for service %s with qualifier %s",
                request.getServiceName(),
                request.getQualifier()
        );
        final List<ServerInfo> servers = namingServer
                .lookup(request.getServiceName(), request.getQualifier())
                .stream()
                .map(this::convertEntryToGrpc)
                .toList();
        final LookupServerResponse response = LookupServerResponse.newBuilder().addAllServerInfo(servers).build();
        log.debug(
                "Replying with server list for service %s with qualifier %s",
                request.getServiceName(),
                request.getQualifier()
        );
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteServer(DeleteServerRequest request, StreamObserver<DeleteServerResponse> responseObserver) {
        try {
            var address = createAddressFactory.createAddressFromGrpc(request.getAddress());
            log.debug("Deleting server %s for service %s", address, request.getServiceName());
            namingServer.delete(request.getServiceName(), address);
            log.debug("Server %s deleted from service", address, request.getServiceName());
            responseObserver.onNext(DeleteServerResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ServerDoesNotExistException | AddressWithInvalidParametersException e) {
            log.debug("Error deleting server: %s", e.getMessage());
            responseObserver.onError(e.toGrpcRuntimeException());
        }
    }

    private ServerInfo convertEntryToGrpc(ServerEntry serverEntry) {
        return ServerInfo
                .newBuilder()
                .setAddress(
                        convertAddressToGrpc(serverEntry.address())
                )
                .setQualifier(serverEntry.qualifier())
                .build();
    }

    private NamingServerDistLedger.ServerAddress convertAddressToGrpc(ServerAddress address) {
        return NamingServerDistLedger.ServerAddress.newBuilder()
                .setHost(address.host())
                .setPort(address.port())
                .build();
    }

}
