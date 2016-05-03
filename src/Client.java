
import java.net.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

   private AtomicReferenceArray<Node> nodes;
   private ObjectInputStream fromServer;
   private ObjectOutputStream toServer;
   private Worker[] workers;
   private ReentrantLock[] workerLocks = new ReentrantLock[100];
   private Condition[] workerConditions = new Condition[100];

   public Client() {
      workers = new Worker[100];
      for (int i = 0; i < 100; i++) {
         workerLocks[i] = new ReentrantLock();
         workerConditions[i] = workerLocks[i].newCondition();
      }
//      if (args.length != 2) {
//         System.out.println("Usage: java VerySimpleBrowser host port");
//         System.exit(1);
//      }

   }

   public void connectToServer(String host, int port) {
      try {
         Socket server = new Socket(host, port);
         System.out.println("Connected to host " + server.getInetAddress());

         toServer = new ObjectOutputStream(server.getOutputStream());
         fromServer = new ObjectInputStream(server.getInputStream());

         nodes = (AtomicReferenceArray<Node>) fromServer.readObject();

         System.out.println("Nodes: " + nodes);
         for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(nodes, toServer, fromServer, workerLocks[i], workerConditions[i], i);
         }

         for (Worker worker : workers) {
            System.out.println("");
            worker.start();
         }

         Object in;

         while ((in = fromServer.readObject()) != null) {
            if (in instanceof Node) {
               Node node = (Node) in;
               nodes.set(node.getIndex(), node);
            } else if (in instanceof UpdateResponse) {
               UpdateResponse response = (UpdateResponse)in;
               int worker = response.getWorkerID();
               boolean canUpdate = response.isAvailable();

               if (canUpdate) {
                  workers[worker].setCanUpdate(true);
               } else {
                  workers[worker].setCanUpdate(false);
               }
               workerLocks[worker].lock();
               try {
                  workerConditions[worker].signal();
               } finally {
                  workerLocks[worker].unlock();
               }
            }
         }

         fromServer.close();
         toServer.close();
         server.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void main(String[] args) {
      Client client = new Client();
      client.connectToServer("localhost", 3333);
   }
}
