package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Multiplication_Test_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int n = 3 * -2;
		s.arrive(n);
		int m = 10 * 10;
		s.arrive(m);
		int o = 5 * 4;
		s.arrive(o);
		s.arrive(20);
	}
}
