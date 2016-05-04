
import java.io.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// This class represents a worker that is tasked with updating nodes
// periodically. The client connecting to the server initializes these
// workers and starts them. The worker then performs 200 updates, sending
// requests for the node to update with each update. A lock condition
// is used to  make the worker wait for a response from the server. Once
// the worker is allowed to operate on the node, it performs the shuffle
// then sends the node back to the server.
public class Worker extends Thread {
   
   private final int NUMBER_OF_TURNS = 10;
   // A concurrent array of nodes given by the client thread
   private AtomicReferenceArray<Node> nodeArray;
   // The output stream used to send requests
   private ObjectOutputStream output;
   // This worker's ID
   int workerNumber;
   // The lock used to make the worker wait for a response
   ReentrantLock lock;
   // The lock used to ensure only one thread is writing to the output stream
   ReentrantLock oosLock;
   // The lock condition used to signal that a response has came
   Condition updateNode;
   // Set by the client to tell whether the worker can work on the node
   boolean canUpdate;

   // Used to initialize the worker with all necessary variables
   public Worker(AtomicReferenceArray<Node> nodeArray, ObjectOutputStream oos,
           ObjectInputStream ois, ReentrantLock l, Condition c, 
           ReentrantLock oosLock, int wn) {
      this.nodeArray = nodeArray;
      this.output = oos;
      workerNumber = wn;
      lock = l;
      updateNode = c;
      this.oosLock = oosLock;
   }

   // The worker threads main run method
   public void run() {
      // Performs 200 updates
      for (int i = 0; i < NUMBER_OF_TURNS; i++) {
         // Chooses a random node to update
         int nodeIndex = new Random().nextInt(nodeArray.length());
         try {
            try {
               do {
                  // Lock on the worker's specific lock to be able to
                  // await a response from the server
                  lock.lock();
                  try {
                     // Lock on the output stream to ensure that
                     // only one thread is writing to it
                     oosLock.lock();
                     try {
                        // Send a request to the server containing this
                        // worker's ID and the node they wish to update
                        output.writeObject(
                                new UpdateRequest(workerNumber, nodeIndex));
                        output.flush();
                     } finally {
                        oosLock.unlock();
                     }
                     // Wait until the client receives a response and wakes
                     // up this worker
                     updateNode.await();
                  } finally {
                     lock.unlock();
                  }
                  // Continue until the client sets this worker's
                  // boolean to true
               } while (!canUpdate);
               
               // Acquire the output stream lock again to send the node back
               oosLock.lock();
               try {
                  // Update the node at the given index
                  update(nodeIndex);
                  // Send the newly updated node to the server
                  output.writeObject(nodeArray.get(nodeIndex));
                  output.flush();
                  // Print out the newly shuffled node
                  System.out.println(workerNumber + " shuffled " + nodeIndex + 
                          " to " + nodeArray.get(nodeIndex).getChars());
               } finally {
                  oosLock.unlock();
               }

            } catch (InterruptedException ex) {
               Logger.getLogger(
                       Worker.class.getName()).log(Level.SEVERE, null, ex);
            }
         } catch (IOException ex) {
            Logger.getLogger(
                    Worker.class.getName()).log(Level.SEVERE, null, ex);
         }
         try {
            // 10ms delay
            sleep(10);
            // Catches an exception and prints the stack trace
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }

   // Get the status of the worker's update availability
   public Condition getUpdateNode() {
      return updateNode;
   }

   // Set the status of the worker's update availability
   public void setCanUpdate(boolean canUpdate) {
      this.canUpdate = canUpdate;
   }

   // Update the node by calling its shuffleArray function
   public void update(int index) {
      nodeArray.get(index).shuffleArray();
      nodeArray.get(index).setWorker(workerNumber);
   }
}
