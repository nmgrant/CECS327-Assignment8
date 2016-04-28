
import java.util.Random;

public class Worker extends Thread {
   
   private Node[] nodeArray;
   
   public Worker(Node[] nodeArray) {
      this.nodeArray = nodeArray;
   }
   
   public void run() {
      for (int i = 0; i < 200; i ++) {
         operate();
         try {
            sleep(10);
            // Catches an exception and prints the stack trace
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
   
   public void operate() {
      (nodeArray[new Random().nextInt(nodeArray.length)]).shuffleArray();
   }
}
