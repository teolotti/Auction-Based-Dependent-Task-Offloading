package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.edge_client.PCP;

import java.util.List;

public class WorkflowProperty {

    private List<TaskProperty> taskList;
    private int[][] dependencyMatrix;
    private int workflowType;
    private double startTime;
    private List<PCP> pcpList;


    public WorkflowProperty() {
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public List<TaskProperty> getTaskList() {
        return taskList;
    }

    public void addTask(List<TaskProperty> taskList) {
        this.taskList = taskList;
    }

    public void addPCP(List<PCP> pcpList) {
        this.pcpList = pcpList;
    }

    public List<PCP> getPcpList() {
        return pcpList;
    }

    public int getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(int workflowType) {
        this.workflowType = workflowType;
    }

    public int[][] getDependencyMatrix() {
        return dependencyMatrix;
    }

    public void setDependencyMatrix(int[][] dependencyMatrix) {
        this.dependencyMatrix = dependencyMatrix;
    }

}
