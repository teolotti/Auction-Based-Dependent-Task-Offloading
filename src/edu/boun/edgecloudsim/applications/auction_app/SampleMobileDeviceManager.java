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
import edu.boun.edgecloudsim.edge_server.EdgeStatus;
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
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import java.math.BigInteger;
import java.util.*;

public class SampleMobileDeviceManager extends MobileDeviceManager {
	private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
	
	private static final int UPDATE_MM1_QUEUE_MODEL = BASE + 1;
	private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 2;
	private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 3;
	private static final int REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE = BASE + 4;
	private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR = BASE + 5;
	private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 6;
	private static final int RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE = BASE + 7;
	private static final int SEND_RESULTS_TO_UNLOCKABLE_TASKS = BASE + 8;
	private static final int UNLOCK_TASKS = BASE + 9;


	private static final double MM1_QUEUE_MODEL_UPDATE_INTEVAL = 5; //seconds
	
	private static final int ENQUEUE_REQ     = BASE+10;
	private static final int RUN_AUCTION     = BASE+11;
	private static final int AUCTION_RESULT  = BASE+12;

	private  Deque<Request> reqQueue = new ArrayDeque<>();
	private  int auctionTodoCounter = 0;
	private boolean auctionRunning = false;

	private int taskIdCounter=0;

	private ArrayList<WorkflowProperty> workflowList = new ArrayList<>();
	private ArrayList<AppDependencies> appDependenciesList = new ArrayList<>();

	
	
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
			EdgeVM vm = (EdgeVM) ((SimManager.
					getInstance().
					getEdgeServerManager().
					getDatacenterList().get(task.getAssociatedHostId()).
					getHostList().get(0)).getVmList().get(0));
			if(vm.getCloudletScheduler().getCloudletExecList().isEmpty())//if edge device queue is empty, fill it
				schedule(getId(), 0.0, RUN_AUCTION);
			//if last task, pop it from the index list for this workflow, else unlock dependencies and submit tasks
			WorkflowProperty workflow = workflowList.get(task.getMobileDeviceId());
			ArrayList<Integer> finalTaskIds = workflow.getFinalTaskIds();
			if(workflow.removeFinalTaskIndex(task.getTaskAppId())) {
				schedule(getId(), 0.0, SEND_RESULTS_TO_UNLOCKABLE_TASKS, task);
			}
		
