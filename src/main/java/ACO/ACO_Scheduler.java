package ACO;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import utils.Constants;
import utils.DatacenterCreator;
import utils.GenerateMatrices;
import utils.GetResults;

public class ACO_Scheduler {

	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmList;
	private static Datacenter[] datacenter;
	private static double[][] commMatrix;
	private static double[][] execMatrix;

	private static List<Vm> createVM(int userId, int vms) {
		// Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// VM Parameters
		long size = 10000; // image size (MB)
		int ram = 256; // vm memory (MB)
		int mips = 200;
		long bw = 1000;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name

		// create VMs
		Vm[] vm = new Vm[vms];

		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(datacenter[i].getId(), userId, mips, pesNumber, ram, bw, size, vmm,
					new CloudletSchedulerSpaceShared());
			list.add(vm[i]);
		}

		/*
		 * Manually added a VM to check list.add(new Vm(datacenter[vms-1].getId(),
		 * userId, 150, 2, 128, 1000, 11111, "XEN2", new
		 * CloudletSchedulerSpaceShared()));
		 */

		return list;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		// cloudlet parameters
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
			long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel,
					utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			cloudlet[i].setVmId(dcId + 2);
			list.add(cloudlet[i]);
		}
		return list;
	}

	public static void main(String[] args) {
		// Log.printLine("Starting Scheduler...");
		System.out.println("Starting ACO Scheduler...");
		System.out.println();

		new GenerateMatrices();
		execMatrix = GenerateMatrices.getExecMatrix();
		commMatrix = GenerateMatrices.getCommMatrix();

		try {
			int num_user = 1; // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
			for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
				datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
			}

			// Third step: Create Broker
			ACODatacenterBroker broker = createBroker("Broker_0");
			int brokerId = broker.getId();

			// Fourth step: Create VMs and Cloudlets and send them to broker
			vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
			cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			// newList.addAll(globalBroker.getBroker().getCloudletReceivedList());

			CloudSim.stopSimulation();
			printCloudletList(newList);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static ACODatacenterBroker createBroker(String name) throws Exception {
		return new ACODatacenterBroker(name, 37, 1, 2, 1, 2, 0.05);
	}

	/**
	 * Prints the Cloudlet objects
	 *
	 * @param list list of Cloudlets
	 */

	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		double cloudlet_max=0;
		double cloudlet_min=0;
		double time_total=0;
		double DI=0;
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			double cloudlet_time = cloudlet.getFinishTime()-cloudlet.getExecStartTime();
			time_total += cloudlet_time;
			if(cloudlet_time>cloudlet_max) {
				cloudlet_max=cloudlet_time;
			}
			if(cloudlet_time<cloudlet_min) {
				cloudlet_min=cloudlet_time;
			}
		}
		DI = (cloudlet_max -cloudlet_min)/(time_total/size);
		GetResults.listDI.add(DI);
		Log.printLine(indent + "DI =" + DI);
	}

	
	public static Double calcTempMakespan(List<Cloudlet> list) {
		double makespan=0;
		
		for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
			double totalTimeVM=0;
			for (int j = 0; j < Constants.NO_OF_TASKS; j++) {
				if (i == list.get(j).getVmId()) {
					totalTimeVM += execMatrix[j][i] + commMatrix[j][i];
				}
			}
			if (totalTimeVM>makespan) {
				makespan = totalTimeVM;
			}
		}
		return makespan;
	}
}
