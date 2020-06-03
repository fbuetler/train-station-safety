package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Moodle_Test_4 {
	// https://moodle-app2.let.ethz.ch/mod/forum/discuss.php?d=51993
	public static void m3(int x) {
		TrainStation st = new TrainStation(500);

		for (int i = 0; i < 20; i++) {
			st.arrive(i);
		}
		for (int j = 0; j < x; j++) {
			st.arrive(j);
		}
		st.arrive(12);
		st.arrive(48);
	}
}