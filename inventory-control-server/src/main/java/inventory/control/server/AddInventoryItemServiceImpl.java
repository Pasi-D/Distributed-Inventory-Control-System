package inventory.control.server;

import inventory.control.synchronization.DistributedTxCoordinator;
import inventory.control.synchronization.DistributedTxListener;
import inventory.control.synchronization.DistributedTxParticipant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import inventory.control.grpc.generated.AddInventoryItemRequest;
import inventory.control.grpc.generated.AddInventoryItemResponse;
import inventory.control.grpc.generated.AddInventoryItemServiceGrpc;
import javafx.util.Pair;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class AddInventoryItemServiceImpl extends AddInventoryItemServiceGrpc.AddInventoryItemServiceImplBase implements DistributedTxListener {
    private ManagedChannel channel = null;

    AddInventoryItemServiceGrpc.AddInventoryItemServiceBlockingStub clientStub = null;

    private InventoryControlServer server;

    private Pair<String, InventoryItem> tempDataHolder;

    private boolean transactionStatus = false;

    public AddInventoryItemServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    @Override
    public void addInventoryItem(AddInventoryItemRequest request, StreamObserver<AddInventoryItemResponse> responseObserver) {
        String itemCode = request.getItemCode();
        String itemName = request.getItemName();
        double quantity = request.getQuantity();
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Adding inventory item to storage as Primary");
                startDistributedTx(itemCode, new InventoryItem(itemName, quantity));
                updateSecondaryServers(itemCode, itemName, quantity);
                System.out.println("going to perform item addition");
                if (!itemName.isEmpty() && quantity > 0) {
                    ((DistributedTxCoordinator)server.getStoreTransaction()).perform();
                    transactionStatus = true;
                } else {
                    ((DistributedTxCoordinator)server.getStoreTransaction()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while updating the inventory list" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As Secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding inventory item to storage on secondary, on primary's command");
                startDistributedTx(itemCode, new InventoryItem(itemName, quantity));
                updateInventory();
                if (!itemName.isEmpty() && quantity > 0) {
                    ((DistributedTxParticipant)server.getStoreTransaction()).voteCommit();
                    transactionStatus = true;
                } else {
                    ((DistributedTxParticipant)server.getStoreTransaction()).voteAbort();
                }
            } else {
                AddInventoryItemResponse response = callPrimary(itemCode, itemName, quantity);
                if (response.getStatus()) {
                    transactionStatus = true;
                }
            }
        }
        AddInventoryItemResponse response = AddInventoryItemResponse.newBuilder().setStatus(transactionStatus).build();
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

    private void updateInventory() {
        if (tempDataHolder != null) {
            String itemCode = tempDataHolder.getKey();
            InventoryItem item = tempDataHolder.getValue();
            server.setInventoryItem(itemCode, item);
            System.out.println("Inventory Item with code " + itemCode +
                    " updated with :- " + item.getItemName() + " (item-name) & quantity = "
                    + item.getItemQuantity());
            tempDataHolder = null;
        }
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

    private void startDistributedTx(String itemCode, InventoryItem item) {
        try {
            server.getStoreTransaction().start(itemCode, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new Pair(itemCode, item);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGlobalCommit() {
        updateInventory();
    }
    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
