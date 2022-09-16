package inventory.control.server;

import inventory.control.grpc.generated.ReserveInventoryItemRequest;
import inventory.control.grpc.generated.ReserveInventoryItemResponse;
import inventory.control.grpc.generated.ReserveInventoryItemServiceGrpc;
import inventory.control.synchronization.DistributedTxCoordinator;
import inventory.control.synchronization.DistributedTxListener;
import inventory.control.synchronization.DistributedTxParticipant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ReserveInventoryItemServiceImpl
        extends ReserveInventoryItemServiceGrpc.ReserveInventoryItemServiceImplBase implements DistributedTxListener {
    private ManagedChannel channel = null;
    private Utility utils;
    ReserveInventoryItemServiceGrpc.ReserveInventoryItemServiceBlockingStub clientStub = null;
    private InventoryControlServer server;
    private boolean transactionStatus = false;
    private String transactionMessage = "Failed to reserve";
    private Pair<String, InventoryItem> reservingDataHolder;
    public ReserveInventoryItemServiceImpl(InventoryControlServer server) {
        this.server = server;
        this.utils = new Utility(server);
    }

    @Override
    public void reserveInventoryItem(ReserveInventoryItemRequest request, StreamObserver<ReserveInventoryItemResponse> responseObserver) {
        String itemCode = request.getItemCode();
        double requestQuantity = request.getQuantity();
        InventoryItem itemOnReserved = server.getReservingItem(itemCode, requestQuantity);
        if (server.isLeader()) {
            // Act as primary
            try {
                System.out.println("Checking item existence in primary");
                if (!server.checkInventoryItemExistence(itemCode)) {
                    transactionMessage = "Provided item code does not exist";
                } else {
                   startDistributedTx(itemCode, itemOnReserved);
                   updateSecondaryServers(itemCode, requestQuantity);
                   System.out.println("going to perform reserving");
                   if (itemOnReserved.getItemName().isEmpty() && itemOnReserved.getItemQuantity() == 0) {
                       ((DistributedTxCoordinator)server.getReserveTransaction()).sendGlobalAbort();
                       transactionMessage = "Provided quantity is too large than available";
                   } else {
                       ((DistributedTxCoordinator)server.getReserveTransaction()).perform();
                       transactionStatus = true;
                       transactionMessage = "Successfully reserved item";
                   }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Act as secondary
            if (request.getIsSentByPrimary()) {
                System.out.println("Reserving inventory item from storage on secondary, on primary's command");
                startDistributedTx(itemCode, itemOnReserved);
                updateInventory();
                if (itemOnReserved.getItemName().isEmpty() && itemOnReserved.getItemQuantity() == 0) {
                    ((DistributedTxParticipant)server.getReserveTransaction()).voteAbort();
                    transactionMessage = "Provided quantity is too large than available";
                } else {
                    ((DistributedTxParticipant)server.getReserveTransaction()).voteCommit();
                    transactionStatus = true;
                    transactionMessage = "Successfully reserved item";
                }
            } else {
                ReserveInventoryItemResponse response = callPrimary(itemCode, requestQuantity);
                if (response.getStatus()) {
                    transactionStatus = true;
                    transactionMessage = "Successfully reserved item";
                }
            }
            utils.updateSelfInventoryStorage();
        }

        ReserveInventoryItemResponse response = ReserveInventoryItemResponse.newBuilder()
                .setStatus(transactionStatus)
                .setMessage(transactionMessage)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void updateSecondaryServers(String itemCode, double quantity) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = server.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(itemCode, quantity, true, IPAddress, port);
        }
    }

    private void startDistributedTx(String itemCode, InventoryItem itemReserved) {
        try {
            server.getReserveTransaction().start(itemCode, String.valueOf(UUID.randomUUID()));
            reservingDataHolder = new Pair<>(itemCode, itemReserved);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ReserveInventoryItemResponse callServer(
            String itemCode,
            double quantity,
            boolean isSentByPrimary,
            String IPAddress,
            int port
    ) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ReserveInventoryItemServiceGrpc.newBlockingStub(channel);
        ReserveInventoryItemRequest request = ReserveInventoryItemRequest.newBuilder()
                .setItemCode(itemCode)
                .setQuantity(quantity)
                .setIsSentByPrimary(isSentByPrimary)
                .build();
        ReserveInventoryItemResponse response = clientStub.reserveInventoryItem(request);
        return response;
    }

    private ReserveInventoryItemResponse callPrimary(String itemCode, Double quantity) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = server.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemCode, quantity, false, IPAddress, port);
    }

    private void updateInventory() {
        if (reservingDataHolder != null) {
            String itemCode = reservingDataHolder.getKey();
            InventoryItem item = reservingDataHolder.getValue();
            server.setInventoryItem(itemCode, item);
            System.out.println("Inventory Item with code " + itemCode +
                    " updated with :- " + item.getItemName() + " (item-name) & quantity = "
                    + item.getItemQuantity());
            reservingDataHolder = null;
        }
    }

    @Override
    public void onGlobalCommit() {
        updateInventory();
    }

    @Override
    public void onGlobalAbort() {
        reservingDataHolder = null;
        System.out.println("Transaction Aborted by the Coordinator");
    }
}
