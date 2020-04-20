package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Simple_Multiplication_Test_RANGE_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 4;
		int m = 10 * n;
		int o = m * 2;
		s.arrive(o);
	}
}
