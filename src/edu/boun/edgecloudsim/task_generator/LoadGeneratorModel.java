/*
 * Title:        EdgeCloudSim - Load Generator Model
 * 
 * Description: 
 * LoadGeneratorModel is an abstract class which is used for 
 * deciding task generation pattern via a task list. For those who
 * wants to add a custom Load Generator Model to EdgeCloudSim should
 * extend this class and provide a concrete instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.task_generator;

import java.util.List;

import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;

public abstract class LoadGeneratorModel {
	protected List<TaskProperty> taskList;
	protected int numberOfMobileDevices;
	protected double simulationTime;
	protected String simScenario;

	protected List<WorkflowProperty> workflowList; // This can be used if workflows are supported in the model
	
	public LoadGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario){
		numberOfMobileDevices=_numberOfMobileDevices;
		simulationTime=_simulationTime;
		simScenario=_simScenario;
	};
	
	/*
	 * Default Constructor: Creates an empty LoadGeneratorModel
	 */
	public LoadGeneratorModel() {
	}

	/*
	 * each task has a virtual start time
	 * it will be used while generating task
	 */
	public List<TaskProperty> getTaskList() {
		return taskList;
	}

	public List<WorkflowProperty> getWorkflowList() {
		return null; // This method can be overridden if needed
	}

	/*
	 * fill task list according to related task generation model
	 */
	public abstract void initializeModel();
	
	/*
	 * returns the task type (index) that the mobile device uses
	 */
	public abstract int getTaskTypeOfDevice(int deviceId);
}
