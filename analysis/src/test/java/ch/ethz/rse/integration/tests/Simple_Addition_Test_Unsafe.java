package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Addition_Test_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 25 + 7;
		s.arrive(n);
		int m = 3 + Integer.MAX_VALUE;
		s.arrive(m);
		int o = 7;
		s.arrive(o);
		s.arrive(7);
	}
}