			//if no last task left, conclude, send to the mobile device and schedule an auction
			if(finalTaskIds.isEmpty()) {
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
				schedule(getId(), 0.0, RUN_AUCTION);//run auction when an app is completed
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
				//if task is unlocked, submit it
				ArrayList<Integer> unlockedTasks = appDependenciesList.get(task.getMobileDeviceId()).checkForUnlockedTasks();
				if(unlockedTasks.contains(task.getTaskAppId()))
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
			case ENQUEUE_REQ:
			{
				WorkflowProperty workflow = (WorkflowProperty) ev.getData();
				
				Task dummyTask = new Task (workflow.getMobileDeviceId(), -1, 0, 1, workflow.getUploadSize(), workflow.getDownloadSize(), null, null, null);
				
				//need to handle two-hop situations
				double uploadCost = networkModel.getUploadDelay(workflow.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, dummyTask);
				double downloadCost = networkModel.getDownloadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, workflow.getMobileDeviceId(), dummyTask);

				double processingEstimated = workflow.getPredictedMakespan() + uploadCost + downloadCost;
				if(CloudSim.clock() + processingEstimated > workflow.getDeadline())//handle failure of workflow here
					break;
				
				double maxUnitCost = SimManager.getInstance().getEdgeServerManager().getMaxCost();
				double minMips = SimManager.getInstance().getEdgeServerManager().getMinMips();
				double bid = decideBid(workflow.getTotalWorkload(), maxUnitCost, minMips);
				
				Request request = new Request(workflow.getMobileDeviceId(), bid, processingEstimated, workflow);

				reqQueue.add(request);
				if(!auctionRunning)
					schedule(getId(), 0.001, RUN_AUCTION);
				else
					auctionTodoCounter++;
				break;
			}
			case RUN_AUCTION:
			{
				if(reqQueue.size() <= 0)
					break;
				auctionRunning = true;
				AuctionResult winnerData = ((SampleEdgeOrchestrator)SimManager.getInstance().getEdgeOrchestrator()).auction(reqQueue);
				Request winner = null;
				double auctionTime = (double) reqQueue.size() * Math.log((double) reqQueue.size()) * 0.0001;
				for(Request request : reqQueue) {
					if(winnerData.getWinnerId() == request.getId()) //FIXME: check this if clause
						winner = request;
						reqQueue.remove(request);
				}
				//Payment needs to be saved (data file with all the stats seems best solution here)
				
				if(winner != null)
					schedule(getId(), auctionTime, AUCTION_RESULT, winner);
				else
					SimLogger.printLine("Winner not found");
				break;
			}
			case AUCTION_RESULT:
			{
				if(auctionTodoCounter > 0) {
					auctionTodoCounter--;
					schedule(getId(), 0.0, RUN_AUCTION);
				}
				auctionRunning = false;
				Request winner = (Request) ev.getData();
				WorkflowProperty workflow = winner.getWorkflow();
				AppDependencies dependencyTracker = new AppDependencies();
				dependencyTracker.addWorkflowDependencies(workflow);
				ArrayList<Integer> firsts = workflowList.get(workflow.getMobileDeviceId()).getInitialTaskIds();
				//submit the first tasks using an indexed list
				ArrayList<TaskProperty> tasks = workflow.getTaskList();
				
				for(int i = 0; i < firsts.size(); i++) {
					int preference = workflow.getPreferredDatacenterForTask(i);
					submitTask(tasks.get(i), preference, tasks.get(i).getTaskAppId());
				}
				break;
			}//when submitting tasks, delays are 0 for same edge device tasks, > 0 for different edges
			case SEND_RESULTS_TO_UNLOCKABLE_TASKS:
			{
				Task task = (Task) ev.getData();
				AppDependencies appDependencies = appDependenciesList.get(task.getMobileDeviceId());
				ArrayList<Integer> unlockableTaskIds = appDependencies.getUnlockableTasks(task.getTaskAppId());
				WorkflowProperty workflow = workflowList.get(task.getMobileDeviceId());
				ArrayList<TaskProperty> tasks = workflow.getTaskList();
				
				for(int i = 0; i < unlockableTaskIds.size(); i++) {
					int preference = workflow.getPreferredDatacenterForTask(i);
					int index = unlockableTaskIds.get(i);
					submitTaskEdgeToEdge(tasks.get(index), preference, task);
				}
				break;
			}
			case UNLOCK_TASKS:
			{
				Task task = (Task) ev.getData();
				AppDependencies appDependencies = appDependenciesList.get(task.getMobileDeviceId());
				appDependencies.unlockDependency(task.getTaskAppId());
				break;
			}
			default:
				SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
				System.exit(0);
				break;
		}
	}

	public double decideBid(long workflowLength, double maxCost, double minMips) {
		double estimatedCost = (maxCost * workflowLength) / minMips;
		UniformRealDistribution distro = new UniformRealDistribution(estimatedCost, 5 * estimatedCost);
		double bid = distro.sample();
		return bid;
	}

	@Override
	public void setupMobileDeviceArrival(WorkflowProperty edgeWorkflow) {//subscribes to the net, marks request arrival, does not need delay
		schedule(getId(), 0.0, ENQUEUE_REQ, edgeWorkflow);
	}

	public void submitTask(TaskProperty edgeTask, int preference, int taskAppId) {
		int vmType=0;
		int nextEvent=0;
		int nextDeviceForNetworkModel;
		NETWORK_DELAY_TYPES delayType;
		double delay=0;
		
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		//create a task
		Task task = createTask(edgeTask, taskAppId);
		
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

		int nextHopId = preference;
		
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
	
	public void submitTaskEdgeToEdge(TaskProperty edgeTask, int preference, Task completedTask) {
		int vmType=0;
		int nextEvent=0;
		int nextDeviceForNetworkModel;
		NETWORK_DELAY_TYPES delayType;
		double delay=0;
		
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		//create a task
		Task task = createTask(edgeTask, edgeTask.getTaskAppId());
		
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

		int nextHopId = preference;
		if(nextHopId == completedTask.getAssociatedDatacenterId()) {
			delay = 0;
			vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
			nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
			delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		else {
			delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, task);
			vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
			nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
			delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
			nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
		}
		
		double theoreticalDelay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, task);
		
		if(theoreticalDelay>0){//TODO find a way to incorporate capacity rejection
			
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
				}
				networkModel.uploadStarted(currentLocation, nextDeviceForNetworkModel);
				
				SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
				SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
				
				schedule(getId(), delay, UNLOCK_TASKS, completedTask);//first unlock dependencies
				schedule(getId(), delay, nextEvent, task);//then check if submittable to vm
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
	
	private Task createTask(TaskProperty edgeTask, int taskAppId){
		UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
		UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

		Task task = new Task(edgeTask.getMobileDeviceId(), ++taskIdCounter,
				edgeTask.getLength(), edgeTask.getPesNumber(),
				edgeTask.getInputFileSize(), edgeTask.getOutputFileSize(),
				utilizationModelCPU, utilizationModel, utilizationModel);
		
		//set the owner of this task
		task.setUserId(this.getId());
		task.setTaskType(edgeTask.getTaskType());
		task.setTaskAppId(taskAppId);
		
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
		Map<Integer, List<TaskAssignmentInfo>> personalMappings = PersonalPCP(workflowProperty);
		Map<Integer, List<Boolean>> booleanMap = buildBooleanMap(personalMappings);
		workflowProperty.setPersonalMappings(personalMappings);
		workflowProperty.setPersonalBooleanMappings(booleanMap);
		// Set the predicted makespan for the workflow
		double predictedMakespan = computePredictedMakespan(personalMappings);
		workflowProperty.setPredictedMakespan(predictedMakespan);
		// Add the workflow to the workflow list
		workflowList.set(workflowProperty.getMobileDeviceId(), workflowProperty);
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

	private Map<Integer, List<TaskAssignmentInfo>> PersonalPCP(WorkflowProperty workflowProperty) {
		// This method should create personal mappings for each task in the workflow
		// based on the PCPs computed earlier.
		int numofEdgeHosts = SimSettings.getInstance().getNumOfEdgeHosts();
		double [] readyTimes = new double[numofEdgeHosts];
		System.out.println("Creating Personal Mapping for Workflow");
		Map<Integer, List<TaskAssignmentInfo>> personalMappings = new HashMap<>();
		for (int i = 0; i < numofEdgeHosts; i++) {
			personalMappings.put(i, new ArrayList<>());
			for (int j = 0; j < workflowProperty.getTaskList().size(); j++) {//this ensures every list is as large as it can be
				personalMappings.get(i).add(new TaskAssignmentInfo(j, 0.0, 0.0, 0.0));
			}
		}
		ArrayList<EdgeStatus> statuses = SimManager.getInstance().getEdgeServerManager().getEdgeDevicesStatus();
		for (int i = 0; i < numofEdgeHosts; i++) {
			readyTimes[i] = statuses.get(i).getWaitingTime();
		}

		for (PCP pcp : workflowProperty.getPcpList()) {
			int edgeDeviceId = 0;
			double bestfinishTime = Double.POSITIVE_INFINITY;
			List<TaskAssignmentInfo> bestAssignments = new ArrayList<>();
			for (int i = 0; i < numofEdgeHosts; i++) {
				double currentReady = readyTimes[edgeDeviceId];
				List<TaskAssignmentInfo> assignments = simulateTaskAssignments(pcp, i, currentReady, workflowProperty, personalMappings, statuses);
				// Get the status of the edge device
				// EdgeStatus status = statuses.get(i);
				// Check if the edge device is available and has enough resources
				// if (status.isAvailable() && status.hasEnoughResources()) {
				// For simplicity, we assume all edge devices are available and have enough resources
				double finishTime = getLastFinishTime(assignments); // This should be calculated based on the task properties
				if (finishTime < bestfinishTime) {
					bestfinishTime = finishTime;
					edgeDeviceId = i;
					bestAssignments = assignments;
				}
			}
			// Update the personal mappings with the best assignments
			for (TaskAssignmentInfo assignment : bestAssignments) {
				personalMappings.get(edgeDeviceId).set(assignment.getTaskIndex(), assignment);
			}
			// Update the ready time for the edge device
			readyTimes[edgeDeviceId] = bestfinishTime;
		}
		return personalMappings;
	}

	private List<TaskAssignmentInfo> simulateTaskAssignments(PCP pcp, int edgeDeviceId, double currentReadyTime,
															 WorkflowProperty workflowProperty, Map<Integer, List<TaskAssignmentInfo>> personalMappings, ArrayList<EdgeStatus> statuses) {
		List<TaskAssignmentInfo> assignments = new ArrayList<>();
		// Simulate the task assignments for the given PCP and edge device
		for(int taskIndex : pcp.getTaskIndexes()) {
			personalMappings.get(edgeDeviceId).set(taskIndex, new TaskAssignmentInfo(taskIndex, 0.0, 0.0, currentReadyTime));
			ArrayList<Integer> predecessors = getPredecessors(workflowProperty.getDependencyMatrix(), taskIndex);
			double dataReadyTime = 0.0;
			if(predecessors.isEmpty()) {
				double transmissionTime = (double) workflowProperty.getUploadSize() / SimSettings.getInstance().getWlanBandwidth();
				dataReadyTime = workflowProperty.getStartTime() + transmissionTime;
			} else {
				// calculate finish time plus transmission time for each predecessor
				for (int predecessor : predecessors) {
					for(int j = 0; j < personalMappings.size(); j++) {
						TaskAssignmentInfo predecessorInfo = personalMappings.get(j).get(predecessor);
						if (predecessorInfo != null) {
							if (j == edgeDeviceId) {
								// If the predecessor is on the same edge device, just use its finish time
								dataReadyTime = Math.max(dataReadyTime, predecessorInfo.getPredictedFinishTime()); //predicted finish time del pred è start time più computazione
							} else {
								// If the predecessor is on a different edge device, add transmission time
								dataReadyTime = Math.max(dataReadyTime, predecessorInfo.getPredictedFinishTime()+
									(double) workflowProperty.getDependencyMatrix()[predecessor][taskIndex] /
											SimSettings.getInstance().getManBandwidth());
							}
						}
					}
				}
			}
			double predStartTime = Math.max(dataReadyTime, currentReadyTime);
			double predFinishTime = predStartTime + (double) workflowProperty.getTaskList().get(taskIndex).getLength() /
					statuses.get(edgeDeviceId).getMips();
			// Update the ready time for the edge device
			currentReadyTime = predFinishTime;
			TaskAssignmentInfo assignmentInfo = new TaskAssignmentInfo(taskIndex, predStartTime, predFinishTime, currentReadyTime, true);
			assignments.add(assignmentInfo);
		}
		return assignments;
	}

	private Map<Integer, List<Boolean>> buildBooleanMap(Map<Integer, List<TaskAssignmentInfo>> personalMappings) {
		Map<Integer, List<Boolean>> booleanMap = new HashMap<>();
		for (Map.Entry<Integer, List<TaskAssignmentInfo>> entry : personalMappings.entrySet()) {
			List<TaskAssignmentInfo> assignments = entry.getValue();
			for (TaskAssignmentInfo assignment : assignments) {
				// Check if the task is assigned to an edge device
				if (assignment.isAssigned()) {
					booleanMap.get(entry.getKey()).set(assignment.getTaskIndex(), true);
				} else {
					booleanMap.get(entry.getKey()).set(assignment.getTaskIndex(), false);
				}
			}
		}
		return booleanMap;
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

	private double computePredictedMakespan(Map<Integer, List<TaskAssignmentInfo>> personalMappings) {
		// Calcola il makespan come differenza tra il tempo di fine massimo e il tempo di inizio minimo
		double minStart = Double.POSITIVE_INFINITY;
		double maxFinish = 0.0;
		for (List<TaskAssignmentInfo> assignments : personalMappings.values()) {
		    for (TaskAssignmentInfo assignment : assignments) {
		        if (assignment.getPredictedStartTime() < minStart) {
		            minStart = assignment.getPredictedStartTime();
		        }
		        if (assignment.getPredictedFinishTime() > maxFinish) {
		            maxFinish = assignment.getPredictedFinishTime();
		        }
		    }
		}
		return maxFinish - minStart;
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

	public static ArrayList<Integer> getSuccessors(int[][] dependencyMatrix, int taskIndex) {
		ArrayList<Integer> successors = new ArrayList<>();
		for (int j = 0; j < dependencyMatrix[taskIndex].length; j++) {
			if (dependencyMatrix[taskIndex][j] > 0) {
				successors.add(j);
			}
		}
		return successors;
	}

	public static ArrayList<Integer> getPredecessors(int[][] dependencyMatrix, int taskIndex) {
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


    public ArrayList<WorkflowProperty> getWorkflowList() {
        return workflowList;
    }

    public void setWorkflowList(ArrayList<WorkflowProperty> workflowList) {
        this.workflowList = workflowList;
    }
}
