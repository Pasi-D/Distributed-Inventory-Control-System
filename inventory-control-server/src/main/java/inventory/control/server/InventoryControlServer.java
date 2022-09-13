package inventory.control.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.zookeeper.KeeperException;

import inventory.control.synchronization.DistributedLock;

public class InventoryControlServer {
    private int serverPort;

    private DistributedLock leaderLock;

    private AtomicBoolean isLeader = new AtomicBoolean(false);

    private byte[] leaderData; // Leader's IP & port

    private Map<String, InventoryItem> inventoryItems = new HashMap();

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
    }

    public void setInventoryItem(String itemCode, InventoryItem item) {
        inventoryItems.put(itemCode, item);
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

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(new AddInventoryItemServiceImpl(this))
                .build();
        server.start();
        System.out.println("Inventory control server started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    public static void main (String[] args) throws Exception {
        DistributedLock.setZooKeeperURL("localhost:2181");

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
    public List<String[]> getOthersData() throws
            KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();
        for (byte[] data : othersData) {
            String[] dataStrings = new
                    String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;
        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");
            try {
                boolean leader = leaderLock.tryAcquireLock();
                while (!leader) {
                    byte[] leaderData =
                            leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(10000);
                    leader = leaderLock.tryAcquireLock();
                }
                System.out.println("I got the leader lock. Now acting as primary");
                isLeader.set(true);
                currentLeaderData = null;
            } catch (Exception e){
                System.out.println("Thread Exception " + e.getMessage());
            }
        }
    }
}
