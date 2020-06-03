package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Subtraction_Test_Safe {
	public static void s1(int j) {
		TrainStation s = new TrainStation(27);
		int i = 3;
		if (j > 3) {
			i = 2;
		}
		int n = 3 - i;
		s.arrive(n);
	}
}
