package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Subtraction_Test_Safe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 30 - 7;
		s.arrive(n);
	}
}
