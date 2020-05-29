package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Basic_Two_Initializer_Test_Safe {
	public static void m1() {
		TrainStation s = new TrainStation(1);
		TrainStation t = new TrainStation(1);
		s.arrive(0);
		t.arrive(0);
	}
}
