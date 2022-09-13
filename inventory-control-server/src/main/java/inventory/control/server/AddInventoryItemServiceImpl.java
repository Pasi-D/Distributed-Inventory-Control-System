package inventory.control.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import inventory.control.grpc.generated.AddInventoryItemRequest;
import inventory.control.grpc.generated.AddInventoryItemResponse;
import inventory.control.grpc.generated.AddInventoryItemServiceGrpc;
import org.apache.zookeeper.KeeperException;

import java.util.List;

public class AddInventoryItemServiceImpl extends AddInventoryItemServiceGrpc.AddInventoryItemServiceImplBase {
    private ManagedChannel channel = null;

    AddInventoryItemServiceGrpc.AddInventoryItemServiceBlockingStub clientStub = null;

    private InventoryControlServer server;

    public AddInventoryItemServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    @Override
    public void addInventoryItem(AddInventoryItemRequest request, StreamObserver<AddInventoryItemResponse> responseObserver) {
        String itemCode = request.getItemCode();
        String itemName = request.getItemName();
        double quantity = request.getQuantity();
        boolean status = false;
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Adding inventory item to storage as Primary");
                updateInventoryItem(itemCode, itemName, quantity);
                updateSecondaryServers(itemCode, itemName, quantity);
                status = true;
            } catch (Exception e) {
                System.out.println("Error while updating the account balance" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding inventory item to storage on secondary, on primary's command");
                updateInventoryItem(itemCode, itemName, quantity);
            } else {
                AddInventoryItemResponse response = callPrimary(itemCode, itemName, quantity);
                if (response.getStatus()) {
                    status = true;
                }
            }
        }
        AddInventoryItemResponse response = AddInventoryItemResponse.newBuilder().setStatus(status).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void updateInventoryItem(String itemCode, String itemName, double quantity) {
        server.setInventoryItem(itemCode, new InventoryItem(itemName, quantity));
        System.out.println("Updated Inventory storage with \n" + itemCode + " : " + quantity);
    }

    private void updateSecondaryServers(String itemCode, String itemName, double quantity) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemCode, itemName, quantity, true, IPAddress, port);
        }
    }

    private AddInventoryItemResponse callPrimary(String itemCode, String itemName, double quantity) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemCode, itemName, quantity, false, IPAddress, port);
    }

    private AddInventoryItemResponse callServer(
            String itemCode,
            String itemName,
            double quantity,
            boolean isSentByPrimary,
            String IPAddress,
            int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = AddInventoryItemServiceGrpc.newBlockingStub(channel);
        AddInventoryItemRequest request = AddInventoryItemRequest
                .newBuilder()
                .setItemCode(itemCode)
                .setItemName(itemName)
                .setQuantity(quantity)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        AddInventoryItemResponse response = clientStub.addInventoryItem(request);
        return response;
    }
}
