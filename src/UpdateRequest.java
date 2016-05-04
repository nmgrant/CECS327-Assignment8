
import java.io.Serializable;

// This class represents a "request" being sent from the client (the worker)
// to the server requesting a node to shuffle. The server receives this request
// and checks the availability of the requested node, then sends a response
// accordingly.
public class UpdateRequest implements Serializable {
   // The workerID of the worker requesting a node
   private int workerID;
   // The node being requested by the worker
   private int workerNode;
   
   // Default constructor for serializing
   public UpdateRequest() {
      workerID = 0;
      workerNode = 0;
   }
   
   // Normal constructor to initialize the request
   public UpdateRequest(int wID, int wN) {
      workerID = wID;
      workerNode = wN;
   }

   // Get the workerID of the request
   public int getWorkerID() {
      return workerID;
   }

   // Get the requested node of the request
   public int getWorkerNode() {
      return workerNode;
   }
}
