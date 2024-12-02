import java.io.*;
import java.util.*;
import com.zeroc.Ice.Current;

import VotingSystem.VotingSystemException;

public class ServerMenu {
    private final VotingServiceI votingService;

    public ServerMenu(VotingServiceI votingService) {
        this.votingService = votingService;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Servidor de Votaciones ===");
        while (true) {
            System.out.println("\nOpciones:");
            System.out.println("1. Procesar archivo de documentos");
            System.out.println("2. Ver estado de clientes registrados");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");

            String option = scanner.nextLine();
            switch (option) {
                case "1":
                    System.out.print("Ingrese la ruta del archivo con documentos: ");
                    String filePath = scanner.nextLine();
                    handleFileProcessing(filePath);
                    break;
                case "2":
                    showRegisteredClients();
                    break;
                case "3":
                    System.out.println("Cerrando servidor...");
                    return;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    private void handleFileProcessing(String filePath) {
        try {
            List<String> documentIds = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));

            String line;
            while ((line = reader.readLine()) != null) {
                documentIds.add(line.trim());
            }
            reader.close();
            System.out.println("\nProcesando " + documentIds.size() + " documentos...");
            votingService.distributeTasks(documentIds.toArray(new String[0]), null);

        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        } catch (VotingSystemException e) {
            System.err.println("Error del sistema de votación: " + e.getMessage());
        }
    }

    private void showRegisteredClients() {
        synchronized (votingService.clients) {
            if (votingService.clients.isEmpty()) {
                System.out.println("No hay clientes registrados.");
            } else {
                System.out.println("Clientes registrados:");
                for (String clientId : votingService.clients.keySet()) {
                    System.out.println("- " + clientId);
                }
            }
        }
    }
}
