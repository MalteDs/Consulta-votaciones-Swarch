import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;

public class Server {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "server.cfg")) {
            // Crear el adaptador para el servicio VotingService
            ObjectAdapter adapter = communicator.createObjectAdapter("VotingServiceAdapter");

            // Registrar la implementación del servicio
            adapter.add(new VotingServiceI(), Util.stringToIdentity("VotingService"));

            // Activar el adaptador
            adapter.activate();

            System.out.println("Servidor de votaciones iniciado. Esperando conexiones...");
            communicator.waitForShutdown();
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

