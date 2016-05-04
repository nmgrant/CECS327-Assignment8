
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

   static int clientNumber = 0;
   static AtomicReferenceArray<Node> nodes = new AtomicReferenceArray<>(150);
   static HashSet<ObjectOutputStream> clients = new HashSet<>();
   static boolean[] tokens = new boolean[150];
   static ReentrantLock oosLock;

   public static void main(String[] args) {
      for (int i = 0; i < nodes.length(); i++) {
         nodes.set(i, new Node(i, 0));
      }

      System.out.println("Server is running");

      oosLock = new ReentrantLock();

      try {
         ServerSocket listener
                 = new ServerSocket(3333);
         while (true) {
            Socket socket = listener.accept();
            ClientThread client = new ClientThread(socket, clientNumber++, nodes);
            client.start();
            System.out.println("ThreadedWebServer Connected to "
                    + listener.getInetAddress());
         }
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }

   public static void removeDisconnectedClient(ObjectOutputStream out) {
      Iterator<ObjectOutputStream> iter = clients.iterator();

      while (iter.hasNext()) {
         ObjectOutputStream oos = iter.next();

         if (oos == out) {
            iter.remove();
         }
      }
   }

   public static void shareToAll(Node node) {
      for (ObjectOutputStream out : clients) {
         try {
            out.writeObject(node);
         } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }

   static class ClientThread extends Thread {

      private Socket client;
      private int clientNumber;
      ObjectInputStream fromClient;
      ObjectOutputStream toClient;
      private AtomicReferenceArray<Node> nodes;
      private LinkedBlockingQueue<UpdateRequest> requests = new LinkedBlockingQueue<>();

      public ClientThread(Socket socket, int clientNum, AtomicReferenceArray<Node> nodes) {

         this.client = socket;
         this.clientNumber = clientNum;
         this.nodes = nodes;

         System.out.println("New connection with client# " + clientNumber + " at " + socket);

      }

      public ObjectInputStream getFromClient() {
         return fromClient;
      }

      public ObjectOutputStream getToClient() {
         return toClient;
      }

      public int getClientNumber() {
         return clientNumber;
      }

      @Override
      public void run() {
         try {

            toClient = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
            toClient.flush();
            fromClient = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));

            toClient.writeObject(nodes);
            toClient.flush();

            clients.add(toClient);

            Object in;
            try {
               while (true) {
                  try {
                     int total = 0;
                     for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i]) {
                           total++;
                        }
                     }
                     System.out.println(total);
                     in = fromClient.readObject();
                  } catch (SocketException ex) {
                     break;
                  } catch (IOException ex) {
                     break;
                  }
                  if (in instanceof UpdateRequest) {
                     UpdateRequest request = (UpdateRequest) in;
                     int worker = request.getWorkerID();
                     int node = request.getWorkerNode();
                     if (!tokens[node]) {
                        oosLock.lock();
                        try {
                           toClient.writeObject(new UpdateResponse(worker, node, true));
                           toClient.flush();
                           tokens[node] = true;
                           System.out.println(worker + " can work " + node);
                        } finally {
                           oosLock.unlock();
                        }
                     } else {
                        oosLock.lock();
                        try {
                           toClient.reset();
                           toClient.writeObject(new UpdateResponse(worker, node, false));
                           System.out.println(worker + " can't work " + node);
                           toClient.flush();
                        } finally {
                           oosLock.unlock();
                        }
                     }
                  }
                  if (in instanceof Node) {
                     Node updatedNode = (Node) in;
                     int index = ((Node) in).getIndex();
                     nodes.set(index, updatedNode);
                     oosLock.lock();
                     try {
                        for (ObjectOutputStream out : clients) {
                           out.reset();
                           out.writeObject(updatedNode);
                           out.flush();
                        }
                     } finally {
                        oosLock.unlock();
                     }
                     tokens[index] = false;
                     System.out.println("Node " + index + ": " + updatedNode.getChars());
                  }
               }
            } catch (ClassNotFoundException ex) {
               Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (EOFException ef) {
               client.close();
            }
            System.out.println("Client disconnected");
            System.out.println(tokens);
            removeDisconnectedClient(toClient);
            client.close();
         } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
         }
      }
   }

}
