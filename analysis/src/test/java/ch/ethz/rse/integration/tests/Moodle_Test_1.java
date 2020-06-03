package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Moodle_Test_1 {
	// https://moodle-app2.let.ethz.ch/mod/forum/discuss.php?d=51626
	public static void example(int j) {
		TrainStation s = new TrainStation(10);
		
		int i = 2; 
		int a = 2;
		while(a*i < 10) {
			s.arrive(i);
			
			i++;
			a++;
		}
	}
}