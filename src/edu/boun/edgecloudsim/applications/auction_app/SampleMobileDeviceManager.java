/*
 * Title:        EdgeCloudSim - Mobile Device Manager
 * 
 * Description: 
 * Mobile Device Manager is one of the most important component
 * in EdgeCloudSim. It is responsible for creating the tasks,
 * submitting them to the related VM with respect to the
 * Edge Orchestrator decision, and takes proper actions when
 * the execution of the tasks are finished. It also feeds the
 * SimLogger with the relevant results.

 * SampleMobileDeviceManager sends tasks to the edge servers or
 * cloud servers. The mobile devices use WAN if the tasks are
 * offloaded to the edge servers. On the other hand, they use WLAN
 * if the target server is an edge server. Finally, the mobile
 * devices use MAN if they must be served by a remote edge server
 * due to the congestion at their own location. In this case,
 * they access the edge server via two hops where the packets
 * must go through WLAN and MAN.
 * 
 * If you want to use different topology, you should modify
 * the flow implemented in this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.edge_client.*;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.WorkflowProperty;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class SampleMobileDeviceManager extends MobileDeviceManager {
	private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
	
	private static final int UPDATE_MM1_QUEUE_MODEL = BASE + 1;
	private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 2;
	private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 3;
	private static final int REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE = BASE + 4;
	private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR = BASE + 5;
	private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 6;
	private static final int RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE = BASE + 7;

	private static final double MM1_QUEUE_MODEL_UPDATE_INTEVAL = 5; //seconds
	
	private int taskIdCounter=0;
	
	public SampleMobileDeviceManager() throws Exception{
	}

	@Override
	public void initialize() {
	}
	
	@Override
	public UtilizationModel getCpuUtilizationModel() {
		return new CpuUtilizationModel_Custom();
	}
	
	@Override
	public void startEntity() {
		super.startEntity();
		schedule(getId(), SimSettings.CLIENT_ACTIVITY_START_TIME +
				MM1_QUEUE_MODEL_UPDATE_INTEVAL, UPDATE_MM1_QUEUE_MODEL);
	}
	
	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		//do nothing!
	}
	
	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		Task task = (Task) ev.getData();
		
		SimLogger.getInstance().taskExecuted(task.getCloudletId());

		if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
			//SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from cloud");
			double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
			if(WanDelay > 0)
			{
				Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+WanDelay);
				if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
				{
					networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
					SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
					schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
				}
				else
				{
					SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
				}
			}
			else
			{
				SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WAN_DELAY);
			}
		}
		else{
			int nextEvent = RESPONSE_RECEIVED_BY_MOBILE_DEVICE;
			int nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
			NETWORK_DELAY_TYPES delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
			
			EdgeHost host = (EdgeHost)(SimManager.
					getInstance().
					getEdgeServerManager().
					getDatacenterList().get(task.getAssociatedHostId()).
					getHostList().get(0));
			
			//if neighbor edge device is selected
			if(host.getLocation().getServingWlanId() != task.getSubmittedLocation().getServingWlanId())
			{
				delay = networkModel.getDownloadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
				nextEvent = RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE;
				nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID + 1;
				delayType = NETWORK_DELAY_TYPES.MAN_DELAY;
			}
			
			if(delay > 0)
			{
				Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
				if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
				{
					networkModel.downloadStarted(currentLocation, nextDeviceForNetworkModel);
					SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, delayType);
					
					schedule(getId(), delay, nextEvent, task);
				}
				else
				{
					SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
				}
			}
			else
			{
				SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType);
			}
		}
	}
	
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
			System.exit(0);
			return;
		}
		
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		switch (ev.getTag()) {
			case UPDATE_MM1_QUEUE_MODEL:
			{
				((SampleNetworkModel)networkModel).updateMM1QueeuModel();
				schedule(getId(), MM1_QUEUE_MODEL_UPDATE_INTEVAL, UPDATE_MM1_QUEUE_MODEL);
	
				break;
			}
			case REQUEST_RECEIVED_BY_CLOUD:
			{
				Task task = (Task) ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
				break;
			}
			case REQUEST_RECEIVED_BY_EDGE_DEVICE:
			{
				Task task = (Task) ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
				break;
			}
			case REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE:
			{
				Task task = (Task) ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
				submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
				
				break;
			}
			case REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR:
			{
				Task task = (Task) ev.getData();
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				double manDelay =  networkModel.getUploadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
				if(manDelay>0){
					networkModel.uploadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
					SimLogger.getInstance().setUploadDelay(task.getCloudletId(), manDelay, NETWORK_DELAY_TYPES.MAN_DELAY);
					schedule(getId(), manDelay, REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE, task);
				}
				else
				{
					//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
					SimLogger.getInstance().rejectedDueToBandwidth(
							task.getCloudletId(),
							CloudSim.clock(),
							SimSettings.VM_TYPES.EDGE_VM.ordinal(),
							NETWORK_DELAY_TYPES.MAN_DELAY);
				}
				
				break;
			}
			case RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE:
			{
				Task task = (Task) ev.getData();
				networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
				
				//SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from edge");
				double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
				
				if(delay > 0)
				{
					Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
					if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
					{
						networkModel.downloadStarted(currentLocation, SimSettings.GENERIC_EDGE_DEVICE_ID);
						SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, NETWORK_DELAY_TYPES.WLAN_DELAY);
						schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
					}
					else
					{
						SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
					}
				}
				else
				{
					SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
				}
				
				break;
			}
			case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
			{
				Task task = (Task) ev.getData();
				
				if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
					networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				else
					networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
				break;
			}
			default:
				SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
				System.exit(0);
				break;
		}
	}

	public void submitTask(TaskProperty edgeTask) {
		int vmType=0;
		int nextEvent=0;
		int nextDeviceForNetworkModel;
		NETWORK_DELAY_TYPES delayType;
		double delay=0;
		
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		//create a task
		Task task = createTask(edgeTask);
		
		Location currentLocation = SimManager.getInstance().getMobilityModel().
				getLocation(task.getMobileDeviceId(), CloudSim.clock());
		
		//set location of the mobile device which generates this task
		task.setSubmittedLocation(currentLocation);

		//add related task to log list
		SimLogger.getInstance().addLog(task.getMobileDeviceId(),
				task.getCloudletId(),
				task.getTaskType(),
				(int)task.getCloudletLength(),
				(int)task.getCloudletFileSize(),
				(int)task.getCloudletOutputSize());

		int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
		
		if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.CLOUD_DATACENTER_ID, task);
			vmType = SimSettings.VM_TYPES.CLOUD_VM.ordinal();
			nextEvent = REQUEST_RECEIVED_BY_CLOUD;
			delayType = NETWORK_DELAY_TYPES.WAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
		}
		else {
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, task);
			vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
			nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
			delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		
		if(delay>0){
			
			Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
			
			if(selectedVM != null){
				//set related host id
				task.setAssociatedDatacenterId(nextHopId);

				//set related host id
				task.setAssociatedHostId(selectedVM.getHost().getId());
				
				//set related vm id
				task.setAssociatedVmId(selectedVM.getId());
				
				//bind task to related VM
				getCloudletList().add(task);
				bindCloudletToVm(task.getCloudletId(), selectedVM.getId());
				
				if(selectedVM instanceof EdgeVM){
					EdgeHost host = (EdgeHost)(selectedVM.getHost());
					
					//if neighbor edge device is selected
					if(host.getLocation().getServingWlanId() != task.getSubmittedLocation().getServingWlanId()){
						nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR;
					}
				}
				networkModel.uploadStarted(currentLocation, nextDeviceForNetworkModel);
				
				SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
				SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);

				schedule(getId(), delay, nextEvent, task);
			}
			else{
				//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
				SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType);
			}
		}
		else
		{
			//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
			SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType, delayType);
		}
	}
	
	private void submitTaskToVm(Task task, SimSettings.VM_TYPES vmType) {
		//SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
		schedule(getVmsToDatacentersMap().get(task.getVmId()), 0, CloudSimTags.CLOUDLET_SUBMIT, task);

		SimLogger.getInstance().taskAssigned(task.getCloudletId(),
				task.getAssociatedDatacenterId(),
				task.getAssociatedHostId(),
				task.getAssociatedVmId(),
				vmType.ordinal());
	}
	
	private Task createTask(TaskProperty edgeTask){
		UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
		UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

		Task task = new Task(edgeTask.getMobileDeviceId(), ++taskIdCounter,
				edgeTask.getLength(), edgeTask.getPesNumber(),
				edgeTask.getInputFileSize(), edgeTask.getOutputFileSize(),
				utilizationModelCPU, utilizationModel, utilizationModel);
		
		//set the owner of this task
		task.setUserId(this.getId());
		task.setTaskType(edgeTask.getTaskType());
		
		if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
			((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
		}
		
		return task;
	}

	//=============================================
	//Additional methods for auction application
	//=============================================


	@Override
	public void processWorkflow(WorkflowProperty workflowProperty) {
		System.out.println("Processing Workflow");
		// Compute the PCPs (Partial Critical Paths) for the workflow
		computePCPs(workflowProperty);
		// Next step is to create personal mapping for each task in the workflow
		PersonalPCP(workflowProperty);
	}

	private void computePCPs(WorkflowProperty workflowProperty) {
		int [][] taskDependencies = workflowProperty.getDependencyMatrix();
		// Aggiungi due task dummy (ingresso e uscita) in fondo alla matrice, mantenendo gli indici attuali invariati
		int n = taskDependencies.length;
		int[][] extendedMatrix = addDummyTasks(taskDependencies);
		ArrayList<TaskProperty> extendedTaskList = new ArrayList<>(workflowProperty.getTaskList());
		// Aggiungi task dummy all'inizio e alla fine della lista
		extendedTaskList.add(0, new TaskProperty(0, 0, 0, 0, 0, 0, 0)); // Dummy task at the start
		extendedTaskList.add(new TaskProperty(0, 0, 0, 0, 0, 0, 0)); // Dummy task at the end
		TaskPCPutils[] taskPCPutils = new TaskPCPutils[extendedMatrix.length];
		for (int i = extendedMatrix.length; i > 0; i--) {
			double priority = computePriority(taskPCPutils, extendedMatrix, i - 1, extendedTaskList.get(i-1));
			taskPCPutils[i-1] = new TaskPCPutils(i, false);
		}
		// This method should compute the PCPs (Partial Critical Paths) for the workflow
		// based on the tasks and their dependencies.
		SearchPCP(0, extendedMatrix, taskPCPutils, workflowProperty);
	}

	private void PersonalPCP(WorkflowProperty workflowProperty) {
		// This method should create personal mappings for each task in the workflow
		// based on the PCPs computed earlier.
		int numofEdgeHosts = SimSettings.getInstance().getNumOfEdgeHosts();
		double [] readyTimes = new double[numofEdgeHosts];
		//getReadyTimes();
		System.out.println("Creating Personal Mapping for Workflow");
		Map<Integer, List<TaskAssignmentInfo>> personalMappings = new HashMap<>();
		// ArrayList<EdgeStatus> statuses = SimManager.getInstance().getEdgeServerManager().getEdgeDevicesStatus();
		for (int i = 0; i < numofEdgeHosts; i++) {
			readyTimes[i] = 0.0; // Initialize ready times for each edge host, to be inizialized based on the actual status of the edge devices
			personalMappings.get(i).set(0, new TaskAssignmentInfo(0, workflowProperty.getStartTime(), workflowProperty.getStartTime(), readyTimes[i]));
		}

		for (PCP pcp : workflowProperty.getPcpList()) {
			int edgeDeviceId = 0;
			double maxfinishTime = Double.POSITIVE_INFINITY;
			List<TaskAssignmentInfo> bestAssignments = new ArrayList<>();
			for (int i = 0; i < numofEdgeHosts; i++) {
				List<TaskAssignmentInfo> assignments = simulateTaskAssignments(pcp, i, readyTimes, workflowProperty, personalMappings);
				// Get the status of the edge device
				// EdgeStatus status = statuses.get(i);
				// Check if the edge device is available and has enough resources
				// if (status.isAvailable() && status.hasEnoughResources()) {
				// For simplicity, we assume all edge devices are available and have enough resources
				double finishTime = getLastFinishTime(assignments); // This should be calculated based on the task properties
				if (finishTime < maxfinishTime) {
					maxfinishTime = finishTime;
					edgeDeviceId = i;
					bestAssignments = assignments;
				}
			}
		}
	}

	private List<TaskAssignmentInfo> simulateTaskAssignments(PCP pcp, int edgeDeviceId, double[] readyTimes,
															 WorkflowProperty workflowProperty, Map<Integer, List<TaskAssignmentInfo>> personalMappings) {
		List<TaskAssignmentInfo> assignments = new ArrayList<>();
		// Simulate the task assignments for the given PCP and edge device
		for(int i = 0; i < pcp.getTaskIndexes().size(); i++) {
			int taskIndex = pcp.getTaskIndexes().get(i);
			personalMappings.get(edgeDeviceId).set(taskIndex, new TaskAssignmentInfo(taskIndex, 0.0, 0.0, readyTimes[edgeDeviceId]));
			ArrayList<Integer> predecessors = getPredecessors(workflowProperty.getDependencyMatrix(), taskIndex);
			if(predecessors.isEmpty()) {
				//case of the first task to be executed
			} else {
				// calculate finish time plus transmission time for each predecessor
				double finishTime = 0.0;
				for (int predecessor : predecessors) {
					for(int j = 0; j < personalMappings.size(); j++) {
						TaskAssignmentInfo predecessorInfo = personalMappings.get(j).get(predecessor);
						if (predecessorInfo != null) {
							if (j == edgeDeviceId) {
								// If the predecessor is on the same edge device, just use its finish time
								finishTime = Math.max(finishTime, predecessorInfo.getPredictedFinishTime());
							} else {
								// If the predecessor is on a different edge device, add transmission time
								finishTime = Math.max(finishTime, predecessorInfo.getPredictedFinishTime()+
									(double) workflowProperty.getDependencyMatrix()[predecessor][taskIndex] /
											SimSettings.getInstance().getManBandwidth());
							}
						}
					}
				}
			}
		}
		return assignments;
	}

	private double getLastFinishTime(List<TaskAssignmentInfo> assignments) {
		// This method should return the last finish time from the list of task assignments
		double lastFinishTime = 0.0;
		for (TaskAssignmentInfo assignment : assignments) {
			if (assignment.getPredictedFinishTime() > lastFinishTime) {
				lastFinishTime = assignment.getPredictedFinishTime();
			}
		}
		return lastFinishTime;
	}

	private int[][] addDummyTasks(int[][] taskDependencies) {
		int n = taskDependencies.length;
	int[][] extendedMatrix = new int[n + 2][n + 2];

	// Copia la matrice originale nella matrice estesa, partendo da 1
	for (int i = 0; i < n; i++) {
	    for (int j = 0; j < n; j++) {
	        extendedMatrix[i + 1][j + 1] = taskDependencies[i][j];
	    }
	}

	// Task di ingresso: archi uscenti verso tutti i task senza archi entranti
	for (int j = 0; j < n; j++) {
	    boolean hasIncoming = false;
	    for (int i = 0; i < n; i++) {
	        if (taskDependencies[i][j] != 0) {
	            hasIncoming = true;
	            break;
	        }
	    }
	    if (!hasIncoming) {
	        extendedMatrix[0][j + 1] = 1; // peso dummy
	    }
	}

	// Task di uscita: archi entranti da tutti i task senza archi uscenti
	for (int i = 0; i < n; i++) {
	    boolean hasOutgoing = false;
	    for (int j = 0; j < n; j++) {
	        if (taskDependencies[i][j] != 0) {
	            hasOutgoing = true;
	            break;
	        }
	    }
	    if (!hasOutgoing) {
	        extendedMatrix[i + 1][n + 1] = 1; // peso dummy
	    }
	}

	return extendedMatrix;
	}

	public double computePriority(TaskPCPutils[] taskPCPutils, int[][] dependencyMatrix, int taskIndex, TaskProperty taskProperty) {
		// Need a way to get average trasmission rate between edge devices(B) and average processing rate of edge devices(ρ)
		int numOfEdgeHosts = SimSettings.getInstance().getNumOfEdgeHosts();
		int averageTransmissionRate = ((SimSettings.getInstance().getManBandwidth() * binomialCoefficient(numOfEdgeHosts, 2).intValue()) +
				SimSettings.getInstance().getWlanBandwidth()) / (numOfEdgeHosts + 1);
		double averageProcessingRate = SimSettings.getInstance().getMipsForCloudVM();
		double priority = 0.0;
		if (taskIndex == dependencyMatrix.length - 1) {
			// If it's the last task, return a high priority
			return priority;
		} else {
			ArrayList<Integer> successors = getSuccessors(dependencyMatrix, taskIndex);
			for (int successor : successors) {
				TaskPCPutils successorTask = taskPCPutils[successor];
				// Get the max of the successor for this formula: priority + weight in depMatrix / averageTransmissionRate
				priority = Math.max(priority,
						successorTask.getPriority() + dependencyMatrix[taskIndex][successor] / averageTransmissionRate);
			}
		}
		priority += taskProperty.getLength() / averageProcessingRate; // Add the processing time of the current task
		return priority;
	}

	public ArrayList<Integer> getSuccessors(int[][] dependencyMatrix, int taskIndex) {
		ArrayList<Integer> successors = new ArrayList<>();
		for (int j = 0; j < dependencyMatrix[taskIndex].length; j++) {
			if (dependencyMatrix[taskIndex][j] > 0) {
				successors.add(j);
			}
		}
		return successors;
	}

	public ArrayList<Integer> getPredecessors(int[][] dependencyMatrix, int taskIndex) {
		ArrayList<Integer> predecessors = new ArrayList<>();
		for (int i = 0; i < dependencyMatrix.length; i++) {
			if (dependencyMatrix[i][taskIndex] > 0) {
				predecessors.add(i);
			}
		}
		return predecessors;
	}

	public void SearchPCP(int taskIndex, int[][] dependencyMatrix, TaskPCPutils[] taskPCPutils, WorkflowProperty workflowProperty) {
		// This method should implement the search for Partial Critical Paths (PCPs)
		// based on the task dependencies and the computed priorities.
		// It should populate the pcpList with the found PCPs.

		//find successors of the current task that are not marked but all its predecessors are marked

		int selectedSuccessor = selectCandidateSuccessor(dependencyMatrix, taskPCPutils, taskIndex);
		int PCPIndex = -1;
		while (selectedSuccessor != -1) {
			PCPIndex++;
			PCP pcp = new PCP();
			while (selectedSuccessor != -1) {
				pcp.addTask(selectedSuccessor-1); // Adjust for dummy tasks
				taskPCPutils[selectedSuccessor].setMarked(true);
				selectedSuccessor = selectCandidateSuccessor(dependencyMatrix, taskPCPutils, selectedSuccessor);
			}
			// Add the found PCP to the workflow property
			workflowProperty.addPCP(pcp);
			for (int i = 0; i < pcp.getTaskIndexes().size(); i++) {
				SearchPCP(pcp.getTaskIndexes().get(i), dependencyMatrix, taskPCPutils, workflowProperty);
			}
			selectedSuccessor = selectCandidateSuccessor(dependencyMatrix, taskPCPutils, taskIndex);
		}
	}

	public int selectCandidateSuccessor(int[][] dependencies, TaskPCPutils[] taskPCPutils, int taskIndex) {
		ArrayList<Integer> candidateSuccessors = getSuccessors(dependencies, taskIndex);

		boolean candidate = false;
		for (int successor : candidateSuccessors) {
			if (!taskPCPutils[successor].isMarked()) {
				candidate = true;
				for (int i = 0; i < dependencies.length; i++) {
					if (dependencies[i][successor] > 0 && !taskPCPutils[i].isMarked()) {
						candidate = false;
						break;
					}
				}
			}
			if (!candidate) {
				candidateSuccessors.remove(Integer.valueOf(successor));
			}
		}
		//seleziona come candidato il successore con priorità più alta
		int selectedSuccessor = -1;
		if (!candidateSuccessors.isEmpty()) {
			for (int successor : candidateSuccessors) {
				if (selectedSuccessor == -1 || taskPCPutils[successor].getPriority() > taskPCPutils[selectedSuccessor].getPriority()) {
					selectedSuccessor = successor;
				}
			}
		}
		return selectedSuccessor;
	}

	public static BigInteger binomialCoefficient(int n, int k) {
		if (k < 0 || k > n) {
			return BigInteger.ZERO;
		}
		if (k == 0 || k == n) {
			return BigInteger.ONE;
		}
		if (k > n / 2) {
			k = n - k; // Utilizza la simmetria del coefficiente binomiale
		}
		return factorial(n).divide(factorial(k).multiply(factorial(n - k)));
	}

	public static BigInteger factorial(int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = 2; i <= n; i++) {
			fact = fact.multiply(BigInteger.valueOf(i));
		}
		return fact;
	}


}
