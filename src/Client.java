
import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// This class represents a client that connects to the server through the given
// socket and retrieves an object input stream and object output stream using
// the given socket. The client initializes a set number of workers and then 
// gives each worker a lock and condition. It gives each worker a copy of the
// node array, the object input stream, and the object output stream. It then
// starts all the workers. In an infinte loop the client then receives 
// responses from the sever and parse the response to either update the given
// node array it receives or determine if the worker can work on its requested
// node or to sleep.
public class Client {

   // Initializes the number of workers to 100
   private final int NUM_OF_WORKERS = 100;
   // Declares an atomic reference array of nodes, object input stream, object
   // output stream, socket, and worker array
   private AtomicReferenceArray<Node> nodes;
   private ObjectInputStream fromServer;
   private ObjectOutputStream toServer;
   private Socket server;
   private Worker[] workers;
   // Initializes a reentrant lock array and condition array with the number of 
   // workers
   private ReentrantLock[] workerLocks = new ReentrantLock[NUM_OF_WORKERS];
   private Condition[] workerConditions = new Condition[NUM_OF_WORKERS];

   // Client constructor that initializes the workers array to the number of 
   // workers and intializes each worker in the reentrant lock and condition
   // arrays with a new reentrant lock and condition
   public Client() {
      workers = new Worker[NUM_OF_WORKERS];
      for (int i = 0; i < NUM_OF_WORKERS; i++) {
         workerLocks[i] = new ReentrantLock();
         workerConditions[i] = workerLocks[i].newCondition();
      }
   }

   // Main method that creates a new client object and calls the connect to 
   // server method with the given parameters of localhost and port 3333
   public static void main(String[] args) {
      Client client = new Client();
      client.connectToServer("localhost", 3333);
   }

   // Connect to server method that takes in a string for the host and the port
   // number and connects the client to the server
   public void connectToServer(String host, int port) {
      try {
         // Sets the server to a new socket with the given host and port number
         server = new Socket(host, port);
         // Prints out the connection
         System.out.println("Connected to host " + server.getInetAddress());

         // Sets toServer to a new object output stream from the server's output
         // stream
         toServer = new ObjectOutputStream(
            new BufferedOutputStream(server.getOutputStream()));
         // Sets fromServer to a new object input stream from the server's input
         // stream
         fromServer = new ObjectInputStream(new BufferedInputStream(server.getInputStream()));
         // Sets a new object output stream lock to a new reentrant lock
         ReentrantLock oosLock = new ReentrantLock();
         // Sets node to the read node object from server
         nodes = (AtomicReferenceArray<Node>) fromServer.readObject();

         // For loop to initialize all the workers with the given nodes, 
         // object output stream, object inputstream, lock, condition, object
         // output stream lock, and index number
         for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(nodes, toServer, fromServer, workerLocks[i],
               workerConditions[i], oosLock, i);
         }

         // For each worker in the workers array, starts the thread
         for (Worker worker : workers) {
            worker.start();
         }

         while (true) {
            // Initializes an object in to null
            Object in = null;
            try {
               try {
                  // Reads the object from the server and sets it to in
                  in = fromServer.readObject();
               } catch (SocketException ex) {
               }
               // If the read object is of type node run through this if 
               // statement
               if (in instanceof Node) {
                  // Sets a node object to the read object
                  Node node = (Node)in;
                  // Sets nodes to the given index and the read node
                  nodes.set(node.getIndex(), node);
               // Else if the read object is of type update response run through
               // this else if statement
               } else if (in instanceof UpdateResponse) {
                  // Sets an updateresponse object to the read object
                  UpdateResponse response = (UpdateResponse)in;
                  // Sets an int variable to the response's worker id
                  int worker = response.getWorkerID();
                  // Sets an int variable to the response's node number
                  int node = response.getWorkerNode();
                  // Initializes a boolean variable to the response's value of
                  // isAvailable method
                  boolean canUpdate = response.isAvailable();

                  // If the node is available to update set the worker at the 
                  // given index to be able to update
                  if (canUpdate) {
                     workers[worker].setCanUpdate(true);
                  // Else set the worker at the given index to not be able to 
                  // update
                  } else {
                     workers[worker].setCanUpdate(false);
                  }
                  // Lock the workerlock at the given worker id
                  workerLocks[worker].lock();
                  try {
                     // Signal the worker's condition at the given worker id
                     workerConditions[worker].signal();
                  } finally {
                     // Finally unlock the worker at the given worker id
                     workerLocks[worker].unlock();
                  }
               }
            } catch (EOFException ef) {
               // If an exception is caught, close the object output, input, and
               // server stream
               fromServer.close();
               toServer.close();
               server.close();
            } catch (IOException | ClassNotFoundException ex) {
               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      } catch (IOException | ClassNotFoundException e) {
         e.printStackTrace();
      }
   }
//   public class ResponseHandler extends Thread {
//
//      public ResponseHandler() {
//      }
//
//      @Override
//      public void run() {
//         while (true) {
//            Object in = null;
//            try {
//               try {
//                  in = fromServer.readObject();
//               } catch (SocketException ex) {
//                  System.out.println("socket closed");
//                  server.close();
//                  break;
//               }
//               if (in instanceof Node) {
//                  Node node = (Node) in;
//                  nodes.set(node.getIndex(), node);
//               } else if (in instanceof UpdateResponse) {
//                  UpdateResponse response = (UpdateResponse) in;
//                  int worker = response.getWorkerID();
//                  int node = response.getWorkerNode();
//                  boolean canUpdate = response.isAvailable();
//
//                  if (canUpdate) {
//                     workers[worker].setCanUpdate(true);
//                  } else {
//                     workers[worker].setCanUpdate(false);
//                  }
//                  workerLocks[worker].lock();
//                  try {
//                     workerConditions[worker].signal();
//                  } finally {
//                     workerLocks[worker].unlock();
//                  }
//               }
//            } catch (EOFException eof) {
//               try {
//                  System.out.println("socket closed2");
//                  server.close();
//                  break;
//               } catch (IOException ioe) {
//                  Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ioe);
//               }
//            } catch (IOException | ClassNotFoundException ex) {
//               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
//            }
//         }
//      }
//   }
}
