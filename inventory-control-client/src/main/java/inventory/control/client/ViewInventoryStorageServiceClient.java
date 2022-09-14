package inventory.control.client;

import inventory.control.grpc.generated.ViewInventoryStorageRequest;
import inventory.control.grpc.generated.ViewInventoryStorageResponse;
import inventory.control.grpc.generated.ViewInventoryStorageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;

public class ViewInventoryStorageServiceClient {
    private ManagedChannel channel = null;

    ViewInventoryStorageServiceGrpc.ViewInventoryStorageServiceBlockingStub clientStub = null;

    String host = null;

    int port = -1;

    public ViewInventoryStorageServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection() {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        clientStub = ViewInventoryStorageServiceGrpc.newBlockingStub(channel);
    }

    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests() throws InterruptedException {
        System.out.println("Requesting server to return all inventory storage items ");
        ViewInventoryStorageRequest request = ViewInventoryStorageRequest.newBuilder()
                .build();

        ViewInventoryStorageResponse response = clientStub.viewInventoryStorage(request);
        Map<String, Double> itemList = response.getItemsMap();
        if (itemList.size() > 0) {
            System.out.println("|   Item Name   |   Item Quantity   |");
            itemList.forEach((itemName, quantity) -> {
                System.out.println("-------------------------------------");
                System.out.println("| " + itemName + "  |  " + quantity.toString() + " |");
            });
        } else {
            System.out.println("No items available in inventory storage");
        }
    }
}
