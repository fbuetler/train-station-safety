package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Simple_Addition_Test_RANGE_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 3 + 26;
		s.arrive(n);
	}
}
