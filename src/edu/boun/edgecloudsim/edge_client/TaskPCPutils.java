package edu.boun.edgecloudsim.edge_client;

public class TaskPCPutils{
    private double priority;
    private boolean marked;

    public TaskPCPutils(double priority, boolean marked) {
        this.priority = priority;
        this.marked = marked;
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
