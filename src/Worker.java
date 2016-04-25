
import java.util.Random;

public class Worker extends Thread {
   
   private Node[] nodeArray;
   
   public Worker(Node[] nodeArray) {
      this.nodeArray = nodeArray;
   }
   
   public void run() {
      for (int i = 0; i < 200; i ++) {
         operate();
         
      }
   }
   
   public void operate() {
      (nodeArray[new Random().nextInt(nodeArray.length)])
   }
}
