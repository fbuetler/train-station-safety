package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Looped_Station_Assignment_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if (j < 0) {
			j = 0;
		}
		if (j > 10) {
			j = 10;
		}

		s.arrive(0);
		for (int i = 1; i < j; i ++) {
			TrainStation t = s;
			t.arrive(i);
		}
	}
}
