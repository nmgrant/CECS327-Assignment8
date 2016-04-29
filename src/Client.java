
import java.net.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {

   public static void main(String[] args) {
      Worker[] workers = new Worker[100];
      String s;
      if (args.length != 3) {
         System.out.println("Usage: java VerySimpleBrowser host port file");
         System.exit(1);
      }
      try {
         int port = Integer.parseInt(args[1]);
         Socket server = new Socket(args[0], port);
         System.out.println("Connected to host " + server.getInetAddress());
         
         ObjectInputStream fromServer = new ObjectInputStream(server.getInputStream());
         ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
         
         CopyOnWriteArrayList<Node> nodeArray = (CopyOnWriteArrayList<Node>)fromServer.readObject();
         
         for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(nodeArray, toServer);
         }

         fromServer.close();
         toServer.close();
         server.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
