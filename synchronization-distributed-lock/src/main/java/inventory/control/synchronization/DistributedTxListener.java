package inventory.control.synchronization;

public interface DistributedTxListener {
    void onGlobalCommit();
    void onGlobalAbort();
}
