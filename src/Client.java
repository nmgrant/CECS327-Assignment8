
import java.net.*;
import java.io.*;

public class Client {

   public static void main(String[] args) {
      String s;
      if (args.length != 3) {
         System.out.println("Usage: java VerySimpleBrowser host port file");
         System.exit(1);
      }
      try {
         int port = Integer.parseInt(args[1]);
         Socket server = new Socket(args[0], port);
         System.out.println("Connected to host "
                 + server.getInetAddress());
         BufferedReader fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
         PrintWriter toServer = new PrintWriter(server.getOutputStream(), true);
         toServer.println("GET " + args[2] + " HTTP/1.1");
         toServer.println("Host: " + args[0] + ':' + args[1]);
         toServer.println();
         while (!(s = fromServer.readLine()).equals("")) {
            System.out.println(s);
         }
         while ((s = fromServer.readLine()) != null) {
            System.out.println(s);
         }
         fromServer.close();
         toServer.close();
         server.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
