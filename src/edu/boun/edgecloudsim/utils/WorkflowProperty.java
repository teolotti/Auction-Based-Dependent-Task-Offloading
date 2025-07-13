package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.edge_client.PCP;

import java.util.List;

public class WorkflowProperty {

    private String name;
    private List<TaskProperty> taskList;
    private int[][] dependencyMatrix;
    private int workflowType;
    private double startTime;
    private double deadlineFactor;
    private List<PCP> pcpList;


    public WorkflowProperty(String name, List<TaskProperty> taskList, int workflowType, double startTime, int[][] dependencyMatrix, double deadlineFactor) {
        this.name = name;
        this.taskList = taskList;
        this.workflowType = workflowType;
        this.startTime = startTime;
        this.dependencyMatrix = dependencyMatrix;
        this.deadlineFactor = deadlineFactor;
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
