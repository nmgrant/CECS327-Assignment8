
import java.net.*;
import java.io.*;
import java.util.ArrayList;
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
   static ArrayList<ClientThread> clients = new ArrayList<>();
   static AtomicIntegerArray tokens = new AtomicIntegerArray(150);

   public static void main(String[] args) {
      for (int i = 0; i < nodes.length(); i++) {
         nodes.set(i, new Node(i, 0));
      }

      System.out.println("Server is running");

      try {
         ServerSocket listener
                 = new ServerSocket(3333);
         while (true) {
            Socket socket = listener.accept();
            ClientThread client = new ClientThread(socket, clientNumber++, nodes);
            clients.add(client);
            client.start();
            System.out.println("ThreadedWebServer Connected to "
                    + listener.getInetAddress());
         }
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }

   public static void removeDisconnectedClient(ClientThread client) {
      Iterator<ClientThread> iter = clients.iterator();

      while (iter.hasNext()) {
         ClientThread thread = iter.next();

         if (thread == client) {
            iter.remove();
         }
      }

      for (ClientThread thread : clients) {
         if (thread == client) {
            clients.remove(thread);
         }
      }
   }

   public static void shareToAll(Node node, int sendingClient) {
      for (int i = 0; i < clients.size(); i++) {
         if (clients.get(i).getClientNumber() != sendingClient) {
            try {
               (clients.get(i)).getToClient().writeObject(node);
               (clients.get(i)).getToClient().flush();
            } catch (IOException ex) {
               Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
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
            toClient = new ObjectOutputStream(client.getOutputStream());
            fromClient = new ObjectInputStream(client.getInputStream());

            toClient.writeObject(nodes);
            Object in;
            try {
               while (true) {
                  try {
                     in = fromClient.readObject();
                  } catch(SocketException ex) {
                     break;
                  } catch(IOException ex) {
                     break;
                  }
                  if (in instanceof UpdateRequest) {
                     UpdateRequest request = (UpdateRequest) in;
                     int worker = request.getWorkerID();
                     int node = request.getWorkerNode();
                     if (tokens.compareAndSet(node, 0, 1)) {
                        toClient.writeObject(new UpdateResponse(worker, node, true));
                     } else {
                        toClient.writeObject(new UpdateResponse(worker, node, false));
                     }
                  }
                  if (in instanceof Node) {
                     Node updatedNode = (Node) in;
                     int index = ((Node) in).getIndex();
                     nodes.set(index, updatedNode);
                     shareToAll((Node) in, clientNumber);
                     tokens.set(index, 0);
                     System.out.println("Node " + index + ": " + updatedNode.getChars());
                  }
               }
            } catch (ClassNotFoundException ex) {
               Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (EOFException ef) {
               fromClient.close();
               toClient.close();
               client.close();
            }
            System.out.println("Client disconnected");
            removeDisconnectedClient(this);
            fromClient.close();
            toClient.close();
            client.close();
         } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
         }
      }
   }

}
