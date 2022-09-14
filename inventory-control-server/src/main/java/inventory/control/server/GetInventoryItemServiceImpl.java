package inventory.control.server;

import inventory.control.grpc.generated.GetInventoryItemRequest;
import inventory.control.grpc.generated.GetInventoryItemResponse;
import inventory.control.grpc.generated.GetInventoryItemServiceGrpc;
import io.grpc.stub.StreamObserver;

public class GetInventoryItemServiceImpl extends GetInventoryItemServiceGrpc.GetInventoryItemServiceImplBase {
    private InventoryControlServer server;

    public GetInventoryItemServiceImpl(InventoryControlServer server) {
        this.server = server;
    }

    @Override
    public void getInventoryItem(GetInventoryItemRequest request, StreamObserver<GetInventoryItemResponse> responseObserver) {
        String itemCode = request.getItemCode();
        System.out.printf("Request received to lookup item code %s...\n", itemCode);
        InventoryItem item = server.getInventoryItemByCode(itemCode);
        GetInventoryItemResponse response;
        if (item == null) {
            System.out.println("No item found in Inventory");
            response = GetInventoryItemResponse.newBuilder()
                    .setItemName("")
                    .setQuantity(0)
                    .build();

        } else {
            String itemName = item.getItemName();
            double itemQuantity = item.getItemQuantity();
            response = GetInventoryItemResponse.newBuilder()
                    .setItemName(itemName)
                    .setQuantity(itemQuantity)
                    .build();
            System.out.println("Responding.... Storage has " + itemQuantity + " of " + itemName);
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
