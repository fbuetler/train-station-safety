package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Moodle_Test_2 {
	// https://moodle-app2.let.ethz.ch/mod/forum/discuss.php?d=50767
	public static void m2(int j) {
	    TrainStation s = new TrainStation(10);
	     j = 10;
	     for (int i = 0; i < j; i++ ) {
	       s.arrive(i);
	    }
	}
}