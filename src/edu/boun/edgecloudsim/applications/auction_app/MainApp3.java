package edu.boun.edgecloudsim.applications.auction_app;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

public class MainApp3 {
	public static void main(String[] args) {
		//disable console output of cloudsim library
				Log.disable();

				//enable console output and file output of this application
				SimLogger.enablePrintLog();

				int iterationNumber = 1;
				String configFile = "";
				String outputFolder = "";
				String edgeDevicesFile = "";
				String applicationsFile = "";
				if (args.length == 5){
					configFile = args[0];
					edgeDevicesFile = args[1];
					applicationsFile = args[2];
					outputFolder = args[3];
					iterationNumber = Integer.parseInt(args[4]);
				}
				else{
					SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
					configFile = "scripts/auction_app/config/default_config.properties";
					applicationsFile = "scripts/auction_app/config/applications.xml";
					edgeDevicesFile = "scripts/auction_app/config/edge_devices.xml";
					outputFolder = "sim_results/ite" + iterationNumber;
				}

				//load settings from configuration file and initialize simulation settings, using graph initialization files
				SimSettings SS = SimSettings.getInstance();
				if(SS.initializeGraph(configFile, edgeDevicesFile, applicationsFile) == false){
					SimLogger.printLine("cannot initialize simulation settings!");
					System.exit(0);
				}

				if(SS.getFileLoggingEnabled()){
					SimLogger.enableFileLog();
					SimUtils.cleanOutputFolder(outputFolder);
				}

				DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				Date SimulationStartDate = Calendar.getInstance().getTime();
				String now = df.format(SimulationStartDate);
				SimLogger.printLine("Simulation started at " + now);
				SimLogger.printLine("----------------------------------------------------------------------");
				
				int numMobileDevices = 50;
				int numRepetitions = 1;
				int lambda = 20;
				String filePath = "results/";
				SS.setThirdExp(true);
				
				for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
				{
					for(int k=0; k<1; k++)//scenario (single tier is sufficient here)
					{
						for(int i=0; i<1; i++)//iterating on heuristics
						{
							ArrayList<Double> valuations = new ArrayList<>();
							ArrayList<Double> payments = new ArrayList<>();
							SS.setLAMBDA(lambda);
							for(int l=0; l<numRepetitions; l++) {//averaging iterations for variance reduction
								String simScenario = SS.getSimulationScenarios()[k];
								String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
								Date ScenarioStartDate = Calendar.getInstance().getTime();
								now = df.format(ScenarioStartDate);

								SimLogger.printLine("Scenario started at " + now);
								SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
								SimLogger.printLine("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + j);
								SimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");

								try
								{
									// First step: Initialize the CloudSim package. It should be called
									// before creating any entities.
									int num_user = 2;   // number of grid users
									Calendar calendar = Calendar.getInstance();
									boolean trace_flag = false;  // mean trace events

									// Initialize the CloudSim library
									CloudSim.init(num_user, calendar, trace_flag, 0.01);

									// Generate EdgeCloudsim Scenario Factory
									ScenarioFactory sampleFactory = new SampleScenarioFactory(numMobileDevices,SS.getSimulationTime(), orchestratorPolicy, simScenario);

									// Generate EdgeCloudSim Simulation Manager, using our AuctionSimManager
									SimManager manager = new AuctionSimManager(sampleFactory, numMobileDevices, simScenario, orchestratorPolicy);

									// Start simulation
									manager.startSimulation();
								}
								catch (Exception e)
								{
									SimLogger.printLine("The simulation has been terminated due to an unexpected error");
									e.printStackTrace();
									System.exit(0);
								}

								Date ScenarioEndDate = Calendar.getInstance().getTime();
								now = df.format(ScenarioEndDate);
								SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
								SimLogger.printLine("----------------------------------------------------------------------");
								SampleMobileDeviceManagerTruthfulness deviceManager = (SampleMobileDeviceManagerTruthfulness)SimManager.getInstance().getMobileDeviceManager();
								Map<Integer, Double> iterationValuations = deviceManager.getValuations();
								double utility = 0;
								for(Map.Entry<Integer, Double> valuation : iterationValuations.entrySet()) {
									valuations.add(valuation.getValue());
									utility += valuation.getValue();
								}
								Map<Integer, Double> iterationPayments = deviceManager.getPayments();
								for(Map.Entry<Integer, Double> payment : iterationPayments.entrySet()) {
									payments.add(payment.getValue());
									utility -= payment.getValue();
								}
								
							}
							String jsonName = "truthfulness_valuation_results.json";
							Gson gson = new GsonBuilder()
				                    .setPrettyPrinting()
				                    .serializeNulls()  // include nulls if you want
				                    .create();
							try (Writer writer = Files.newBufferedWriter(Paths.get(filePath + jsonName))) {
						        gson.toJson(gson.toJsonTree(valuations), writer);
						    } catch (IOException e) {
								e.printStackTrace();
							}
							jsonName = "truthfulness_payment_results.json";
							try (Writer writer = Files.newBufferedWriter(Paths.get(filePath + jsonName))) {
						        gson.toJson(gson.toJsonTree(payments), writer);
						    } catch (IOException e) {
								e.printStackTrace();
							}
						}//End of orchestrators loop
					}//End of scenarios loop
				}//End of mobile devices loop

				Date SimulationEndDate = Calendar.getInstance().getTime();
				now = df.format(SimulationEndDate);
				SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));	
	}
}
