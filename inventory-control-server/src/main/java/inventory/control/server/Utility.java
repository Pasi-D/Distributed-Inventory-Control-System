package inventory.control.server;

import inventory.control.grpc.generated.GetInventoryStorageItemsRequest;
import inventory.control.grpc.generated.GetInventoryStorageItemsResponse;
import inventory.control.grpc.generated.GetInventoryStorageItemsServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;

public class Utility {

    private ManagedChannel channel = null;
    private InventoryControlServer server;
    private GetInventoryStorageItemsServiceGrpc.GetInventoryStorageItemsServiceBlockingStub getInventoryStorageItemsClientStub = null;
    private Map<String, InventoryItem> inventoryItemsTempDataHold = new HashMap<>();
    public Utility(InventoryControlServer server) {
        this.server = server;
    }

    /**
     * Call Primary node and update the current inventory storage
     */
    public void updateSelfInventoryStorage() {
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        GetInventoryStorageItemsResponse primaryServerResponse = this.callPrimary(IPAddress, port);
        System.out.println("Updating inventory storage");
        primaryServerResponse.getInventoryItemsMap().forEach((itemName, item) -> {
            inventoryItemsTempDataHold.put(itemName, new InventoryItem(item.getItemName(), item.getItemQuantity()));
        });
        server.updateInventoryItemList(inventoryItemsTempDataHold);
    }

    private GetInventoryStorageItemsResponse callPrimary(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        getInventoryStorageItemsClientStub = GetInventoryStorageItemsServiceGrpc.newBlockingStub(channel);
        GetInventoryStorageItemsRequest request = GetInventoryStorageItemsRequest.newBuilder().build();
        GetInventoryStorageItemsResponse response = getInventoryStorageItemsClientStub.getInventoryStorageItems(request);
        return response;
    }
}
