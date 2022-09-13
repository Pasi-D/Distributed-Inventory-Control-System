package inventory.control.server;

public class InventoryItem {
    private String itemName;
    private double itemQuantity;

    public InventoryItem(String name, double quantity) {
        itemName = name;
        itemQuantity = quantity;
    }

    String getItemName() {
        return this.itemName;
    }

    double getItemQuantity() {
        return this.itemQuantity;
    }

    void setItemName(String name) {
        itemName = name;
    }

    void setItemQuantity(double quantity) {
        itemQuantity = quantity;
    }
}
