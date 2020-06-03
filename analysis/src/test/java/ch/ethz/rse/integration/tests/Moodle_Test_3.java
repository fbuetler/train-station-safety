package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Moodle_Test_3 {
	// https://moodle-app2.let.ethz.ch/mod/forum/discuss.php?d=51993
	public static void m1(int j) {

		TrainStation s = new TrainStation(10);

		for (int i = 0; i < 100; i++) {
			if (i < 10) {
				s.arrive(i);
			}
		}
	}
}