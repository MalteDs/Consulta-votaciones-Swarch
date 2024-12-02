
import com.zeroc.Ice.Current;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import VotingSystem.Observer;
import VotingSystem.VoterInfo;
import VotingSystem.VotingServicePrx;
import VotingSystem.VotingSystemException;

public class ObserverI implements Observer {
    private final ExecutorService threadPool;
    private final VotingServicePrx votingService;

    public ObserverI(int poolSize, VotingServicePrx votingService) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        this.votingService = votingService;
    }

    @Override
    public void update(String message, Current current) {
        System.out.println("Received update: " + message);
        // Aquí puedes agregar la lógica para procesar las tareas recibidas del servidor
        processTasks(message);
    }

    private void processTasks(String message) {
        threadPool.submit(() -> {
            // Lógica para procesar las tareas recibidas del servidor
            System.out.println("Processing tasks: " + message);
            // Aquí puedes agregar la lógica específica para procesar las tareas
            String[] documentIds = message.split(",");
            performBatchQuery(documentIds);
        });
    }

    private void performBatchQuery(String[] documentIds) {
        for (String documentId : documentIds) {
            try {
                long startTime = System.currentTimeMillis();
                VoterInfo info = votingService.getVotingInfo(documentId);
                long endTime = System.currentTimeMillis();
                logResult(documentId, info, endTime - startTime);
            } catch (VotingSystemException e) {
                e.printStackTrace();
            }
        }
    }

    private void logResult(String documentId, VoterInfo info, long responseTime) {
        // Implementar la lógica para registrar los resultados
        System.out.println("Documento: " + info.documentId + "\n" +
               "Nombre: " + info.firstName + " " + info.lastName + "\n" +
               "Mesa: " + info.table.location + ", " + info.table.city + "\n" +
               "Puesto de votación: " + info.table.votingPlace + "\n" +
               "Es primo: " + (info.isPrimeFactorsPrime ? "Sí" : "No") + "\n" +
               "Tiempo de respuesta: " + responseTime + "ms");
    }
}