package ThreadManagement;

import DataObjects.Submission;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Worker extends Thread{
    private volatile BlockingQueue<Submission> submissions;
    private volatile Consumer<Submission> processor;
    public Worker(BlockingQueue<Submission> submissions, Consumer<Submission> processor){
        super();
        this.submissions = submissions;
        this.processor = processor;
    }

    public void setSubmissions(BlockingQueue<Submission> submissions) {
        this.submissions = submissions;
    }

    public BlockingQueue<Submission> getSubmissions() {
        return submissions;
    }

    public void setProcessor(Consumer<Submission> processor) {
        this.processor = processor;
    }

    @Override
    public void run(){
        try {
            while (true) {
                Submission submission = submissions.poll(200, TimeUnit.MILLISECONDS);
                if(submission == null) continue;
                if(submission == Submission.SHUTDOWN) break;
                processor.accept(submission);
            }
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted");
            e.printStackTrace();
            System.exit(16);
        }
    }
}
