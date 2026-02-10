package ThreadManagement;

import DataObjects.Submission;
import IPC.ClassBundleWriter;
import IPC.ResultBundleReader;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class Worker extends Thread{
    private volatile BlockingQueue<Submission> submissions;
    private volatile BiConsumer<Submission, Worker> processor;
    private volatile String container;
    private volatile OutputStream outStream;

    private volatile InputStream inStream;

    public Worker(BlockingQueue<Submission> submissions, BiConsumer<Submission, Worker> processor){
        super();
        this.submissions = submissions;
        this.processor = processor;
    }

    public void setSubmissions(BlockingQueue<Submission> submissions) {
        this.submissions = submissions;
    }

    public void setProcessor(BiConsumer<Submission, Worker> processor) {
        this.processor = processor;
    }

    public void assignContainer(String container) throws IOException {
        Process p = new ProcessBuilder(
                "docker", "exec", "-i", container,
                "java", "-jar", "/runner/runner-fat.jar"
        ).start();

        this.container = container;
        outStream = p.getOutputStream();
        inStream = p.getInputStream();
    }

    public OutputStream getOutStream() {
        return outStream;
    }

    public InputStream getInStream() {
        return inStream;
    }

    @Override
    public void run(){
        try {
            while (true) {
                Submission submission = submissions.poll(200, TimeUnit.MILLISECONDS);
                if(submission == null) continue;
                if(submission == Submission.SHUTDOWN) {
                    ClassBundleWriter.sendTerminationSignal(outStream);
                    if(ResultBundleReader.readBundle(inStream) != null) {
                        throw new RuntimeException("Could not terminate container runner");
                    }
                    break;
                }
                processor.accept(submission, this);
            }
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted");
            e.printStackTrace();
            System.exit(16);
        } catch (IOException e) {
            throw new RuntimeException("Could not terminate container runner");
        }
    }
}
