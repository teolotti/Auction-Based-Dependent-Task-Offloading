package edu.boun.edgecloudsim.utils;

import java.util.List;

public class WorkflowProperty {

    private List<TaskProperty> taskList;
    private int workflowType;
    private double startTime;


    public WorkflowProperty() {
    }

    public double getStartTime() {
        return startTime;
    }

}
