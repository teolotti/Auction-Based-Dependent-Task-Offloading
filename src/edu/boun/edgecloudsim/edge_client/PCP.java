package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.utils.TaskProperty;

import java.util.ArrayList;

public class PCP {
    private ArrayList<Integer> taskIndexes;

    public PCP() {
        this.taskIndexes = new ArrayList<>();
    }

    public void addTask(int taskIndex) {
        this.taskIndexes.add(taskIndex);
    }

    public ArrayList<Integer> getTaskIndexes() {
        return taskIndexes;
    }
}
