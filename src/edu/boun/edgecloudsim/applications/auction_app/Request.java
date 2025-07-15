package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.utils.WorkflowProperty;

public class Request {
	int id;
	double bid;
	double processingEstimated;
	WorkflowProperty workflow;
	
	public Request(int id, double bid, double processingEstimated, 	WorkflowProperty workflow) {
		this.id = id;
		this.bid = bid;
		this.processingEstimated = processingEstimated;
		this.workflow = workflow;
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

	public WorkflowProperty getTask() {
		return workflow;
	}

	public void setTask(WorkflowProperty workflow) {
		this.workflow = workflow;
	}
}
