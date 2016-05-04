
import java.io.Serializable;

// This class represents a "response" being sent from the server to the client.
// It is a response to a worker's request to update a specific node.
// The body of the response includes the worker that made the request,
// the node they wish to update, and a boolean variable indicating whether
// or not they can update that node.
public class UpdateResponse implements Serializable {
   // The workerID of the requesting worker
   private int workerID;
   // The node the requesting worker wishes to work on
   private int workerNode;
   // Whether or not the worker can work on the node
   private boolean available;
   
   // Default constructor for serializing purposes
   public UpdateResponse() {
      workerID = 0;
      workerNode = 0;
      available = false;
   }
   
   // Constructor to initialize an UpdateResponse
   public UpdateResponse(int wID, int wN, boolean a) {
      workerID = wID;
      workerNode = wN;
      available = a;
   }
   
   // String representation for debugging purposes
   @Override
   public String toString() {
     return ("Response: Worker: " + workerID + " Node: " + workerNode);
   }

   
   // Get the workerID
   public int getWorkerID() {
      return workerID;
   }

   // Get the worker node
   public int getWorkerNode() {
      return workerNode;
   }
   
   // Get the node availability
   public boolean isAvailable() {
      return available;
   }
   
}
