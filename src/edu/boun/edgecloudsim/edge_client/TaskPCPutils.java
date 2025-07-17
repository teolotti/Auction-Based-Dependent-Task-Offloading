package edu.boun.edgecloudsim.edge_client;

public class TaskPCPutils{
    private double priority;
    private boolean marked;
    private int taskId;

    public TaskPCPutils(double priority, boolean marked, int taskId) {
        this.priority = priority;
        this.marked = marked;
        this.taskId = taskId;
    }

    public double getPriority() {
        return priority;
    }
    public void setPriority(double priority) {
        this.priority = priority;
    }
    public boolean isMarked() {
        return marked;
    }
    public void setMarked(boolean marked) {
        this.marked = marked;
    }
}
