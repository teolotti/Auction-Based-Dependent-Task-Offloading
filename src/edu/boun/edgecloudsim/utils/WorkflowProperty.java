package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.edge_client.PCP;
import edu.boun.edgecloudsim.edge_client.TaskAssignmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    Map<Integer, List<TaskAssignmentInfo>> personalMappings;
    Map<Integer, List<Boolean>> personalBooleanMappings;
    private double PredictedMakespan;




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

    public String getName() {
        return name;
    }

    public double getDeadline() {
        return deadline;
    }

    public int getMobileDeviceId() {
        return mobileDeviceId;
    }

    public long getUploadSize() {
        return uploadSize;
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public Map<Integer, List<TaskAssignmentInfo>> getPersonalMappings() {
        return personalMappings;
    }

    public void setPersonalMappings(Map<Integer, List<TaskAssignmentInfo>> personalMappings) {
        this.personalMappings = personalMappings;
    }

    public Map<Integer, List<Boolean>> getPersonalBooleanMappings() {
        return personalBooleanMappings;
    }

    public void setPersonalBooleanMappings(Map<Integer, List<Boolean>> personalBooleanMappings) {
        this.personalBooleanMappings = personalBooleanMappings;
    }

    public double getPredictedMakespan() {
        return PredictedMakespan;
    }

    public void setPredictedMakespan(double predictedMakespan) {
        PredictedMakespan = predictedMakespan;
    }
}
