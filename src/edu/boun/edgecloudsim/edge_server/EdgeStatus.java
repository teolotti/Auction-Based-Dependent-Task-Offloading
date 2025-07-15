package edu.boun.edgecloudsim.edge_server;

public class EdgeStatus {
	double utilization;
	double mips;
	double waitingTime;
	double unitCost;
	
	
	
	public EdgeStatus(double utilization, double mips, double waitingTime, double unitCost) {
		super();
		this.utilization = utilization;
		this.mips = mips;
		this.waitingTime = waitingTime;
		this.unitCost = unitCost;
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
	
	public double getUnitCost() {
		return unitCost;
	}
	public void setUnitCost(double unitCost) {
		this.unitCost = unitCost;
	}	
}
