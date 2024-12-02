import VotingSystem.Observer;
import com.zeroc.Ice.Current;

public class ObserverI implements Observer {
    @Override
    public void update(String message, Current current) {
        System.out.println("Notificación recibida: " + message);
    }
}

