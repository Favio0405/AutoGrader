package ThreadManagement;

import DataObjects.TestResult;
import IPC.ClassBundleWriter;
import Containerization.ContainerPool;
import FileManipulation.Compiler;
import FileManipulation.ZipExtractor;
import DataObjects.Submission;
import IPC.ResultBundleReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class  ThreadManager {
    private static final ThreadManager INSTANCE = new ThreadManager();
    private final BlockingQueue<Submission> unzipQueue;
    private final BlockingQueue<Submission> compileQueue;
    private final BlockingQueue<Submission> executionQueue;
    private final List<Worker> unzipWorkers;
    private final List<Worker> compileWorkers;
    private final List<Worker> executionWorkers;
    private final BiConsumer<Submission, Worker> unzip;
    private final BiConsumer<Submission, Worker> compile;
    private final BiConsumer<Submission, Worker> execute;
    private final AtomicInteger unzipsInProgress;
    private final AtomicInteger compilesInProgress;
    private final AtomicInteger executionsInProgress;
    private volatile boolean noMoreIncoming;
    private volatile boolean unzipDone;
    private volatile boolean compileDone;
    private final Object transferLock;
    private final CompletableFuture<Submission[]> future;
    private final ConcurrentLinkedQueue<Submission> completedSubmissions;
    private final ContainerPool containerPool;
    private ThreadManager(){
        unzipQueue = new LinkedBlockingQueue<>();
        compileQueue = new LinkedBlockingQueue<>();
        executionQueue = new LinkedBlockingQueue<>();
        unzipWorkers = new ArrayList<>();
        compileWorkers = new ArrayList<>();
        executionWorkers = new ArrayList<>();
        unzipsInProgress = new AtomicInteger(0);
        compilesInProgress = new AtomicInteger(0);
        executionsInProgress = new AtomicInteger(0);
        noMoreIncoming = false;
        unzipDone = false;
        compileDone = false;
        transferLock = new Object();
        future = new CompletableFuture<>();
        completedSubmissions = new ConcurrentLinkedQueue<>();

        Compiler compiler = new Compiler();

        unzip = (submission, worker) -> {
            unzipsInProgress.incrementAndGet();
            try {
                ZipExtractor.processSubmission(submission);
                compileQueue.add(submission);
            }
            finally {
                unzipsInProgress.decrementAndGet();
            }
            if(noMoreIncoming && unzipQueue.isEmpty() && unzipsInProgress.get() == 0)
                transferUnzipWorkers();
        };
        compile = (submission, worker) -> {
            compilesInProgress.incrementAndGet();
            try {
                compiler.processSubmission(submission);
                executionQueue.add(submission);
            }
            finally {
                compilesInProgress.decrementAndGet();
            }
            if(unzipDone && compileQueue.isEmpty() && compilesInProgress.get() == 0)
                transferCompileWorkers();
        };
        execute = (submission, worker) -> {
            executionsInProgress.incrementAndGet();
            try {
                ClassBundleWriter.writeBundle(worker.getOutStream(), submission.getClassesDir());
                TestResult[] results = ResultBundleReader.readBundle(worker.getInStream());
                submission.setResults(results);
                completedSubmissions.add(submission);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't write bundle to container");
            } finally {
                executionsInProgress.decrementAndGet();
            }
            if(compileDone && executionQueue.isEmpty() && executionsInProgress.get() == 0)
                future.complete(completedSubmissions.toArray(new Submission[0]));
        };
        int[] threadSizes = allocateThreads(0.2, 0.4, 0.4);
        containerPool = new ContainerPool(threadSizes[0] + threadSizes[1] + threadSizes[2]);
        initializeResources(threadSizes);
    }

    public int[] allocateThreads(double pctUnzip, double pctCompile, double pctExec) {
        int total = Runtime.getRuntime().availableProcessors();
        // initial allocations, rounded:
        int unzip   = Math.max(1, (int) Math.round(total * pctUnzip));
        int compile = Math.max(1, (int) Math.round(total * pctCompile));
        int exec    = Math.max(1, (int) Math.round(total * pctExec));

        // adjust so sum == total
        int sum = unzip + compile + exec;
        int diff = total - sum;

        // under‑allocated, give the remainder to compile
        if (diff > 0) {
            compile += diff;
        }
        // over‑allocated, subtract from exec
        else if (diff < 0) {
            exec += diff;
            if (exec < 1) {
                // ensure exec stays ≥ 1
                compile += exec - 1;
                exec = 1;
            }
        }

        return new int[]{ unzip, compile, exec };
    }
    private void initializeResources(int[] sizes){
        for(int i = 0; i < sizes[0]; i++){
            Worker worker = new Worker(unzipQueue, unzip);
            unzipWorkers.add(worker);
            worker.start();
        }
        for(int i = 0; i < sizes[1]; i++){
            Worker worker = new Worker(compileQueue, compile);
            compileWorkers.add(worker);
            worker.start();
        }
        for(int i = 0; i < sizes[2]; i++){
            Worker worker = new Worker(executionQueue, execute);
            try {
                worker.assignContainer(containerPool.requestContainer());
            } catch (IOException e) {
                throw new RuntimeException("Cannot assign container to worker");
            }
            executionWorkers.add(worker);
            worker.start();
        }
    }
    public static ThreadManager getInstance(){
        return INSTANCE;
    }

    public void addSubmission(Submission submission){
        if(submission == Submission.NO_INCOMING) noMoreIncoming = true;
        else unzipQueue.add(submission);
    }

    private void transferUnzipWorkers(){
        synchronized (transferLock) {
            for (Worker worker : unzipWorkers) {
                worker.setSubmissions(compileQueue);
                worker.setProcessor(compile);
            }
            compileWorkers.addAll(unzipWorkers);
            unzipWorkers.clear();
            unzipDone = true;
        }
    }

    private void transferCompileWorkers(){
        synchronized (transferLock) {
            for (Worker worker : compileWorkers) {
                try {
                    worker.assignContainer(containerPool.requestContainer());
                } catch (IOException e) {
                    throw new RuntimeException("Cannot assign container to worker");
                }
                worker.setSubmissions(executionQueue);
                worker.setProcessor(execute);
            }
            executionWorkers.addAll(compileWorkers);
            compileWorkers.clear();
            compileDone = true;
        }
    }

    public void shutdown(){
        for (Worker ignored : executionWorkers){
            executionQueue.add(Submission.SHUTDOWN);
        }
        for (Worker worker : executionWorkers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for worker to finish.");
                e.printStackTrace();
            }
        }
        containerPool.close();
    }

    public CompletableFuture<Submission[]> getFuture() {
        return future;
    }
}
