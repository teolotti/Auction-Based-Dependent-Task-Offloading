package edu.boun.edgecloudsim.applications.auction_app;

import cern.jet.random.Exponential;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.List;

public class WorkflowLoadGenerator extends LoadGeneratorModel {
    int taskTypeOfDevices[];
    public WorkflowLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
    }

    @Override
    public void initializeModel() {
        // Initialize the workflow load generator model here
        // This method should set up the necessary parameters and data structures
        // for generating workflow tasks.
        workflowList = new ArrayList<WorkflowProperty>();

        // Example: Populate the workflowList with WorkflowProperty objects
        taskTypeOfDevices = new int[numberOfMobileDevices];

        ExponentialDistribution[] expRngWorkflow = new ExponentialDistribution[SimSettings.getInstance().getWorkflows().length];
        ArrayList<ExponentialDistribution[][]> expRngTaskList = new ArrayList<>();
        //for per inizializzazione distribuzioni, poi for annidati per workflow e task properties

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
