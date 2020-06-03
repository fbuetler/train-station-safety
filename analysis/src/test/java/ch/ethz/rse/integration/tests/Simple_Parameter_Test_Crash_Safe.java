package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Simple_Parameter_Test_Crash_Safe {
	public static void m3(int j) {
		TrainStation s = new TrainStation(10);
		for (int i = 0; i < 10; i++) {
			s.arrive(j);
			j++;
		}
	}
}
