package inventory.control.server;

import inventory.control.grpc.generated.GetInventoryStorageItemsRequest;
import inventory.control.grpc.generated.GetInventoryStorageItemsResponse;
import inventory.control.grpc.generated.GetInventoryStorageItemsServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

public class GetInventoryStorageItemsServiceImpl extends GetInventoryStorageItemsServiceGrpc.GetInventoryStorageItemsServiceImplBase {
    private InventoryControlServer server;

    public GetInventoryStorageItemsServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    private Map<String, inventory.control.grpc.generated.InventoryItem> tempInventoryItemListData = new HashMap<>();

    @Override
    public void getInventoryStorageItems(GetInventoryStorageItemsRequest request, StreamObserver<GetInventoryStorageItemsResponse> responseObserver) {
        System.out.println("Request received to get a snap of current inventory storage...");
        Map<String, InventoryItem> inventoryItems = server.getInventoryItems();
        inventoryItems.forEach((itemCode, inventoryItem) -> {
            inventory.control.grpc.generated.InventoryItem item = inventory.control.grpc.generated.InventoryItem
                    .newBuilder()
                    .setItemName(inventoryItem.getItemName())
                    .setItemQuantity(inventoryItem.getItemQuantity())
                    .build();
            tempInventoryItemListData.put(itemCode, item);
        });
        GetInventoryStorageItemsResponse response = GetInventoryStorageItemsResponse
                .newBuilder()
                .putAllInventoryItems(tempInventoryItemListData)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
