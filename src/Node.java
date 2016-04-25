import java.util.Random;

public class Node {
   private char[] charArray;

   public Node() {
      charArray = new char[500];
      
      for (int i = 0; i < charArray.length; i++) {
         Random rng = new Random();
         char initValue = (char)(rng.nextInt(52) + 'A');
         charArray[i] = initValue;
      }
   }

   public static void shuffleArray(char[] array) {
      Random rng = new Random();
      
      for (int i = array.length - 1; i > 0; i--) {
         int newPosition = rng.nextInt(i + 1);
         char temp = array[i];
         array[i] = array[newPosition];
         array[newPosition] = temp;
      }
   }
}
