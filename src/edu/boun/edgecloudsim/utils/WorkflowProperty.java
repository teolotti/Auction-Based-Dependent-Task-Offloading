package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.edge_client.PCP;

import java.util.ArrayList;

public class WorkflowProperty {

    private String name;
    private ArrayList<TaskProperty> taskList;
    private int[][] dependencyMatrix;
    private int workflowType;
    private double startTime;
    private double deadline;
    private ArrayList<PCP> pcpList;
    private int mobileDeviceId;
    private long uploadSize, downloadSize;



    public WorkflowProperty(String name, ArrayList<TaskProperty> taskList, int workflowType, double startTime, int[][] dependencyMatrix, double deadline, int mobileDeviceId, long uploadSize, long downloadSize) {
        this.name = name;
        this.taskList = taskList;
        this.workflowType = workflowType;
        this.startTime = startTime;
        this.dependencyMatrix = dependencyMatrix;
        this.deadline = deadline;
        this.mobileDeviceId = mobileDeviceId;
        this.uploadSize = uploadSize;
        this.downloadSize = downloadSize;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public ArrayList<TaskProperty> getTaskList() {
        return taskList;
    }

    public void addTask(ArrayList<TaskProperty> taskList) {
        this.taskList = taskList;
    }

    public void addPCP(PCP pcp) {
        if (this.pcpList == null) {
            this.pcpList = new ArrayList<>();
        }
        this.pcpList.add(pcp);
    }

    public ArrayList<PCP> getPcpList() {
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
