package edu.boun.edgecloudsim.applications.auction_app;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;

public class AppDependencies {
	private Map<Integer, ArrayList<Integer>> dependencies;
	
	public AppDependencies() {
		this.dependencies = new HashMap<Integer, ArrayList<Integer>>();
	}
	
	public AppDependencies(AppDependencies appDependencies) {
	    this.dependencies = new HashMap<>();

	    for (Map.Entry<Integer, ArrayList<Integer>> entry
	            : appDependencies.getDependencies().entrySet()) {
	        this.dependencies.put(entry.getKey(),
	                              new ArrayList<>(entry.getValue()));
	    }
	}

	public Map<Integer, ArrayList<Integer>> getDependencies() {
		return dependencies;
	}
	
	public void addWorkflowDependencies(WorkflowProperty workflow) {
		ArrayList<TaskProperty> tasks = workflow.getTaskList();
		int[][] dependencyMatrix = workflow.getDependencyMatrix();
		for(int i = 0; i < tasks.size(); i++) {
			ArrayList<Integer> taskDependencies = new ArrayList<Integer>();
			for(int j = 0; j < tasks.size(); j++) {
				if(dependencyMatrix[j][i] > 0 ) {
					taskDependencies.add(j);
				}
			}
			dependencies.put(i, taskDependencies);
		}
	}
	
	public void unlockDependency(int appIndex, int idToRemove) {
	    List<Integer> deps = dependencies.get(appIndex);
	    if (deps != null) {
	        deps.removeIf(task -> task.equals(idToRemove));
	    }
	}
	
	public void removeTask(int taskAppId) {
		dependencies.remove(taskAppId);
	}
	
	public ArrayList<Integer> checkForUnlockedTasks(){
		ArrayList<Integer> unlockedTasks = new ArrayList<Integer>();
		for(Map.Entry<Integer, ArrayList<Integer>> entry : dependencies.entrySet()) {
			if(entry.getValue().isEmpty()) {
				unlockedTasks.add(entry.getKey());
			}
		}
		return unlockedTasks;
	}
}
