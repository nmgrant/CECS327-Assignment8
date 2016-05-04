
import java.io.Serializable;
import java.util.Random;

public class Node implements Serializable {
   private char[] charArray;
   private int index;
   private int worker;

   public Node(int index, int worker) {
      charArray = new char[500];
      this.index = index;
      this.worker = worker;

      for (int i = 0; i < charArray.length; i++) {
         Random rng = new Random();
         char initValue = (char) (rng.nextInt(26) + 'A');
         charArray[i] = initValue;
      }
   }

   public int getIndex() {
      return index;
   }
   
   public int getWorker() {
      return worker;
   }
   
   public void setWorker(int w) {
      worker = w;
   }

   public void shuffleArray() {
      Random rng = new Random();

      for (int i = charArray.length - 1; i > 0; i--) {
         int newPosition = rng.nextInt(i + 1);
         char temp = charArray[i];
         charArray[i] = charArray[newPosition];
         charArray[newPosition] = temp;
      }
   }
   
   public String getChars() {
      String result = "";
      for (int i = 0; i < 5; i++) {
         result += charArray[i];
      }
      return result;
   }
}
