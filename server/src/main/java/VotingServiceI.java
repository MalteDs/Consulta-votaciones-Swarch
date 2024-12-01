import com.zeroc.Ice.*;
import com.zeroc.Ice.Object;

import java.sql.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.crypto.Data;

import VotingSystem.ObserverPrx;
import VotingSystem.VoterInfo;
import VotingSystem.VotingService;
import VotingSystem.VotingSystemException;
import VotingSystem.VotingTable;
// import VotingSystem.VotingServicePrx;
// import VotingSystem.VotingServicePrxHelper;
// Ensure the VotingSystem package is correctly defined and VotingService class is available
public class VotingServiceI implements VotingService {
    private final Connection dbConnection;
    private final Map<String, ObserverPrx> clients = new HashMap<>();

    public VotingServiceI() {
        this.dbConnection = connectToDatabase();
    }

    private Connection connectToDatabase() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        return dbConfig.connectToDatabase();
    }

    @Override
    public void registerClient(String clientId, Current current) throws VotingSystemException {
        ObserverPrx client = ObserverPrx.uncheckedCast(current.con.createProxy(current.id));
        synchronized (clients) {
            clients.put(clientId, client);
        }
        System.out.println("Cliente registrado: " + clientId);
    }

    @Override
    public void unregisterClient(String clientId, Current current) throws VotingSystemException {
        synchronized (clients) {
            clients.remove(clientId);
        }
        System.out.println("Cliente desregistrado: " + clientId);
    }

    @Override
    public VoterInfo getVotingInfo(String documentId, Current current) throws VotingSystemException {
        long startTime = System.nanoTime();
        try {
            // Consulta para obtener la información del ciudadano y mesa de votación
            String query = "SELECT c.mesa_id, mv.consecutive, pv.nombre AS puesto_nombre, pv.direccion, "
                         + "m.nombre AS municipio_nombre, d.nombre AS departamento_nombre "
                         + "FROM ciudadano c "
                         + "JOIN mesa_votacion mv ON mv.id = c.mesa_id "
                         + "JOIN puesto_votacion pv ON pv.id = mv.puesto_id "
                         + "JOIN municipio m ON m.id = pv.municipio_id "
                         + "JOIN departamento d ON d.id = m.departamento_id "
                         + "WHERE c.documento = ?";

            PreparedStatement stmt = dbConnection.prepareStatement(query);
            stmt.setString(1, documentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Obtener los resultados
                VotingTable table = new VotingTable(
                    rs.getString("puesto_nombre"),  // Nombre del puesto
                    rs.getString("municipio_nombre") // Nombre del municipio
                );

                // Determinar si el número de factores primos es primo
                boolean isPrimeFactorsPrime = isPrime(countPrimeFactors(Integer.parseInt(documentId)));

                long endTime = System.nanoTime();
                // Devolver la información del votante
                return new VoterInfo(documentId, table, isPrimeFactorsPrime, (endTime - startTime) / 1e9f);
            } else {
                throw new VotingSystemException("Documento no encontrado: " + documentId);
            }
        } catch (SQLException e) {
            throw new VotingSystemException("Error al consultar en la base de datos: " + e.getMessage());
        }
    }

    @Override
    public VoterInfo[] performBatchQuery(String clientId, String[] documentIds, Current current) throws VotingSystemException {
        List<VoterInfo> results = new ArrayList<>();
        for (String documentId : documentIds) {
            results.add(getVotingInfo(documentId, current));
        }
        return results.toArray(new VoterInfo[0]);
    }

    @Override
    public void notifyObservers(String message, Current current) throws VotingSystemException {
        for (ObserverPrx observer : clients.values()) {
            observer.updateAsync(message);
        }
    }

    @Override
    public String[] getServerLog(Current current) throws VotingSystemException {
        return null;
    }

    @Override
    public void distributeTasks(String[] documentIds, Current current) throws VotingSystemException {
        int batchSize = documentIds.length / clients.size();
        List<String> batch = new ArrayList<>();
        int index = 0;

        synchronized (clients) {
            for (Map.Entry<String, ObserverPrx> entry : clients.entrySet()) {
                String clientId = entry.getKey();
                ObserverPrx client = entry.getValue();

                for (int i = 0; i < batchSize && index < documentIds.length; i++) {
                    batch.add(documentIds[index++]);
                }

                if (!batch.isEmpty()) {
                    client.update("Nuevas tareas: " + String.join(", ", batch));
                }
                batch.clear();
            }
        }
    }

    private boolean isPrime(int number) {
        if (number < 2) return false;
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    private int countPrimeFactors(int number) {
        int count = 0;
        for (int i = 2; i <= number; i++) {
            while (number % i == 0) {
                count++;
                number /= i;
            }
        }
        return count;
    }
}
