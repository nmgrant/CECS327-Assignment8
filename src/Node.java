
import java.io.Serializable;
import java.util.Random;

public class Node implements Serializable {
   private char[] charArray;
   private int index;

   public Node(int index) {
      charArray = new char[500];
      this.index = index;

      for (int i = 0; i < charArray.length; i++) {
         Random rng = new Random();
         char initValue = (char) (rng.nextInt(52) + 'A');
         charArray[i] = initValue;
      }
   }

   public int getIndex() {
      return index;
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

   public char getFirst() {
      return charArray[0];
   }
}
