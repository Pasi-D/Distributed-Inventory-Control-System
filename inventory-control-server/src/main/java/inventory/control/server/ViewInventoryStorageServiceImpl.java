package inventory.control.server;

import inventory.control.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

public class ViewInventoryStorageServiceImpl extends ViewInventoryStorageServiceGrpc.ViewInventoryStorageServiceImplBase {

    private ManagedChannel channel = null;
    private InventoryControlServer server;
    private Utility utils;

    public ViewInventoryStorageServiceImpl(InventoryControlServer server) {
        this.server = server;
        this.utils = new Utility(server);
    }

    @Override
    public void viewInventoryStorage(ViewInventoryStorageRequest request, StreamObserver<ViewInventoryStorageResponse> responseObserver) {
        System.out.println("Request received to view the entire inventory storage...");
        // Updating the data replica of this node if its not leader
        if (!server.isLeader()) {
            utils.updateSelfInventoryStorage();
        }

        Map<String, Double> itemList = server.getInventoryItemsList();
        ViewInventoryStorageResponse response = ViewInventoryStorageResponse.newBuilder()
                .putAllItems(itemList)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
