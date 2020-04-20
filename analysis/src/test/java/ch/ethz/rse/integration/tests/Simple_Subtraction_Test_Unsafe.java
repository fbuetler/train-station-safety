package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Subtraction_Test_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int l = 1;
		int n = l - 7;
		s.arrive(n);
		int m = 20 - (-10);
		s.arrive(m);
		int o = 17 - 3;
		s.arrive(o);
		s.arrive(14);
	}
}
