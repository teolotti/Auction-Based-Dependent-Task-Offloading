package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;

import java.util.List;

public class WorkflowLoadGenerator extends LoadGeneratorModel {

    public WorkflowLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
    }

    @Override
    public void initializeModel() {
        // Initialize the workflow load generator model here
        // This method should set up the necessary parameters and data structures
        // for generating workflow tasks.
    }

    public List<WorkflowProperty> getWorkflowList() {
        // Return a list of workflows, where each workflow is a list of TaskProperty objects
        // This method should be implemented to return the actual workflows.
        return null; // Placeholder return statement
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return 0;
    }
}
