package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.List;

public class WorkflowLoadGenerator extends LoadGeneratorModel {
    int[] workflowTypeOfDevices;
    public WorkflowLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
    }

    @Override
    public void initializeModel() {
        // Initialize the workflow load generator model here
        // This method should set up the necessary parameters and data structures
        // for generating workflow tasks.
        workflowList = new ArrayList<WorkflowProperty>();
        // Initialize the exponential distribution as an array of Lists of ExponentialDistribution
        ArrayList<ExponentialDistribution[]>[] expRngList = new ArrayList[SimSettings.getInstance().getWorkflows().length];
        for (int i = 0; i < expRngList.length; i++) {
            expRngList[i] = new ArrayList<>();
            for(int k = 0; k < SimSettings.getInstance().getWorkflows()[i].getTasks().length; k++){
                ExponentialDistribution[] distributions = new ExponentialDistribution[3];
                for (int j = 0; j < 3; j++) {
                    distributions[j] = new ExponentialDistribution(SimSettings.getInstance().getWorkflows()[i].getTasks()[k].getTaskNodeProperties()[j]); // Sostituisci 1.0 con il parametro desiderato
                }
                expRngList[i].add(distributions);
            }
        }
        // Valuta se servono distribuzioni esponenziali per input/output file size e task length oppure no


        // Each mobile device utilizes an app type (workflow type)
        workflowTypeOfDevices = new int[numberOfMobileDevices];
        for (int i = 0; i < numberOfMobileDevices; i++) {
            int randomWorkflowType = -1;
            double workflowTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
            double workflowTypePercentage = 0;
            for (int j = 0; j < SimSettings.getInstance().getWorkflows().length; j++) {
                workflowTypePercentage += SimSettings.getInstance().getWorkflows()[j].getWorkflowProperties()[0];
                if (workflowTypeSelector <= workflowTypePercentage) {
                    randomWorkflowType = j;
                    break;
                }
            }
            if (randomWorkflowType == -1) {
                SimLogger.printLine("Impossible is occurred! no random workflow type!");
                continue;
            }

            workflowTypeOfDevices[i] = randomWorkflowType;


            // Generate tasks for the selected workflow type
            double poissonMean = SimSettings.getInstance().getWorkflows()[randomWorkflowType].getWorkflowProperties()[2];
            double deadline_factor = SimSettings.getInstance().getWorkflows()[randomWorkflowType].getWorkflowProperties()[7];
            double dataUploadSize = SimSettings.getInstance().getWorkflows()[randomWorkflowType].getWorkflowProperties()[8];
            double dataDownloadSize = SimSettings.getInstance().getWorkflows()[randomWorkflowType].getWorkflowProperties()[9];
            double activePeriodStartTime = SimSettings.CLIENT_ACTIVITY_START_TIME;
            double virtualTime = activePeriodStartTime;

            ExponentialDistribution rng = new ExponentialDistribution(poissonMean);

            double interArrivalTime = rng.sample();

            if (interArrivalTime < 0) {
                SimLogger.printLine("Impossible is occurred! Inter-arrival time is " + interArrivalTime + " for device " + i + " at time " + virtualTime);
                continue;
            }
            virtualTime += interArrivalTime;
            double totalLength = 0.0;
            for (int k = 0; k < SimSettings.getInstance().getWorkflows()[randomWorkflowType].getTasks().length; k++) {
                totalLength += SimSettings.getInstance().getWorkflows()[randomWorkflowType].getTasks()[k].getTaskNodeProperties()[2]; // Assuming the length is at index 2
            }
            double basicMakespan = totalLength / SimSettings.getInstance().getMipsForEdgeVM();
            double deadline = virtualTime + basicMakespan * deadline_factor;

            long totalWorkload = 0;
            ArrayList<TaskProperty> taskList = new ArrayList<>();
            for (int k = 0; k < SimSettings.getInstance().getWorkflows()[randomWorkflowType].getTasks().length; k++) {
                // Create tasks for the workflow
                TaskProperty taskProperty = new TaskProperty(
                        virtualTime,
                        i,
                        randomWorkflowType,
                        1, // Assuming 1 PEs for simplicity, can be adjusted
                        (long) expRngList[randomWorkflowType].get(k)[0].sample(), // Length
                        (long) expRngList[randomWorkflowType].get(k)[1].sample(), // Input file size
                        (long) expRngList[randomWorkflowType].get(k)[2].sample(),  // Output file size
                        k
                );
                taskList.add(taskProperty);
                totalWorkload += taskProperty.getLength();
            }
            workflowList.add(new WorkflowProperty(
                    SimSettings.getInstance().getWorkflows()[randomWorkflowType].getName(),
                    taskList,
                    randomWorkflowType,
                    virtualTime,
                    SimSettings.getInstance().getWorkflows()[randomWorkflowType].getDependencies(),
                    deadline,
                    i,
                    (long)dataUploadSize,
                    (long)dataDownloadSize,
                    totalWorkload
            ));
        }
    }

    public List<WorkflowProperty> getWorkflowList() {
        // Return a list of workflows, where each workflow is a list of TaskProperty objects
        return workflowList;
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return workflowTypeOfDevices[deviceId];
    }
}
