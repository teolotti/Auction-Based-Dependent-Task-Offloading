package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.utils.TaskProperty;

import java.util.ArrayList;


public class GraphTaskProperty {
	private int numNodes;
	private TaskProperty[] taskProperties;
	private int[][] adjacencyMatrix;

	public GraphTaskProperty(int numNodes, TaskProperty[] properties, int[][] adjacencyMatrix) {
		this.numNodes = numNodes;
		this.taskProperties = properties;
		this.adjacencyMatrix = adjacencyMatrix;
	}

	public TaskProperty getTaskProperty(int index) {
		return taskProperties[index];
	}
	
	public ArrayList<Integer> getDependencies(int index){//dependencies are all tasks that point to index (i.e. all ones on column index) in the DAG
		ArrayList<Integer> dependencies = new ArrayList<Integer>();
		for (int j=0; j<numNodes; j++)
			if (adjacencyMatrix[j][index] == 1)
				dependencies.add(j);//we need the tasks, so  we store the row index
		return dependencies;
	}
}