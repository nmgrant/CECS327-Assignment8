
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler {

   private ObjectOutputStream toServer;

   public RequestHandler(ObjectOutputStream oos) {
      toServer = oos;
   }

   public void sendRequest(Object object) {
      try {
         toServer.writeObject(object);
         toServer.flush();
      } catch (IOException ex) {
         Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
      }
   }
}
