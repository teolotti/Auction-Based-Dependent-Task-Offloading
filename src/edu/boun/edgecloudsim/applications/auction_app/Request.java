package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.utils.TaskProperty;

public class Request {
	int id;
	double bid;
	double processingEstimated;
	TaskProperty task;
	int preference;
	
	public Request(int id, double bid, double processingEstimated, 	TaskProperty task, int preference) {
		this.id = id;
		this.bid = bid;
		this.processingEstimated = processingEstimated;
		this.task = task;
		this.preference = preference;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getBid() {
		return bid;
	}

	public void setBid(double bid) {
		this.bid = bid;
	}

	public double getProcessingEstimated() {
		return processingEstimated;
	}

	public void setProcessingEstimated(double processingEstimated) {
		this.processingEstimated = processingEstimated;
	}

	public TaskProperty getTask() {
		return task;
	}

	public void setTask(TaskProperty task) {
		this.task = task;
	}	
}
