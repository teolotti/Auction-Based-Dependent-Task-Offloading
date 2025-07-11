package edu.boun.edgecloudsim.edge_server;

public class EdgeStatus {
	double utilization;
	double mips;
	double waitingTime;
	
	
	
	public EdgeStatus(double utilization, double mips, double waitingTime) {
		super();
		this.utilization = utilization;
		this.mips = mips;
		this.waitingTime = waitingTime;
	}
	public double getUtilization() {
		return utilization;
	}
	public void setUtilization(double utilization) {
		this.utilization = utilization;
	}
	public double getMips() {
		return mips;
	}
	public void setMips(double mips) {
		this.mips = mips;
	}
	public double getWaitingTime() {
		return waitingTime;
	}
	public void setWaitingTime(double waitingTime) {
		this.waitingTime = waitingTime;
	}
	
}
