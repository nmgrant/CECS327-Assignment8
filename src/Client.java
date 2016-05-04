
import java.net.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

   private AtomicReferenceArray<Node> nodes;
   private ObjectInputStream fromServer;
   private ObjectOutputStream toServer;
   private Socket server;
   private Worker[] workers;
   private ReentrantLock[] workerLocks = new ReentrantLock[100];
   private Condition[] workerConditions = new Condition[100];
   private ThreadGroup workerGroup;

   public Client() {
      workers = new Worker[100];
      for (int i = 0; i < 100; i++) {
         workerLocks[i] = new ReentrantLock();
         workerConditions[i] = workerLocks[i].newCondition();
      }
      workerGroup = new ThreadGroup("Workers");
//      if (args.length != 2) {
//         System.out.println("Usage: java VerySimpleBrowser host port");
//         System.exit(1);
//      }

   }

   public void connectToServer(String host, int port) {
      try {
         server = new Socket(host, port);
         System.out.println("Connected to host " + server.getInetAddress());

         toServer = new ObjectOutputStream(new BufferedOutputStream(server.getOutputStream()));
         toServer.flush();
         fromServer = new ObjectInputStream(new BufferedInputStream(server.getInputStream()));
         ReentrantLock oosLock = new ReentrantLock();
         nodes = (AtomicReferenceArray<Node>) fromServer.readObject();

         for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(workerGroup, nodes, toServer, fromServer, workerLocks[i],
                    workerConditions[i], oosLock, i);
         }

         for (Worker worker : workers) {
            worker.start();
         }

//         ResponseHandler responseHandler = new ResponseHandler();
//         responseHandler.start();

         while (true) {
            Object in = null;
            try {
               try {
                  in = fromServer.readObject();
               } catch (SocketException ex) {
               }
               if (in instanceof Node) {
                  Node node = (Node) in;
                  nodes.set(node.getIndex(), node);
                  System.out.println("Received new node:  " + node);
               } else if (in instanceof UpdateResponse) {
                  UpdateResponse response = (UpdateResponse) in;
                  int worker = response.getWorkerID();
                  int node = response.getWorkerNode();
                  boolean canUpdate = response.isAvailable();
                  
                  System.out.println(response);
                  if (canUpdate) {
                     workers[worker].setCanUpdate(true);
                     System.out.println(worker + " can work " + node);
                  } else {
                     workers[worker].setCanUpdate(false);
                     System.out.println(worker + "can't work" + node);
                  }
                  workerLocks[worker].lock();
                  try {
                     workerConditions[worker].signal();
                  } finally {
                     workerLocks[worker].unlock();
                  }
               }
            } catch (EOFException ef) {
               fromServer.close();
               toServer.close();
               server.close();
            } catch (IOException ex) {
               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
         }

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void main(String[] args) {
      Client client = new Client();
      client.connectToServer("localhost", 3333);
   }

   public class ResponseHandler extends Thread {

      public ResponseHandler() {
      }

      @Override
      public void run() {
         while (true) {
            Object in = null;
            try {
               try {
                  in = fromServer.readObject();
               } catch (SocketException ex) {
                  System.out.println("socket closed");
                  System.out.println(workerGroup.activeCount());
                  server.close();
                  break;
               }
               if (in instanceof Node) {
                  Node node = (Node) in;
                  nodes.set(node.getIndex(), node);
                  //System.out.println("Node " + node.getIndex() + ": " + nodes.get(node.getIndex()).getChars());
               } else if (in instanceof UpdateResponse) {
                  UpdateResponse response = (UpdateResponse) in;
                  int worker = response.getWorkerID();
                  int node = response.getWorkerNode();
                  boolean canUpdate = response.isAvailable();

                  System.out.println(response);
                  if (canUpdate) {
                     workers[worker].setCanUpdate(true);
                     System.out.println(worker + " can work " + node);
                  } else {
                     workers[worker].setCanUpdate(false);
                     System.out.println(worker + "can't work" + node);
                  }
                  workerLocks[worker].lock();
                  try {
                     workerConditions[worker].signal();
                  } finally {
                     workerLocks[worker].unlock();
                  }
               }
            } catch (EOFException ef) {
               try {
                  System.out.println("socket closed2");
                  server.close();
                  break;
               } catch (IOException ex) {
                  Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
               }
            } catch (IOException ex) {
               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
               Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
      }
   }

   public class RequestHandler extends Thread {

      private ObjectOutputStream toServer;

      public RequestHandler(ObjectOutputStream oos) {
         toServer = oos;
      }

      public void sendRequest(Object object) {
         try {
            toServer.writeObject(object);
            toServer.flush();
         } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }
}
