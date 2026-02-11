package ThreadManagement;

import Containerization.ContainerPool;
import DataObjects.Submission;
import DataObjects.TestResult;
import FileManipulation.Compiler;
import FileManipulation.ZipExtractor;
import IPC.ClassBundleWriter;
import IPC.ResultBundleReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadManager {
    private static final ThreadManager INSTANCE;

    static {
        try {
            INSTANCE = new ThreadManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final int numContainers = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private final ContainerPool containerPool = new ContainerPool(numContainers);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Compiler compiler = new Compiler();
    private final BlockingQueue<Submission> submissionQueue = new LinkedBlockingQueue<>(1000);
    private final ConcurrentLinkedQueue<Submission> finishedSubmissions = new ConcurrentLinkedQueue<>();
    private final List<Thread> containerWorkers = new ArrayList<>();
    private final CompletableFuture<Submission[]> future = new CompletableFuture<>();
    private final AtomicInteger inProgress = new AtomicInteger(0);
    private final AtomicBoolean noIncoming = new AtomicBoolean(false);

    public ThreadManager() throws IOException {
        for(int i = 0; i < numContainers; i++){
            ContainerWorker worker = new ContainerWorker(containerPool.requestContainer());
            containerWorkers.add(Thread.ofVirtual().start(worker));
        }
    }

    public void addSubmission(Submission submission){
        if(submission == Submission.NO_INCOMING){
            noIncoming.set(true);
            tryComplete();
            return;
        }
        inProgress.incrementAndGet();
        executor.submit(() -> {
            try {
                ZipExtractor.processSubmission(submission);
                compiler.processSubmission(submission);
                submissionQueue.put(submission);
            } catch (Throwable t) {
                System.err.println("Couldn't process submission: " + submission);
                inProgress.decrementAndGet();
            }
            finally {
                tryComplete();
            }
        });
    }

    private void tryComplete() {
        if(!noIncoming.get() || inProgress.get() != 0) return;
        future.complete(finishedSubmissions.toArray(new Submission[0]));
    }

    public static ThreadManager getInstance(){
        return INSTANCE;
    }

    public void shutdown() throws Exception {
        executor.shutdown();

        for (int i = 0; i < containerWorkers.size(); i++) {
            submissionQueue.add(Submission.SHUTDOWN);
        }
        for (Thread t : containerWorkers) {
            t.join();
        }

        containerPool.close();
    }

    public CompletableFuture<Submission[]> getFuture(){
        return future;
    }

    private class ContainerWorker implements Runnable{
        private final InputStream inStream;
        private final OutputStream outStream;

        private ContainerWorker(String container) throws IOException {
            Process p = new ProcessBuilder(
                    "docker", "exec", "-i", container,
                    "java", "-jar", "/runner/runner-fat.jar"
            ).redirectErrorStream(true).start();

            this.outStream = p.getOutputStream();
            this.inStream = p.getInputStream();
        }


        @Override
        public void run() {
            while(true) {
                try {
                    Submission submission = submissionQueue.poll(200, TimeUnit.MILLISECONDS);
                    if(submission == null) continue;
                    if(submission == Submission.SHUTDOWN) break;
                    ClassBundleWriter.writeBundle(outStream, submission.getClassesDir());
                    TestResult[] results = ResultBundleReader.readBundle(inStream);
                    submission.setResults(results);
                    finishedSubmissions.add(submission);
                    inProgress.decrementAndGet();
                } catch (InterruptedException | IOException e) {
                    inProgress.decrementAndGet();
                    throw new RuntimeException(e);
                }
                finally {
                    tryComplete();
                }
            }
        }
    }
}
