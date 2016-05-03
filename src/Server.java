
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

   static int clientNumber = 0;
   static AtomicReferenceArray<Node> nodes = new AtomicReferenceArray<>(150);
   static ArrayList<ClientThread> clients = new ArrayList<>();
   static boolean[] tokens = new boolean[150];

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

   public static void shareToAll(Node node, int sendingClient) {
      for (int i = 0; i < clients.size(); i++) {
         try {
            (clients.get(i)).getToClient().writeObject(node);
            (clients.get(i)).getToClient().flush();
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

      @Override
      public void run() {
         try {
            toClient = new ObjectOutputStream(client.getOutputStream());
            fromClient = new ObjectInputStream(client.getInputStream());

            toClient.writeObject(nodes);
            Object in;
            try {
               while (true) {
                  in = fromClient.readObject();
                  if (in instanceof UpdateRequest) {
                     UpdateRequest request = (UpdateRequest) in;
                     int worker = request.getWorkerID();
                     int node = request.getWorkerNode();
                     if (!tokens[node]) {
                        toClient.writeObject(new UpdateResponse(worker, node, true));
                        tokens[node] = true;
                     } else {
                        requests.add(request);
                        toClient.writeObject(new UpdateResponse(worker, node, false));
                     }
                  }
                  if (in instanceof Node) {
                     Node updatedNode = (Node) in;
                     int index = ((Node) in).getIndex();
                     nodes.set(index, updatedNode);
                     shareToAll((Node) in, clientNumber);
                     tokens[index] = false;
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

            fromClient.close();
            toClient.close();
            client.close();
         } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
         }
      }
   }

}
