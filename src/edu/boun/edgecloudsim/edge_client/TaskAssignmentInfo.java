package edu.boun.edgecloudsim.edge_client;

public class TaskAssignmentInfo {
    int taskIndex;
    double predictedStartTime;
    double predictedFinishTime;
    double EdgeAvailableTime;
    boolean isAssigned = false;

    public TaskAssignmentInfo(){};

    public TaskAssignmentInfo(int taskIndex, double predictedStartTime, double predictedFinishTime, double edgeAvailableTime) {
        this.taskIndex = taskIndex;
        this.predictedStartTime = predictedStartTime;
        this.predictedFinishTime = predictedFinishTime;
        this.EdgeAvailableTime = edgeAvailableTime;
    }
    public TaskAssignmentInfo(int taskIndex, double predictedStartTime, double predictedFinishTime, double edgeAvailableTime, boolean isAssigned) {
        this.taskIndex = taskIndex;
        this.predictedStartTime = predictedStartTime;
        this.predictedFinishTime = predictedFinishTime;
        this.EdgeAvailableTime = edgeAvailableTime;
        this.isAssigned = isAssigned;
    }
    public int getTaskIndex() {
        return taskIndex;
    }
    public void setTaskIndex(int taskIndex) {
        this.taskIndex = taskIndex;
    }
    public double getPredictedStartTime() {
        return predictedStartTime;
    }
    public void setPredictedStartTime(double predictedStartTime) {
        this.predictedStartTime = predictedStartTime;
    }
    public double getPredictedFinishTime() {
        return predictedFinishTime;
    }
    public void setPredictedFinishTime(double predictedFinishTime) {
        this.predictedFinishTime = predictedFinishTime;
    }

    public double getEdgeAvailableTime() {
        return EdgeAvailableTime;
    }

    public void setEdgeAvailableTime(double edgeAvailableTime) {
        EdgeAvailableTime = edgeAvailableTime;
    }

    public boolean isAssigned() {
        return isAssigned;
    }

    public void setAssigned(boolean assigned) {
        isAssigned = assigned;
    }

}

