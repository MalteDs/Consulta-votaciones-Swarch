import com.zeroc.Ice.*;
import VotingSystem.*;

import java.util.*;
import java.lang.Exception;

public class Client {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "client.cfg")) {
            // Crear el proxy del servidor
            VotingServicePrx server = VotingServicePrx.checkedCast(
                communicator.stringToProxy("VotingService:default -p 10000")
            );

            if (server == null) {
                throw new RuntimeException("No se pudo conectar con el servidor.");
            }

            // Registrar cliente con un ID único
            String clientId = UUID.randomUUID().toString();
            System.out.println("ID : "+clientId);

            ObserverPrx observer = ObserverPrx.uncheckedCast(
                communicator.createObjectAdapter("").add(new ObserverI(), Util.stringToIdentity(clientId))
            );

            server.registerClient(clientId);

            System.out.println("Cliente registrado con ID: " + clientId);

            // Consultar información de votante
            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingrese el ID del votante: ");
            String documentId = scanner.nextLine();

            VoterInfo voterInfo = server.getVotingInfo(documentId);
            System.out.println("Información del votante: " + voterInfo);

        } catch (VotingSystemException e) {
            System.err.println("Error del sistema de votación: " + e.message);
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
}
