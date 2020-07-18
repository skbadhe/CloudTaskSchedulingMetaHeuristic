package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ACO.ACO_Scheduler;

public class GetResults {
	static double Minimum = 100000;
	static double Maximum = 0;
	public static List<Double> listvalues = new ArrayList<Double>();
	public static List<Double> listDI = new ArrayList<Double>();
	
	public static void main(String[] args) {
		double total = 0;
		for(int i=0;i<4;i++)
			ACO_Scheduler.main(args);
		Iterator<Double> iter = listvalues.iterator();
		double temp;
		while (iter.hasNext()) {
			temp = iter.next();
			if (temp < Minimum)
				Minimum = temp;
			if (temp > Maximum)
				Maximum = temp;
			total += temp;
		}
		double average = total/listvalues.size();
		System.out.println("Minimum "+Minimum+" Maximum "+Maximum + " Average= " + average);
		Iterator<Double> iterr = listDI.iterator();
		//System.out.println(listDI.toString());
		double temp2;
		double DI_max=0;
		double DI_min=10;
		double total2 =0;
		while(iterr.hasNext()) {
			temp2 = iterr.next();
			if(temp2 < DI_min)
				DI_min = temp2;
			if(temp2>DI_max)
				DI_max = temp2;
			total2 += temp2;
		}
		double average2 = total2/listDI.size();
		System.out.println("Minimum "+DI_min+" Maximum "+DI_max + " Average= " + average2);
		/*
		 * FCFS_Scheduler.main(args); System.out.println();
		 * //ACO_Scheduler.main(args); System.out.println();
		 * RoundRobinScheduler.main(args); System.out.println();
		 * SJF_Scheduler.main(args);
		 */
	}

}
