package inventory.control.client;

public class MainClass {
    private static final String addOperation = "add";
    private static final String viewOperation = "view";
    private static final String viewAllOperation = "all";
    private static final String reserveOperation = "take";
    public static void main(String[] args) throws InterruptedException {
        String host = args[0];
        int port = Integer.parseInt(args[1].trim());
        String operation = args[2];

        if (args.length != 3) {
            System.out.printf("Usage of inventory control client <host> <port> <%s | %s | %s>\n",
                    addOperation, viewOperation, viewAllOperation);
            System.out.println("========= Allowed Operations =========");
            System.out.printf("- %s : Add Inventory Item \n", addOperation);
            System.out.printf("- %s : View Inventory Item List \n", viewOperation);
            System.exit(1);
        }

        if (addOperation.equals(operation)) {
            AddInventoryItemServiceClient client = new AddInventoryItemServiceClient(host, port);
            client.initializeConnection();
            client.processUserRequests();
            client.closeConnection();
        } else if (viewOperation.equals(operation)) {
            GetInventoryItemServiceClient client = new GetInventoryItemServiceClient(host, port);
            client.initializeConnection();
            client.processUserRequests();
            client.closeConnection();
        } else if (viewAllOperation.equals(operation)) {
            ViewInventoryStorageServiceClient client = new ViewInventoryStorageServiceClient(host, port);
            client.initializeConnection();
            client.processUserRequests();
            client.closeConnection();
        } else if (reserveOperation.equals(operation)) {
            ReserveInventoryItemClient client = new ReserveInventoryItemClient(host, port);
            client.initializeConnection();
            client.processUserRequests();
            client.closeConnection();
        } else {
            System.out.println("Sorry no such command exist. Please try again.");
            System.exit(1);
        }
    }
}
