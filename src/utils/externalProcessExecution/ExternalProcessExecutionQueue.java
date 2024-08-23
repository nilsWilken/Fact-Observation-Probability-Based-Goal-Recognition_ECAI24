package utils.externalProcessExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;


public class ExternalProcessExecutionQueue extends Thread {

	private int maxThreads;
	
	private Queue<ProcessExecutionHandler> executionQueue;
	private List<ProcessExecutionHandler> currentlyActiveThreads;
	private boolean requestedToStop;
		
	protected ExternalProcessExecutionQueue(int maxThreads) {
		this.maxThreads = maxThreads;
		
		this.executionQueue = new ArrayBlockingQueue<ProcessExecutionHandler>(1000);
		this.currentlyActiveThreads = new ArrayList<ProcessExecutionHandler>();
		
		this.requestedToStop = false;
	}
	
	@Override
	public void run() {
		while(!requestedToStop) {
			this.updateActiveThreadList();
			this.updateWaitingQueue();
		}
	}
	
	public void requestStop() {
		this.requestedToStop = true;
	}
	
	public void addProcessExecution(ProcessExecutionHandler handler) {
		this.executionQueue.add(handler);
	}
	
	private void updateActiveThreadList() {
		List<ProcessExecutionHandler> notActive = new ArrayList<ProcessExecutionHandler>();
		
		for(ProcessExecutionHandler active : this.currentlyActiveThreads) {
			if(!active.isAlive()) {
				notActive.add(active);
			}
		}
		
		this.currentlyActiveThreads.removeAll(notActive);
	}
	
	private void updateWaitingQueue() {
		ProcessExecutionHandler handler;
		while(this.currentlyActiveThreads.size() < this.maxThreads && this.executionQueue.size() > 0) {
			handler = this.executionQueue.poll();
			this.currentlyActiveThreads.add(handler);
			handler.start();
		}
	}
	
}
