package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Simple_Addition_Test_CRASH_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 3 + 5;
		s.arrive(n);
		s.arrive(n);
	}
}
