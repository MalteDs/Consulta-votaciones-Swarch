import com.zeroc.Ice.*;
import VotingSystem.*;

import java.util.*;
import java.lang.Exception;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private final VotingServicePrx votingService;
    private final ExecutorService threadPool;
    private final String clientId;

    public Client(VotingServicePrx votingService, int poolSize, String clientId) {
        this.votingService = votingService;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        this.clientId = clientId;
    }

    public void register() {
        try {
            votingService.registerClient(clientId);
            System.out.println("Cliente registrado: " + clientId);
        } catch (VotingSystemException e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        try {
            votingService.unregisterClient(clientId);
            System.out.println("Cliente desregistrado: " + clientId);
        } catch (VotingSystemException e) {
            e.printStackTrace();
        }
    }

    public void queryVotingInfo(String documentId) {
        threadPool.submit(() -> {
            try {
                long startTime = System.currentTimeMillis();
                VoterInfo info = votingService.getVotingInfo(documentId);
                long endTime = System.currentTimeMillis();
                logResult(documentId, info, endTime - startTime);
            } catch (VotingSystemException e) {
                e.printStackTrace();
            }
        });
    }

    public void performBatchQuery(String[] documentIds) {
        for (String documentId : documentIds) {
            queryVotingInfo(documentId);
        }
    }

    public void logResult(String documentId, VoterInfo voterInfo, long responseTime) {
        // Implementar la lógica para registrar los resultados
        System.out.println("Documento: " + voterInfo.documentId + "\n" +
               "Nombre: " + voterInfo.firstName + " " + voterInfo.lastName + "\n" +
               "Mesa: " + voterInfo.table.location + ", " + voterInfo.table.city + "\n" +
               "Puesto de votación: " + voterInfo.table.votingPlace + "\n" +
               "Es primo: " + (voterInfo.isPrimeFactorsPrime ? "Sí" : "No") + "\n" +
               "Tiempo de respuesta: " + voterInfo.responseTime + "s");
    }

    public static void main(String[] args) {
        String clientId;

        try (Communicator communicator = Util.initialize(args, "client.cfg")) {
            ObjectPrx base = communicator.stringToProxy("VotingService:default -p 10000");
            VotingServicePrx votingService = VotingServicePrx.checkedCast(base);
            if (votingService == null) {
                throw new RuntimeException("No se pudo conectar con el servidor.");
            }

            try {
                InetAddress localHost = InetAddress.getLocalHost();
                clientId = localHost.getHostAddress();
            } catch (UnknownHostException e) {
                clientId = "UnknownHost"; // En caso de error, utiliza un valor por defecto
                System.err.println("No se pudo obtener la dirección IP del cliente: " + e.getMessage());
            }

            

            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingrese el tamaño del pool de hilos: ");
            int poolSize = scanner.nextInt();
            scanner.nextLine(); // Consumir la nueva línea

            ObjectAdapter adapter = communicator.createObjectAdapter("");
            ObserverPrx observer = ObserverPrx.uncheckedCast(
                adapter.add(new ObserverI(poolSize), Util.stringToIdentity(clientId))
            );
            adapter.activate();

            Client client = new Client(votingService, poolSize, clientId);
            client.register();

            System.out.println("Cliente registrado con ID: " + clientId);

            while (true) {
                System.out.print("Ingrese el ID del votante (o 'exit' para salir): ");
                String documentId = scanner.nextLine();
                if (documentId.equalsIgnoreCase("exit")) {
                    break;
                }
                client.queryVotingInfo(documentId);
            }

            client.unregister();
        }
    }
}