
import java.io.Serializable;


public class UpdateResponse implements Serializable {
   private int workerID;
   private int workerNode;
   private boolean available;
       
   public UpdateResponse() {
      workerID = 0;
      workerNode = 0;
      available = false;
   }
   
   public UpdateResponse(int wID, int wN, boolean a) {
      workerID = wID;
      workerNode = wN;
      available = a;
   }
   
   @Override
   public String toString() {
     return ("Response: Worker: " + workerID + " Node: " + workerNode);
   }

   public int getWorkerID() {
      return workerID;
   }

   public int getWorkerNode() {
      return workerNode;
   }

   public boolean isAvailable() {
      return available;
   }
   
}
