
import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

public class Server {

   public static void main(String[] args) {
      try {
         ServerSocket server
                 = new ServerSocket(Integer.parseInt(args[0]));
         while (true) {
            Socket client = server.accept();
            new ClientThread(client);
            System.out.println("ThreadedWebServer Connected to "
                    + client.getInetAddress());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}

class ClientThread extends Thread {

   Socket client;
   BufferedReader fromClient;
   PrintWriter toClient;

   public ClientThread(Socket c) {
      try {
         client = c;
         fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
         toClient = new PrintWriter(client.getOutputStream(), true);
         start();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void run() {
      try {
         String s;
         s = fromClient.readLine();
         StringTokenizer tokens = new StringTokenizer(s);
         if (!(tokens.nextToken()).equals("GET")) {
            toClient.println("HTTP/1.0 501 Not Implemented");
            toClient.println();
         } else {
            String filename = tokens.nextToken();
            while (!(s = fromClient.readLine()).equals(""));
            BufferedReader file
                    = new BufferedReader(new FileReader(filename));
            toClient.println("HTTP/1.0 200 OK");
            toClient.println("Content-type: text/plain");
            toClient.println();
            while ((s = file.readLine()) != null) {
               toClient.println(s);
            }
            file.close();
         }
         fromClient.close();
         toClient.close();
         client.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
