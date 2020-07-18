package ACO;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import utils.Constants;
import utils.GenerateMatrices;
import utils.GetResults;

public class ACO {
	
	protected double Q;
	protected double alpha;
	protected double beta;
	protected double gamma;
	protected double rho;
	protected int m;
	protected Random r;
	protected Map<Integer, Integer> bestAllocation = new HashMap<>();
	protected Map<Integer, Integer> allocatedtasks = new HashMap<>();
	protected Map<Integer, Map<Integer, Double>> executionCosts = new HashMap<>();
	double[][] execTimeMatrix = GenerateMatrices.getExecMatrix();
	double[][] commTimeMatrix = GenerateMatrices.getCommMatrix();
	double bestMakespan=10000000;
	//protected Double BestMakespan=0.0;
	//execosts = {tasks,(VM,time)}

	public Map<Integer, Integer> implement(List<Cloudlet> taskList, List<Vm> vmList, int tmax)
			throws FileNotFoundException {
		int tasks = taskList.size();
		int vms = vmList.size();
		Map<Integer, Map<Integer, Double>> execTimes = new HashMap<>();;
		Map<Integer, Double> cc = new HashMap<>();  //compuational capacity
		Map<Integer, Double> pheromones;	
		System.out.println();

		//for each vm get execution time for the task
		for (int i = 0; i < tasks; i++) {
			Map<Integer, Double> x = new HashMap<>();
			for (int j = 0; j < vms; j++) {
				double t = getExecutionTime(vmList.get(j), taskList.get(i));
				x.put(j, t);
				}
				execTimes.put(i, x);
			}
		executionCosts.putAll(execTimes);
		
		//exectimes stores time to execute the task(key), vm(key2), executiontime(value)

		for (int i = 0; i < vms; i++) {
			Vm vm = vmList.get(i);
			double Cc = vm.getNumberOfPes() * vm.getMips() + vm.getBw();
			cc.put(i, Cc);
		}

		pheromones = initializePheromone(cc);

		for (int t = 1; t <= tmax; t++) {
			Map<Integer, Double> eet = new HashMap<>();

			for (int i = 0; i < vms; i++)
				eet.put(i, 0.0);

			for (int task = 0; task < tasks; task++) {

				Map<Integer, Double> probab = new HashMap<>();
				Map<Integer, Double> eetTemp = new HashMap<>();
				Map<Integer, Double> lbfValues = new HashMap<>();
				for (int i = 0; i < vms; i++)  
					eetTemp.put(i, eet.get(i) + execTimes.get(task).get(i));
					
				double total = 0;
				for (int i = 0; i < vms; i++) { //summed task execution time on all vms.
					total += eetTemp.get(i);
				}
				for (int i = 0; i < vms; i++) {
					//double value = total/eetTemp.get(i); //loadbalancing value
					lbfValues.put(i, total/eetTemp.get(i));   
				}

				total = 0;
				for (int i = 0; i < vms; i++) {
					double p = Math.pow(pheromones.get(i), alpha) * Math.pow(cc.get(i), beta)
							* Math.pow(lbfValues.get(i), gamma);

					probab.put(i, p);
					total += p;
				}
				for (int i = 0; i < vms; i++) {
					probab.put(i, probab.get(i) / total);  //calculating probability 
				}

				int[] votes = new int[vms];
				//get votes for which vm to choose for the task.
				for (int k = 0; k < m; k++) {
					double max = 0;

					int vmIndexChosen = vote(vms, probab);
					votes[vmIndexChosen]++;
				}

				int max_votes = 0;
				int opt_vm = 0;
				for (int i = 0; i < vms; i++) {
					if (max_votes < votes[i]) {
						max_votes = votes[i];
						allocatedtasks.put(task, i);
						opt_vm = i;
					}
				}
				
				eet.put(opt_vm, eet.get(opt_vm) + execTimes.get(task).get(opt_vm));
				pheromones.put(opt_vm, (1 - rho) * (pheromones.get(opt_vm))  + (Q / execTimes.get(task).get(opt_vm)));
			}
			double tempmakespan =calcTempMakespan();
			GetResults.listvalues.add(tempmakespan);
			
			if(bestMakespan>tempmakespan) {
				System.out.println("Best update from " + (int)bestMakespan+ " to: "+(int)tempmakespan);
				bestMakespan =tempmakespan;
				bestAllocation.putAll(allocatedtasks);
			}
			

		}
		System.out.println();
		System.out.println(printAllocation(bestAllocation));
		System.out.println("Best Makespan Time is " + bestMakespan );
		System.out.println();
		return bestAllocation;
	}

	public Double calcTempMakespan() {
		double makespan=0;
		for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
			double totalTimeVM=0;
			for (int j = 0; j < Constants.NO_OF_TASKS; j++) {
				if (i == allocatedtasks.get(j)) {
					totalTimeVM += execTimeMatrix[j][i] + commTimeMatrix[j][i];
				}
			}
			if (totalTimeVM>makespan) {
				makespan = totalTimeVM;
			}
		}
		return makespan;
	}

	public String printAllocation(Map<Integer,Integer> allocated) {
		String output = "";
		for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
			double totalTimeVM=0;
			String tasks = "";
			int no_of_tasks = 0;
			for (int j = 0; j < Constants.NO_OF_TASKS; j++) {
				if (i == allocated.get(j)) {
					tasks += (tasks.isEmpty() ? "" : " ") + j;
					no_of_tasks++;
					totalTimeVM += execTimeMatrix[j][i] + commTimeMatrix[j][i];
				}
			}
			if (tasks.isEmpty())
				output += "There is no tasks associated to Data Center " + i + "\n";
			else
				output += "There are " + no_of_tasks + " tasks associated to VM ID " + (i+2) + " and they are "
						+ (tasks) + " with total run time for VM " + totalTimeVM + "\n";
		}
		return output;
	}

	protected int vote(int vms, Map<Integer, Double> probab) {
		int[] freq = new int[vms];
		int sum = 0;

		for (int i = 0; i < vms; i++) {
			freq[i] = (int) (probab.get(i) * 100000.0);
			sum += freq[i];
		}

		int n = 1 + r.nextInt(sum);
		if (n <= freq[0]) {
			return 0;
		}

		for (int i = 0; i < vms - 1; i++) {
			freq[i + 1] += freq[i];
			if (n > freq[i] && n <= freq[i + 1]) {
				return i + 1;
			}
		}
		return 0;
	}

	public ACO(int m, double Q, double alpha, double beta, double gamma, double rho) {
		this.m = m;
		this.Q = Q;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.rho = rho;
		r = new Random();
	}

	protected Map<Integer, Double> initializePheromone(Map<Integer, Double> cc) {
		Map<Integer, Double> pheromones = new HashMap<>();

		for (int j = 0; j < cc.size(); j++) {
			pheromones.put(j, cc.get(j));
		}

		return pheromones;
	}

	protected double getExecutionTime(Vm VM, Cloudlet cloudlet) {
		return (cloudlet.getCloudletLength() / (VM.getNumberOfPes() * VM.getMips()));
	}
}
