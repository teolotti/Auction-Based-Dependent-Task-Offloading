package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.applications.auction_app.SampleMobileDeviceManager;
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
    private ArrayList<Integer> initialTaskIds;
    private ArrayList<Integer> finalTaskIds;
    private ArrayList<Integer> completedTasks;
   	private long totalWorkload;
   	private boolean failed = false;




    	public WorkflowProperty(String name, ArrayList<TaskProperty> taskList, int workflowType, double startTime, int[][] dependencyMatrix, double deadline, int mobileDeviceId, long uploadSize, long downloadSize, long totalWorkload) {
        this.name = name;
        this.taskList = taskList;
        this.workflowType = workflowType;
        this.startTime = startTime;
        this.dependencyMatrix = dependencyMatrix;
        this.deadline = deadline;
        this.mobileDeviceId = mobileDeviceId;
        this.uploadSize = uploadSize;
        this.downloadSize = downloadSize;
        computeInitialAndFinalTaskIds();
        this.completedTasks = new ArrayList<>();
        this.totalWorkload = totalWorkload; 
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

    public ArrayList<Integer> getInitialTaskIds() {
        return initialTaskIds;
    }

    public ArrayList<Integer> getFinalTaskIds() {
        return finalTaskIds;
    }
    
    public ArrayList<Integer> getTasksDependingOnTask(int taskAppId){
    	ArrayList<Integer> dependentTasks = new ArrayList<>();
    	for(int i = 0; i < taskList.size(); i++) {
    		if(dependencyMatrix[taskAppId][i] > 0)
    			dependentTasks.add(i);
    	}
    	return dependentTasks;
    }


    private void computeInitialAndFinalTaskIds() {
        initialTaskIds = new ArrayList<>();
        finalTaskIds = new ArrayList<>();

        for (int i = 0; i < taskList.size(); i++) {
            if (SampleMobileDeviceManager.getPredecessors(dependencyMatrix, i).isEmpty()) {
                initialTaskIds.add(i);
            }
            if (SampleMobileDeviceManager.getSuccessors(dependencyMatrix, i).isEmpty()) {
                finalTaskIds.add(i);
            }
        }
    }
	
	public long getTotalWorkload() {
		return totalWorkload;
	}

	public void setTotalWorkload(long totalWorkload) {
		this.totalWorkload = totalWorkload;
	}
	
	public int getPreferredDatacenterForTask(int taskAppId) {
		int preferredIndex = -1;
		for(Map.Entry<Integer, List<Boolean>> entry : personalBooleanMappings.entrySet()) {
			if (entry.getValue().get(taskAppId))
				return entry.getKey();
		}
		return preferredIndex;
	}
	
	public ArrayList<Integer> getUnlockableTasks(int taskAppId){
		ArrayList<Integer> unlockable = new ArrayList<>();
		for(int i = 0; i < taskList.size(); i++) {
			if(dependencyMatrix[taskAppId][i] > 0)
				unlockable.add(i);
		}
		return unlockable;
	}
	
	public boolean removeFinalTaskIndex(int index) {
		if(finalTaskIds.contains(index)) {
			if(finalTaskIds.indexOf(index) != -1)
			finalTaskIds.remove(finalTaskIds.indexOf(index));
			return true;
		}
		return false;
	}
	
	 public ArrayList<Integer> getCompletedTasks() {
			return completedTasks;
		}
	
	public void markCompleted(int taskAppId) {
		completedTasks.add(taskAppId);
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}
}
