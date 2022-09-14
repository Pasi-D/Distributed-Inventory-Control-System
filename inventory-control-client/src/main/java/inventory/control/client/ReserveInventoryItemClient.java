package inventory.control.client;

import inventory.control.grpc.generated.ReserveInventoryItemRequest;
import inventory.control.grpc.generated.ReserveInventoryItemResponse;
import inventory.control.grpc.generated.ReserveInventoryItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class ReserveInventoryItemClient {
    private ManagedChannel channel = null;

    ReserveInventoryItemServiceGrpc.ReserveInventoryItemServiceBlockingStub clientStub = null;

    String host = null;
    int port = -1;

    public ReserveInventoryItemClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        clientStub = ReserveInventoryItemServiceGrpc.newBlockingStub(channel);
    }

    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests() throws InterruptedException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter Item code, Item Quantity :");
            String input[] = userInput.nextLine().trim().split(",");
            String itemCode = input[0];
            double quantity = Double.parseDouble(input[1]);
            System.out.println("Requesting server to reserve " + quantity + " of inventory item with item code " + itemCode);
            ReserveInventoryItemRequest request = ReserveInventoryItemRequest.newBuilder()
                    .setItemCode(itemCode)
                    .setQuantity(quantity)
                    .setIsSentByPrimary(false)
                    .build();
            ReserveInventoryItemResponse response = this.clientStub.reserveInventoryItem(request);
            System.out.printf("Inventory Item Reserving Status " + (response.getStatus() ? "Successful" : "Failed"));
            System.out.println("Reason is " + response.getMessage());
            Thread.sleep(1000);
        }
    }
}
