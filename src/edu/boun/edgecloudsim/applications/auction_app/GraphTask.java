package edu.boun.edgecloudsim.applications.auction_app;


import edu.boun.edgecloudsim.edge_client.Task;
import java.util.ArrayList;

public class GraphTask {
	private int numNodes;
	private Task[] tasks;
	private int[][] adjacencyMatrix;
	
	public GraphTask(int numNodes, Task[] tasks, int[][] adjacencyMatrix) {
		this.numNodes = numNodes;
		this.tasks = tasks;
		this.adjacencyMatrix = adjacencyMatrix;
		tasks[numNodes-1].setLast(true);//ensures we know the end of one app when cloudlet is done processing on edge
	}

	public int getNumNodes() {
		return numNodes;
	}

	public Task getTask(int index) {
		return tasks[index];
	}
	
	public ArrayList<Integer> getTaskDependencies(int index) {//dependencies are all tasks that point to index (i.e. all ones on column index) in the DAG
		ArrayList<Integer> dependencies = new ArrayList<Integer>();
		for (int j=0; j<numNodes; j++)
			if (adjacencyMatrix[j][index] == 1)
				dependencies.add(j);//we need the tasks, so  we store the row index
		return dependencies;
	}

}
