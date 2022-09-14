package inventory.control.client;

import inventory.control.grpc.generated.GetInventoryItemRequest;
import inventory.control.grpc.generated.GetInventoryItemResponse;
import inventory.control.grpc.generated.GetInventoryItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class GetInventoryItemServiceClient {
    private ManagedChannel channel = null;

    GetInventoryItemServiceGrpc.GetInventoryItemServiceBlockingStub clientStub = null;

    String host = null;

    int port = -1;

    public GetInventoryItemServiceClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void initializeConnection() {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        clientStub = GetInventoryItemServiceGrpc.newBlockingStub(channel);
    }

    public void closeConnection() {
        channel.shutdown();
    }

    public void processUserRequests() throws InterruptedException {
        while (true) {
            Scanner userInput = new Scanner(System.in);
            System.out.println("\nEnter item code to view the details :");
            String itemCode = userInput.nextLine().trim();
            System.out.println("Requesting server to check the details for item code " + itemCode);
            GetInventoryItemRequest request = GetInventoryItemRequest.newBuilder()
                    .setItemCode(itemCode)
                    .build();
            GetInventoryItemResponse response = clientStub.getInventoryItem(request);
            if (response.getItemName().isEmpty() && response.getQuantity() == 0) {
                System.out.println("No matching item found");
            } else {
                System.out.printf("" +
                        "Inventory storage has " + response.getItemName()
                        + " (item code - " + itemCode + ")" + " with a quantity of "
                        + response.getQuantity() + " items");
            }
            Thread.sleep(1000);
        }
    }
}
