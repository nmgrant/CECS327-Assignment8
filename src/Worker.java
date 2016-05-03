
import java.io.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker extends Thread {

   private AtomicReferenceArray<Node> nodeArray;
   ObjectOutputStream output;
   ObjectInputStream input;
   int workerNumber;
   ReentrantLock lock;
   ReentrantLock oosLock;
   Condition updateNode;
   boolean canUpdate;

   public Worker(AtomicReferenceArray<Node> nodeArray, ObjectOutputStream oos,
           ObjectInputStream ois, ReentrantLock l, Condition c, ReentrantLock oosLock, int wn) {
      this.nodeArray = nodeArray;
      this.output = oos;
      this.input = ois;
      workerNumber = wn;
      lock = l;
      updateNode = c;
      this.oosLock = oosLock;
   }

   public void run() {
      for (int i = 0; i < 200; i++) {
         int nodeIndex = new Random().nextInt(nodeArray.length());
         try {
            try {
               do {
                  lock.lock();
                  try {

                     oosLock.lock();
                     try {
                        output.writeObject(new UpdateRequest(workerNumber, nodeIndex));
                     } finally {
                        oosLock.unlock();
                     }

                     updateNode.await();
                  } finally {
                     lock.unlock();
                  }
               } while (!canUpdate);

               oosLock.lock();
               try {
                  output.writeObject(update(nodeIndex));
                  System.out.println(workerNumber + " is shuffling " + nodeIndex);
               } finally {
                  oosLock.unlock();
               }

            } catch (InterruptedException ex) {
               Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
            }
         } catch (IOException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
         }
         try {
            sleep(10);
            // Catches an exception and prints the stack trace
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }

   public Condition getUpdateNode() {
      return updateNode;
   }

   public void setCanUpdate(boolean canUpdate) {
      this.canUpdate = canUpdate;
   }

   public Node update(int index) {
      nodeArray.get(index).shuffleArray();
      nodeArray.get(index).setWorker(workerNumber);
      return nodeArray.get(index);
   }
}
