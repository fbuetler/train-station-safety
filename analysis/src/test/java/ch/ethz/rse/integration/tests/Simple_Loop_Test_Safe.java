package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Loop_Test_Safe {
	public static void m1() {
		TrainStation s = new TrainStation(10);
		for (int i = 0; i < 10; i++) {
			s.arrive(i);
		}
	}
}
