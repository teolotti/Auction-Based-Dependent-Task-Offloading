package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.utils.WorkflowProperty;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.UtilizationModel;

import edu.boun.edgecloudsim.utils.TaskProperty;

public abstract class MobileDeviceManager  extends DatacenterBroker {

	public MobileDeviceManager() throws Exception {
		super("Global_Broker");
	}
	
	/*
	 * initialize mobile device manager if needed
	 */
	public abstract void initialize();
	
	/*
	 * provides abstract CPU Utilization Model
	 */
	public abstract UtilizationModel getCpuUtilizationModel();
	
	public abstract void submitTask(TaskProperty edgeTask);

	//====================================
	// Additional methods for Auction App
	//====================================

	public void processWorkflow(WorkflowProperty workflowProperty) {
		// Default implementation does nothing
		// This can be overridden in subclasses if needed
	}
}
