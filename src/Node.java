
import java.io.Serializable;
import java.util.Random;

// This class represents a node that holds a char array of 500 characters. The
// server initializes an array of 150 nodes each with their own respective char
// arrays that is to be worked on by 100 workers from each client. The clients 
// utilize the shuffle array method to shuffle the given node's char array when
// access is granted.
public class Node implements Serializable {

   // Initialize the size of the char array in each node
   private final int NUM_OF_CHARS = 500;
   // Declare the char array and variables for the index and worker number
   private char[] charArray;
   private int index, worker;

   // Node constructor that takes in an index and worker number
   public Node(int index, int worker) {
      // Initialize the char array with the size of the char array
      charArray = new char[NUM_OF_CHARS];
      // Set index and worker to the variables past through the constructor
      this.index = index;
      this.worker = worker;

      // For loop to run through the char array and initialize the values with 
      // random char
      for (int i = 0; i < charArray.length; i++) {
         Random rng = new Random();
         char initValue = (char) (rng.nextInt(26) + 'A');
         charArray[i] = initValue;
      }
   }

   // Get method to return the index
   public int getIndex() {
      return index;
   }

   // Get method to return the worker
   public int getWorker() {
      return worker;
   }

   // Set method to change the worker value
   public void setWorker(int w) {
      worker = w;
   }

   // Shuffle array method to shuffle the array of char
   public void shuffleArray() {
      Random rng = new Random();
      for (int i = charArray.length - 1; i > 0; i--) {
         int newPosition = rng.nextInt(i + 1);
         char temp = charArray[i];
         charArray[i] = charArray[newPosition];
         charArray[newPosition] = temp;
      }
   }

   // Get char method to for printing out the first 50 characters of the array 
   // for verification that the shuffle was made
   public String getChars() {
      String result = "";
      for (int i = 0; i < 50; i++) {
         result += charArray[i];
      }
      return result;
   }
}
