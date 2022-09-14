package inventory.control.server;

import inventory.control.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

public class ViewInventoryStorageServiceImpl extends ViewInventoryStorageServiceGrpc.ViewInventoryStorageServiceImplBase {

    private ManagedChannel channel = null;

    GetInventoryStorageItemsServiceGrpc.GetInventoryStorageItemsServiceBlockingStub getInventoryStorageItemsClientStub = null;
    private InventoryControlServer server;

    private Map<String, InventoryItem> inventoryItemsTempDataHold = new HashMap<>();

    public ViewInventoryStorageServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    @Override
    public void viewInventoryStorage(ViewInventoryStorageRequest request, StreamObserver<ViewInventoryStorageResponse> responseObserver) {
        System.out.println("Request received to view the entire inventory storage...");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        GetInventoryStorageItemsResponse primaryServerResponse = this.callPrimary(IPAddress, port);
        updateSelfInventoryStorage(primaryServerResponse);

        Map<String, Double> itemList = server.getInventoryItemsList();
        ViewInventoryStorageResponse response = ViewInventoryStorageResponse.newBuilder()
                .putAllItems(itemList)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private GetInventoryStorageItemsResponse callPrimary(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        getInventoryStorageItemsClientStub = GetInventoryStorageItemsServiceGrpc.newBlockingStub(channel);
        GetInventoryStorageItemsRequest request = GetInventoryStorageItemsRequest.newBuilder().build();
        GetInventoryStorageItemsResponse response = getInventoryStorageItemsClientStub.getInventoryStorageItems(request);
        return response;
    }

    private void updateSelfInventoryStorage(GetInventoryStorageItemsResponse primaryServerResponse) {
        System.out.println("Updating self inventory storage");
        primaryServerResponse.getInventoryItemsMap().forEach((itemName, item) -> {
            inventoryItemsTempDataHold.put(itemName, new InventoryItem(item.getItemName(), item.getItemQuantity()));
        });
        server.updateInventoryItemList(inventoryItemsTempDataHold);
    }
}
