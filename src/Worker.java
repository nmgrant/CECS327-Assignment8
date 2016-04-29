
import java.io.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Worker extends Thread {

   private CopyOnWriteArrayList<Node> nodeArray;
   ObjectOutputStream output;

   public Worker(CopyOnWriteArrayList<Node> nodeArray, ObjectOutputStream oos) {
      this.nodeArray = nodeArray;
      this.output = oos;
   }

   public void run() {
      for (int i = 0; i < 200; i++) {
         Node updatedNode = update();
         try {
            sleep(10);
            // Catches an exception and prints the stack trace
         } catch (Exception ex) {
            ex.printStackTrace();
         }
         try {
            output.writeObject(updatedNode);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public Node update() {
      int index = new Random().nextInt(nodeArray.size());
      nodeArray.get(index).shuffleArray();
      return nodeArray.get(index);
   }
}
