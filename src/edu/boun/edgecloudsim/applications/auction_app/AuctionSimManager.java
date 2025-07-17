package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.WorkflowProperty;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.List;

public class AuctionSimManager extends SimManager {
	private static final int CREATE_WORKFLOW = 0; // Define a tag for the event
    private static final int CHECK_ALL_VM = 1;
    private static final int GET_LOAD_LOG = 2;
    private static final int PRINT_PROGRESS = 3;
    private static final int STOP_SIMULATION = 4;

    private String simScenario;
    private String orchestratorPolicy;
    private int numOfMobileDevice;
    private NetworkModel networkModel;
    private MobilityModel mobilityModel;
    private ScenarioFactory scenarioFactory;
    private EdgeOrchestrator edgeOrchestrator;
    private EdgeServerManager edgeServerManager;
    private CloudServerManager cloudServerManager;
    private MobileServerManager mobileServerManager;
    private LoadGeneratorModel loadGeneratorModel;
    private MobileDeviceManager mobileDeviceManager;


    public AuctionSimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
        super(_scenarioFactory, _numOfMobileDevice, _simScenario, _orchestratorPolicy);
        simScenario = _simScenario;
        scenarioFactory = _scenarioFactory;
        numOfMobileDevice = _numOfMobileDevice;
        orchestratorPolicy = _orchestratorPolicy;
        networkModel = super.getNetworkModel();
        mobilityModel = super.getMobilityModel();
        edgeOrchestrator = super.getEdgeOrchestrator();
        edgeServerManager = super.getEdgeServerManager();
        cloudServerManager = super.getCloudServerManager();
        mobileServerManager = super.getMobileServerManager();
        mobileDeviceManager = super.getMobileDeviceManager();
        loadGeneratorModel = super.getLoadGeneratorModel();
    }

    @Override
    public void startEntity() {
        int hostCounter=0;

        for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
            List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
            for (int j=0; j < list.size(); j++) {
                mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
                hostCounter++;
            }
        }

//        for(int i = 0; i< SimSettings.getInstance().getNumOfCloudHost(); i++) {
//            mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
//        }

        for(int i=0; i<numOfMobileDevice; i++){
            if(mobileServerManager.getVmList(i) != null)
                mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
        }

        //Creation of tasks' workflows are scheduled here!
        for(int i=0; i< loadGeneratorModel.getWorkflowList().size(); i++)
            schedule(getId(), loadGeneratorModel.getWorkflowList().get(i).getStartTime(), CREATE_WORKFLOW, loadGeneratorModel.getWorkflowList().get(i));

        //Periodic event loops starts from here!
        schedule(getId(), 5, CHECK_ALL_VM);
        schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);
        schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
        schedule(getId(), SimSettings.getInstance().getSimulationTime(), STOP_SIMULATION);

        SimLogger.printLine("Done.");
    }

    @Override
    public void processEvent(SimEvent ev) {
        if (ev.getTag() == CREATE_WORKFLOW){
            synchronized (this) {
                try {
                    WorkflowProperty workflow = (WorkflowProperty) ev.getData();
                    mobileDeviceManager.processWorkflow(workflow);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        else {
            super.processEvent(ev);
        }
    }
}
