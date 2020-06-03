package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Involved_Station_Assignment_Crash_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if (j == 0) {
			TrainStation t = s;
			t.arrive(j);
		}
		if (j >= 0) {
		    if (j < 10) {
				s.arrive(j);
			}
		}
	}
}
