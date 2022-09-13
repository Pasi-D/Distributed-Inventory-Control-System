package inventory.control.client;

public class MainClass {
    public static void main(String[] args) throws InterruptedException {
        String host = args[0];
        int port = Integer.parseInt(args[1].trim());
        String operation = args[2];

        if (args.length != 3) {
            System.out.println("Usage of inventory control client <host> <port> <operation ('add' | 'view')>");
            System.out.println("========= Allowed Operations =========");
            System.out.println("- 'add' : Add Inventory Item");
            System.out.println("- 'view' : View Inventory Item List");
            System.exit(1);
        }

        if ("add".equals(operation)) {
            AddInventoryItemServiceClient client = new AddInventoryItemServiceClient(host, port);
            client.initializeConnection();
            client.processUserRequests();
            client.closeConnection();
        } else {
            System.out.println("Sorry no such command exist. Please try again.");
            System.exit(1);
        }
    }
}
