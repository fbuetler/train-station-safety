package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Addition_Test_NEGATIVE_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 3 + Integer.MAX_VALUE;
		s.arrive(n);
	}
}
