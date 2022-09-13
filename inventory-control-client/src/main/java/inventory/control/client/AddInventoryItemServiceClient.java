package inventory.control.client;

import inventory.control.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class AddInventoryItemServiceClient {
    private ManagedChannel channel = null;
    AddInventoryItemServiceGrpc.AddInventoryItemServiceBlockingStub clientStub = null;
    String host = null;
    int port = -1;

    public AddInventoryItemServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection () {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        clientStub = AddInventoryItemServiceGrpc.newBlockingStub(channel);
    }

    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests() throws InterruptedException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter Item code, Item Name, Item Quantity :");
            String input[] = userInput.nextLine().trim().split(",");
            String itemCode = input[0];
            String itemName = input[1];
            double quantity = Double.parseDouble(input[2]);
            System.out.println("Requesting server to add " + quantity + " of "  + itemName + " with item code " + itemCode);
            AddInventoryItemRequest request = AddInventoryItemRequest
                    .newBuilder()
                    .setItemCode(itemCode)
                    .setItemName(itemName)
                    .setQuantity(quantity)
                    .setIsSentByPrimary(false)
                    .build();
            AddInventoryItemResponse response = this.clientStub.addInventoryItem(request);
            System.out.printf("Inventory Transaction Status " + (response.getStatus() ? "Successful" : "Failed"));
            Thread.sleep(1000);
        }
    }
}
