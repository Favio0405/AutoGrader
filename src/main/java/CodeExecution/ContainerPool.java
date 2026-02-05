package CodeExecution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ContainerPool implements AutoCloseable{
    private final BlockingQueue<String> available = new LinkedBlockingQueue<>();
    private final List<String> containers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> failedRemoval = new ArrayList<>();

    public ContainerPool(String image, int numContainers)  {
        for(int i = 0; i < numContainers; i++){
            String name = "autograder-container" + java.util.UUID.randomUUID();
            try {
                ContainerCLI.createAndStart(name, image);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            containers.add(name);
            available.add(name);
        }
    }

    public String requestContainer() {
        return available.poll();
    }

    @Override
    public void close() {
        safeClose();
        if(!failedRemoval.isEmpty()){
            System.out.println("Failed to close containers: ");
            for (String s : failedRemoval){
                System.out.println(s);
            }
        }
    }

    private void safeClose(){
        synchronized (containers) {
            for (String s : containers) {
                try {
                    ContainerCLI.remove(s);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    failedRemoval.add(s);
                }
            }
        }
    }
}
