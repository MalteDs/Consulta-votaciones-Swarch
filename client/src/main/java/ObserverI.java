import com.zeroc.Ice.Current;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import VotingSystem.Observer;
import VotingSystem.VotingServicePrx;
import VotingSystem.VotingSystemException;

public class ObserverI implements Observer {
    private final ExecutorService threadPool;
    private VotingServicePrx votingService;

    public ObserverI(int poolSize) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void update(String message, Current current) {
        System.out.println("Received update: " + message);
        processTasks(message);
    }

    private void processTasks(String message) {
        threadPool.submit(() -> {
            try {
                // Parsing message to extract tasks
                List<String> tasks = Arrays.asList(message.split(","));
                System.out.println("Processing batch of tasks: " + tasks);

                tasks.forEach(task -> {
                    try {
                        System.out.println("Processing task: " + task);
                        try {
                            votingService.getVotingInfo(task);
                            Thread.sleep(100); // Simulate a small delay for processing
                        } catch (VotingSystemException e) {
                            System.err.println("Error getting voting info for task: " + task + " - " + e.getMessage());
                        }
                    } catch (InterruptedException e) {
                        System.err.println("Task interrupted: " + task);
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error processing tasks: " + e.getMessage());
            }
        });
    }
}
