package edu.boun.edgecloudsim.applications.auction_app;

import java.util.ArrayList;
import java.util.List;

import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;

public class AppDependencies {
	private ArrayList<ArrayList<Integer>> dependencies;
	
	public AppDependencies() {
		this.dependencies = new ArrayList<ArrayList<Integer>>();
	}
	
	public void addWorkflowDependencies(WorkflowProperty workflow) {
		ArrayList<TaskProperty> tasks = workflow.getTaskList();
		int[][] dependencyMatrix = workflow.getDependencyMatrix();
		for(int i = 0; i < tasks.size(); i++) {
			ArrayList<Integer> taskDependencies = new ArrayList<Integer>();
			for(int j = 0; j < tasks.size(); j++) {
				if(dependencyMatrix[i][j] > 0 ) {
					taskDependencies.set(j, j);//at position j for easy deletion
				}
			}
			dependencies.set(i, taskDependencies);
		}
	}
	
	public void unlockDependency(int appIndex) {
		dependencies.get(appIndex).remove(appIndex);
	}
	
	public ArrayList<Integer> checkForUnlockedTasks(){
		ArrayList<Integer> unlockedTasks = new ArrayList<Integer>();
		for(int i = 0; i < dependencies.size(); i++) {
			if(dependencies.get(i).isEmpty()) {
				unlockedTasks.add(i);
			}
		}
		return unlockedTasks;
	}
}
