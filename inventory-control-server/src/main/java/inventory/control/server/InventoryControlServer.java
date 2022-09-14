package inventory.control.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import inventory.control.synchronization.DistributedTx;
import inventory.control.synchronization.DistributedTxCoordinator;
import inventory.control.synchronization.DistributedTxParticipant;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import inventory.control.synchronization.DistributedLock;

public class InventoryControlServer {
    private int serverPort;

    private DistributedLock leaderLock;

    private AtomicBoolean isLeader = new AtomicBoolean(false);

    private byte[] leaderData; // Leader's IP & port

    private final Map<String, InventoryItem> inventoryItems = new HashMap();

    private DistributedTx storeTransaction;
    private DistributedTx reserveTransaction;
    private AddInventoryItemServiceImpl addInventoryItemService;
    private GetInventoryItemServiceImpl getInventoryItemService;
    private ViewInventoryStorageServiceImpl viewInventoryStorageService;
    private ReserveInventoryItemServiceImpl reserveInventoryItemService;


    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    public InventoryControlServer(String host, int port) throws IOException, InterruptedException, KeeperException {
        this.serverPort = port;
        leaderLock = new DistributedLock("ICServerCluster", buildServerData(host, port));

        addInventoryItemService = new AddInventoryItemServiceImpl(this);
        getInventoryItemService = new GetInventoryItemServiceImpl(this);
        viewInventoryStorageService = new ViewInventoryStorageServiceImpl(this);
        reserveInventoryItemService = new ReserveInventoryItemServiceImpl(this);

        storeTransaction = new DistributedTxParticipant(addInventoryItemService);
        reserveTransaction = new DistributedTxParticipant(reserveInventoryItemService);
    }

    public DistributedTx getStoreTransaction() {
        return storeTransaction;
    }

    public DistributedTx getReserveTransaction() {
        return reserveTransaction;
    }


    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new
                LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    private void beTheLeader() {
        System.out.println("I got the leader lock. Now acting as primary");
                isLeader.set(true);
        storeTransaction = new DistributedTxCoordinator(addInventoryItemService);
        reserveTransaction = new DistributedTxCoordinator(reserveInventoryItemService);
    }

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(addInventoryItemService)
                .addService(getInventoryItemService)
                .addService(viewInventoryStorageService)
                .addService(reserveInventoryItemService)
                .build();
        server.start();
        System.out.println("Inventory control server started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    public static void main (String[] args) throws Exception {
        DistributedLock.setZooKeeperURL("localhost:2181");
        DistributedTx.setZooKeeperURL("localhost:2181");

        if (args.length != 1) {
            System.out.println("Usage executable-name <port>");
        }

        int serverPort = Integer.parseInt(args[0]);

        InventoryControlServer server = new InventoryControlServer("localhost", serverPort);
        server.startServer();
    }

    /**
     * Get the list of data of other servers connected
     * @return List<String[IP, Port]>
     * @throws KeeperException
     * @throws InterruptedException
     */
    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();
        for (byte[] data : othersData) {
            String[] dataStrings = new
                    String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    public void setInventoryItem(String itemCode, InventoryItem item) {
        inventoryItems.put(itemCode, item);
    }
    public InventoryItem getInventoryItemByCode(String itemCode) {
        InventoryItem item = inventoryItems.get(itemCode);
        return item;
    }

    public Map<String, Double> getInventoryItems() {
        Map<String, Double> itemList = new HashMap<>();
        inventoryItems.forEach((s, inventoryItem) -> {
            String currItemName = inventoryItem.getItemName();
            double currItemQuantity = inventoryItem.getItemQuantity();
            if (itemList.containsKey(currItemName)) {
                itemList.put(currItemName, itemList.get(currItemName) + currItemQuantity);
            } else {
                itemList.put(currItemName, currItemQuantity);
            }
        });
        return itemList;
    }

    public boolean checkInventoryItemExistence(String itemCode) {
        return inventoryItems.containsKey(itemCode);
    }

    /**
     * Returns item if a reservation is possible. If quantity is larger than available, masks all the details about itemname & quantity.
     * @param itemCode - Item code
     * @param quantity - Quantity to be reserved.
     * @return InventoryItem
     */
    public InventoryItem getReservingItem(String itemCode, Double quantity) {
        InventoryItem reservingItem = this.getInventoryItemByCode(itemCode);
        double currQuantity = reservingItem.getItemQuantity();
        if (quantity > currQuantity) {
            reservingItem.setItemName("");
            reservingItem.setItemQuantity(0);
        } else {
            reservingItem.setItemQuantity(currQuantity - quantity);
        }
        return reservingItem;
    }

    public void printInventoryItems() {
        System.out.println("Length of inventory items = " + inventoryItems.size());
        inventoryItems.forEach((s, inventoryItem) -> {
            if (inventoryItem != null) {
                System.out.println("s = " + s + " ==> " + inventoryItem.getItemName() + " : " + inventoryItem.getItemQuantity());
            }
        });
    }

    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;
        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");
            try {
                boolean leader = leaderLock.tryAcquireLock();
                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                currentLeaderData = null;
                beTheLeader();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
