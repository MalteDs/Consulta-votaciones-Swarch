import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.sql.Connection;
import java.util.*;


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
    public final Map<String, ObserverPrx> clients = new HashMap<>();

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
                float queryTime = (endTime - startTime) / 1e9f;

                // Crear el objeto VoterInfo
                VoterInfo voterInfo = new VoterInfo(documentId, table, isPrimeFactorsPrime, queryTime);

                // Registrar la consulta en el log
                logSingleQuery(current.id.name, voterInfo);

                // Devolver la información del votante
                return voterInfo;
            } else {
                throw new VotingSystemException("Documento no encontrado: " + documentId);
            }
        } catch (SQLException e) {
            throw new VotingSystemException("Error al consultar en la base de datos: " + e.getMessage());
        }
    }

    private void logSingleQuery(String clientId, VoterInfo info) {
        String logEntry = String.format("%s,%s,%s,%s,%d,%.3f\n",
            clientId != null ? clientId : "Servidor",
            info.documentId,
            info.table.location,
            info.table.city,
            info.isPrimeFactorsPrime ? 1 : 0,
            info.responseTime
        );

        // Registrar en el archivo de log.
        try {
            Files.write(Paths.get("server_log.csv"), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Error registrando log en el servidor: " + e.getMessage());
        }
    }


    @Override
    public VoterInfo[] performBatchQuery(String clientId, String[] documentIds, Current current) throws VotingSystemException {
        List<VoterInfo> results = new ArrayList<>();
        for (String documentId : documentIds) {
            results.add(getVotingInfo(documentId, current)); // Cada consulta se registrará automáticamente en el log.
        }
        return results.toArray(new VoterInfo[0]);
    }


    @Override
    public void notifyObservers(String message, Current current) throws VotingSystemException {
        synchronized (clients) {
            Iterator<Map.Entry<String, ObserverPrx>> iterator = clients.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ObserverPrx> entry = iterator.next();
                try {
                    entry.getValue().updateAsync(message);
                    System.out.println("Mensaje enviado a cliente: " + entry.getKey());
                } catch (Exception e) {
                    System.err.println("Error notificando al cliente " + entry.getKey() + ": " + e.getMessage());
                    iterator.remove(); // Eliminar cliente no respondiente.
                }
            }
        }
    }
    

    @Override
    public String[] getServerLog(Current current) throws VotingSystemException {
        try {
            // Leer el archivo de log del servidor.
            Path logPath = Paths.get("server_log.csv");
            if (Files.exists(logPath)) {
                return Files.readAllLines(logPath).toArray(new String[0]);
            } else {
                throw new VotingSystemException("No se encontró el archivo de log del servidor.");
            }
        } catch (IOException e) {
            throw new VotingSystemException("Error leyendo el log del servidor: " + e.getMessage());
        }
    }

    @Override
    public void distributeTasks(String[] documentIds, Current current) throws VotingSystemException {
        // Cantidad máxima de consultas que el servidor puede manejar en un segundo.
        final int MAX_QUERIES_PER_SECOND = 100; // Ajustar según pruebas de carga del servidor.
    
        if (documentIds.length <= MAX_QUERIES_PER_SECOND) {
            // Si el servidor puede manejar todas las consultas, procesarlas localmente.
            System.out.println("El servidor procesará todas las consultas.");
            performLocalBatchQuery(documentIds);
        } else {
            // Dividir las consultas entre los clientes registrados.
            synchronized (clients) {
                int totalClients = clients.size();
                if (totalClients == 0) {
                    throw new VotingSystemException("No hay clientes registrados para distribuir tareas.");
                }
    
                int batchSize = (int) Math.ceil((double) documentIds.length / totalClients);
                Iterator<Map.Entry<String, ObserverPrx>> clientIterator = clients.entrySet().iterator();
    
                for (int i = 0; i < documentIds.length; i += batchSize) {
                    List<String> batch = Arrays.asList(documentIds).subList(i, Math.min(i + batchSize, documentIds.length));
                    if (clientIterator.hasNext()) {
                        Map.Entry<String, ObserverPrx> clientEntry = clientIterator.next();
                        String clientId = clientEntry.getKey();
                        ObserverPrx client = clientEntry.getValue();
    
                        try {
                            client.updateAsync("Asignadas " + batch.size() + " tareas: " + String.join(", ", batch));
                            System.out.println("Tareas enviadas a cliente: " + clientId);
                        } catch (Exception e) {
                            System.err.println("Error notificando al cliente " + clientId + ": " + e.getMessage());
                        }
                    } else {
                        // Volver al primer cliente si los clientes son menos que los lotes de documentos.
                        clientIterator = clients.entrySet().iterator();
                    }
                }
            }
        }
    }
    
    private void performLocalBatchQuery(String[] documentIds) {
        for (String documentId : documentIds) {
            try {
                VoterInfo info = getVotingInfo(documentId, null);
                System.out.println("Procesado localmente: " + info.documentId);
            } catch (VotingSystemException e) {
                System.err.println("Error procesando documento " + documentId + ": " + e.getMessage());
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
