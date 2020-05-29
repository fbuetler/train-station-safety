package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Station_Assignment_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(2);
		TrainStation t = s;
		if (j == 0) {
			s.arrive(j);
		}
		if (j == 1) {
			t.arrive(j);
		}
	}
}
