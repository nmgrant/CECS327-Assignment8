
import java.net.*;
import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// This class represents a centralized server that holds an array of 150 nodes
// that a number of clients will place requests to update the contents of each
// node in the array. The server runs infinitely until a client connects and 
// utilizes a client thread to commmunicate between the client and the server. 
// The client thread runs and reads the object output stream from the client and
// returns either an updated response dependent on the variables given or 
// returns an updated node from the worker threads to all other clients so they
// have an updated reference to the updated node.
public class Server {

   // Declare the number of nodes to 150 and client numbers to 0
   private static final int NUM_OF_NODES = 150;
   private static int clientNumber = 0, tokenCounter;
   // Initialize an atomic reference array of type node with the number of nodes
   private static AtomicReferenceArray<Node> nodes
      = new AtomicReferenceArray<>(NUM_OF_NODES);
   // Initalize a hash set of objectoutputstream objects
   private static HashSet<ObjectOutputStream> clients = new HashSet<>();
   // Initialize an integer array of tokens with the number of nodes
   private static AtomicIntegerArray tokens = new AtomicIntegerArray(NUM_OF_NODES);
   // Declare an object output stream lock used to prevent corrupt data
   private static ReentrantLock oosLock;

   // Main method
   public static void main(String[] args) {
      // For loop that runs for the length of the atomic refernce array of nodes
      // and sets the nodes with a given index and new node
      for (int i = 0; i < nodes.length(); i++) {
         nodes.set(i, new Node(i, 0));
      }
      
      if (args.length != 1) {
         System.out.println("Usage: java Server port");
         System.exit(0);
      }

      System.out.println("Server is running");

      // Sets the object output stream lock to a new reentrant lock
      oosLock = new ReentrantLock();

      // Try, catch that declares a seversocket at given port
      try {
         ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
         // Infinite while loop to keep running and listen for any clients 
         while (true) {
            // Initializes a socket to accept a client
            Socket socket = listener.accept();
            // Initializes a clientThread object with the given socket, client
            // number, and the nodes array
            ClientThread client = new ClientThread(socket, clientNumber++,
               nodes);
            // Starts the client thread
            client.start();
         }
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }

   // Remove Disconnected clients method that iterates through an iterator of
   // object output stream and removes any object output streams that are equal
   // to the object output stream being passed in
   public static void removeDisconnectedClient(ObjectOutputStream out) {
      Iterator<ObjectOutputStream> iter = clients.iterator();

      while (iter.hasNext()) {
         ObjectOutputStream oos = iter.next();

         if (oos == out) {
            iter.remove();
         }
      }
   }

   // Share to all method that passes in the given node and for each client,
   // writes the node to the object output stream 
   public static void shareToAll(Node node) {
      for (ObjectOutputStream out : clients) {
         try {
            out.writeObject(node);
         } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }

   // ClientThread class that handles communication between the newly connected
   // clients and the server, allowing the server to continually take in clients 
   static class ClientThread extends Thread {

      // Declaration for a client socket, client number, object output stream
      // object input stream, and an atomic reference array of nodes
      private Socket client;
      private int clientNumber;
      ObjectInputStream fromClient;
      ObjectOutputStream toClient;
      private AtomicReferenceArray<Node> nodes;

      // ClientThread constructor that takes in a socket, client number, and an
      // atomic reference array of nodes
      public ClientThread(Socket socket, int clientNum,
         AtomicReferenceArray<Node> nodes) {
         // Sets the client to the given socket
         this.client = socket;
         // Sets the client number to the given client number
         this.clientNumber = clientNum;
         // Sets the nodes to the given nodes
         this.nodes = nodes;

         // Prints out that a new connection has been made with the client 
         // number at the given socket
         System.out.println("New connection with client# " + clientNumber
            + " at " + socket);
      }

      // Get method to return the object input stream from the client
      public ObjectInputStream getFromClient() {
         return fromClient;
      }

      // Get method to return the object output stream to the client
      public ObjectOutputStream getToClient() {
         return toClient;
      }

      // Get method to return the client number
      public int getClientNumber() {
         return clientNumber;
      }

      @Override
      public void run() {
         try {
            // Sets toClient to a new object output stream from the client's 
            // output stream
            toClient = new ObjectOutputStream(
               new BufferedOutputStream(client.getOutputStream()));
            // Sets fromClient to a new object input stream from the client's
            // input stream
            fromClient = new ObjectInputStream(
               new BufferedInputStream(client.getInputStream()));

            // Writes the nodes array object to the client and flushes
            toClient.writeObject(nodes);
            toClient.flush();

            // Add the toClient object output stream to clients
            clients.add(toClient);

            // Declare an object in
            Object in;
            try {
               while (true) {
                  // Resets the token counter
                  tokenCounter = 0;
                  // For loop to run through the token array and count how many
                  // tokens are in use
                  for (int i = 0; i < tokens.length(); i++) {
                     if (tokens.get(i) == 1) {
                        tokenCounter++;
                     }
                  }
                  // Prints out the number of tokens being used
                  System.out.println("Number of tokens being used: "
                     + tokenCounter);
                  
                  try {
                     // Trys to set the object in to the read object from client
                     in = fromClient.readObject();
                  } catch (SocketException ex) {
                     break;
                  } catch (IOException ex) {
                     break;
                  }
                  // If the object read is of type updaterequest run through 
                  // this if statement
                  if (in instanceof UpdateRequest) {
                     // Set an updaterequest object to the read object
                     UpdateRequest request = (UpdateRequest) in;
                     // Set a int variable worker to the worker id from request
                     int worker = request.getWorkerID();
                     // Set a int variable node to the worker node from request
                     int node = request.getWorkerNode();
                     // If the token is not taken, send the update response
                     // with the given parameters. Atomically sets
                     // the tokens variable to "true" if the token is unused.
                     if (tokens.compareAndSet(node, 0, 1)) {
                        // Object output stream lock to prevent corrupt data
                        oosLock.lock();
                        try {
                           // Resets the toClient object output stream and then
                           // writes the new Updated response to the client,
                           // and finally flushes 
                           toClient.reset();
                           toClient.writeObject(new UpdateResponse(worker, node, true));
                           toClient.flush();
                        } finally {
                           // Finally unlocks the object output stream lock
                           oosLock.unlock();
                        }
                        // Else if the token is taken, send the update response
                        // with the given parameters
                     } else {
                        // Object output stream lock to prevent corrupt data
                        oosLock.lock();
                        try {
                           // Resets the toClient object output stream and then
                           // writes the new uppdated response to the client, 
                           // and finally flushes
                           toClient.reset();
                           toClient.writeObject(new UpdateResponse(worker, node, false));
                           toClient.flush();
                        } finally {
                           // Finally unlocks the object output stream lock
                           oosLock.unlock();
                        }
                     }
                  }
                  // If the object read is of type node then run through this if
                  // statement
                  if (in instanceof Node) {
                     // Sets an object of type node to the read object
                     Node updatedNode = (Node) in;
                     // Sets an int variable index to the read objects index
                     int index = ((Node) in).getIndex();
                     // Sets the node at the given index to the updated node
                     nodes.set(index, updatedNode);
                     // Object output stream lock
                     oosLock.lock();
                     try {
                        // Trys to write the updated node to each client 
                        for (ObjectOutputStream out : clients) {
                           out.reset();
                           out.writeObject(updatedNode);
                           out.flush();
                        }
                     } finally {
                        // Finall unlocks the object output stream lock
                        oosLock.unlock();
                     }
                     // Sets the token at the given index to false, notifying 
                     // that the given index is no longer being workerd on
                     tokens.compareAndSet(index, 1, 0);
                     // Prints out the node at the given index and its updated
                     // contents
                     System.out.println("Node " + index + ": "
                        + updatedNode.getChars());
                  }
               }
            } catch (ClassNotFoundException ex) {
               Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (EOFException ef) {
               client.close();
            }
            // Prints out that a client has disconnected
            System.out.println("Client " + clientNumber + " disconnected");
            // Calls the remove disconnected client method by passing in the
            // toClient object
            removeDisconnectedClient(toClient);
            // Closes client
            client.close();
         } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
         }
      }
   }
}
