package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Simple_Station_Assignment_Crash_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(1);
		TrainStation t = s;
		if (j == 0) {
			s.arrive(j);
			t.arrive(j);
		}
	}
}
