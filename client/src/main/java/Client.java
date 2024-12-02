import com.zeroc.Ice.*;
import VotingSystem.*;

import java.util.*;
import java.lang.Exception;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

            String clientId;

            try {
                InetAddress localHost = InetAddress.getLocalHost();
                clientId = localHost.getHostAddress();
            } catch (UnknownHostException e) {
                clientId = "UnknownHost"; // En caso de error, utiliza un valor por defecto
                System.err.println("No se pudo obtener la dirección IP del cliente: " + e.getMessage());
            }

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
            System.out.println(voterToString(voterInfo));

        } catch (VotingSystemException e) {
            System.err.println("Error del sistema de votación: " + e.message);
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
    public static String voterToString(VoterInfo voter){
        return "Información "+voter.documentId+
        "\nCITY            : "+voter.table.city+
        "\nLOCATION        : "+voter.table.location+
        "\nIS PRIME NUMBER : "+voter.isPrimeFactorsPrime+
        "\nResponse Time   : "+voter.responseTime;
    }
    public static String menu(){
        return ""+
        "\n1) Buscar mesa de votación";
    }
}
