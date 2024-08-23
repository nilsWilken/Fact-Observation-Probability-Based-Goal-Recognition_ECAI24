 package utils;

import java.util.ArrayList;
import java.util.List;

 public class ThreadQueue extends Thread {
	
	private boolean stopRequested;
	private List<Thread> experimentThreadQueue;
	private List<Thread> currentlyRunningThreadList;
	private List<Thread> finishedThreads;
	private int activeThreadCountLimit;
	
	public ThreadQueue(int activeThreadCountLimit) {
		this.stopRequested = false;
		this.experimentThreadQueue = new ArrayList<Thread>();
		this.currentlyRunningThreadList = new ArrayList<Thread>();
		this.finishedThreads = new ArrayList<Thread>();
		this.activeThreadCountLimit = activeThreadCountLimit;
	}
	
	@Override public void run() {
		System.out.println("Thread Queue started!");

		Thread next = null;
		while(!this.stopRequested && this.experimentThreadQueue.size() > 0) {
			while(this.currentlyRunningThreadList.size() < this.activeThreadCountLimit && this.experimentThreadQueue.size() > 0) {
				next = this.getNextThreadInQueue();
				next.start();
				this.currentlyRunningThreadList.add(next);
			}
			try {
				Thread.sleep(100);
				this.updateCurrentlyRunningThreadList();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			while(this.getCurrentlyActiveThreadCount() > 0) {
				this.updateCurrentlyRunningThreadList();
				Thread.sleep(100);
			}
			this.updateCurrentlyRunningThreadList();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Thread Queue finished!");
	}
	
	public void requestStop() {
		this.stopRequested = true;
	}
	
	public void addExperimentThread(Thread thread) {
		this.experimentThreadQueue.add(thread);
	}
	
	public int getCurrentlyActiveThreadCount() {
		return this.currentlyRunningThreadList.size();
	}
	
	public List<Thread> getFinishedThreads() {
		List<Thread> result = new ArrayList<Thread>();
		result.addAll(this.finishedThreads);

		this.finishedThreads.removeAll(result);
		return result;
	}
	
	private Thread getNextThreadInQueue() {
		Thread next = this.experimentThreadQueue.get(0);
		this.experimentThreadQueue.remove(0);
		
		return next;
	}
	
	private void updateCurrentlyRunningThreadList() {
		List<Thread> notActive = new ArrayList<Thread>();
		for(Thread thread : this.currentlyRunningThreadList) {
			if(thread.getState() != State.NEW && !thread.isAlive()) {
				notActive.add(thread);
			}
		}
		
		this.finishedThreads.addAll(notActive);
		this.currentlyRunningThreadList.removeAll(notActive);
	}
 }
