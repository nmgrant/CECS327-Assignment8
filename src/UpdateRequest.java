
import java.io.Serializable;


public class UpdateRequest implements Serializable {
   private int workerID;
   private int workerNode;
   
   public UpdateRequest() {
      workerID = 0;
      workerNode = 0;
   }
   public UpdateRequest(int wID, int wN) {
      workerID = wID;
      workerNode = wN;
   }

   public int getWorkerID() {
      return workerID;
   }

   public int getWorkerNode() {
      return workerNode;
   }
}
