
import java.net.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

   public static void main(String[] args) {
      System.out.println("Server is running");
      int clientNumber = 0;
      CopyOnWriteArrayList<Node> nodes = new CopyOnWriteArrayList(new Node[150]);
      try {
         ServerSocket listener
            = new ServerSocket(Integer.parseInt(args[0]));
         while (true) {
            new ClientThread(listener.accept(), clientNumber++, nodes).start();
            System.out.println("ThreadedWebServer Connected to "
               + listener.getInetAddress());
         }
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }
}

class ClientThread extends Thread {

   private Socket client;
   private ObjectInputStream fromClient;
   private ObjectOutputStream toClient;
   private int clientNumber;

   public ClientThread(Socket socket, int clientNum, CopyOnWriteArrayList<Node> node) {
      this.clientNumber = clientNum;
      try {
         this.client = socket;
         this.fromClient = new ObjectInputStream(client.getInputStream());
         this.toClient = new ObjectOutputStream(client.getOutputStream());
         
         toClient.writeObject(node);
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void run() {
      try {
         System.out.println("Client: " + clientNumber + " is connecting");
         
        
         fromClient.close();
         toClient.close();
         client.close();
      } catch (NumberFormatException | IOException e) {
         e.printStackTrace();
      }
   }
}
