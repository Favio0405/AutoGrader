package ThreadManagement;

import CodeExecution.CodeExecutor;
import FileManipulation.Compiler;
import FileManipulation.ZipExtractor;
import TestObjects.Submission;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker extends Thread{
    private final BlockingQueue<Submission> tasks;
    public Worker(){
        super();
        tasks = new LinkedBlockingQueue<>();
    }

    public void addTask(Submission task){
        tasks.add(task);
    }

    //IN PROGRESS
    @Override
    public void run(){

    }
}
