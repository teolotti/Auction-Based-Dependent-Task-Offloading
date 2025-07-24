/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Sample App2
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.auction_app;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainApp {

	/**
	 * Creates main() to run this example
	 */
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
		
		int numMobileDevices = 300;
		int numRepetitions = 30;
		double baseLambda = 5;
		int lambdaIncrements = 12;
		String filePath = "results/";
		Map<Integer, String> heuristicDict = new HashMap<>();
		heuristicDict.put(0, "au-pcp");
		heuristicDict.put(1, "nearest");
		heuristicDict.put(2, "selfish");
		
		for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
		{
			for(int k=0; k<1; k++)//scenario (single tier is sufficient here)
			{
				for(int i=0; i<3; i++)//iterating on heuristics
				{
					ArrayList<Double> makespans = new ArrayList<>();
					ArrayList<Double> valuations = new ArrayList<>();
					ArrayList<Double> successRates = new ArrayList<>();
					for(int m=1; m<=lambdaIncrements; m++) {//loop for lambda parameter change
						double successRate = 0;
						double avgMakespan = 0;
						double totalValuation = 0;
						SS.setLAMBDA(baseLambda * m);
						for(int l=0; l<numRepetitions; l++) {//averaging iterations for variance reduction
							//TODO change parameters initializations
							//TODO change lambda for load generation
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
							SampleMobileDeviceManager deviceManager = (SampleMobileDeviceManager)SimManager.getInstance().getMobileDeviceManager();
							ArrayList<Double> iterationMakespans = deviceManager.getMakespans();
							Map<Integer, Double> iterationValuations = deviceManager.getValuations();
							Map<Integer, Double> payments = deviceManager.getValuations();
							successRate += deviceManager.getSuccessPercentage();
							double avgIterationMakespan = 0;
							for(Double makespan : iterationMakespans) {
								avgIterationMakespan += makespan;
							}
							avgIterationMakespan /= iterationMakespans.size();
							avgMakespan += avgIterationMakespan;
							double totalIterationValuation = 0;
							for(Map.Entry<Integer, Double> entry : iterationValuations.entrySet()) {
								totalIterationValuation += entry.getValue();
							}
							totalValuation += totalIterationValuation;
						}
						successRate /= numRepetitions;
						avgMakespan /= numRepetitions;
						if(Double.isNaN(avgMakespan))
							avgMakespan = makespans.get(m-2);
						totalValuation /= numRepetitions;
						makespans.add(avgMakespan);
						valuations.add(totalValuation);
						successRates.add(successRate);
					}
					System.out.println("Risultati:" + i);
					System.out.println("Valuations: ");
					for(int l=0; l<valuations.size(); l++) {
						System.out.printf("%f, ", valuations.get(l));
					}
					System.out.printf("\n");
					System.out.println("Makespans: ");
					for(int l=0; l<makespans.size(); l++) {
						System.out.printf("%f, ", makespans.get(l));
					}
					System.out.printf("\n");
					System.out.println("Success rates: ");
					for(int l=0; l<successRates.size(); l++) {
						System.out.printf("%f, ", successRates.get(l));
					}
					String jsonName = "valuation_results_" + heuristicDict.get(i) + ".json";
					Gson gson = new GsonBuilder()
		                    .setPrettyPrinting()
		                    .serializeNulls()  // include nulls if you want
		                    .create();
					try (Writer writer = Files.newBufferedWriter(Paths.get(filePath + jsonName))) {
				        gson.toJson(gson.toJsonTree(valuations), writer);
				    } catch (IOException e) {
						e.printStackTrace();
					}
					jsonName = "makespan_results_" + heuristicDict.get(i) + ".json";
					try (Writer writer = Files.newBufferedWriter(Paths.get(filePath + jsonName))) {
				        gson.toJson(gson.toJsonTree(makespans), writer);
				    } catch (IOException e) {
						e.printStackTrace();
					}
					jsonName = "success_results_" + heuristicDict.get(i) + ".json";
					try (Writer writer = Files.newBufferedWriter(Paths.get(filePath + jsonName))) {
				        gson.toJson(gson.toJsonTree(successRates), writer);
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
