package inventory.control.server;

import inventory.control.grpc.generated.ViewInventoryStorageRequest;
import inventory.control.grpc.generated.ViewInventoryStorageResponse;
import inventory.control.grpc.generated.ViewInventoryStorageServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class ViewInventoryStorageServiceImpl extends ViewInventoryStorageServiceGrpc.ViewInventoryStorageServiceImplBase {
    private InventoryControlServer server;

    public ViewInventoryStorageServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    @Override
    public void viewInventoryStorage(ViewInventoryStorageRequest request, StreamObserver<ViewInventoryStorageResponse> responseObserver) {
        System.out.println("Request received to view the entire inventory storage...");
        Map<String, Double> itemList = server.getInventoryItems();
        ViewInventoryStorageResponse response = ViewInventoryStorageResponse.newBuilder()
                .putAllItems(itemList)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
