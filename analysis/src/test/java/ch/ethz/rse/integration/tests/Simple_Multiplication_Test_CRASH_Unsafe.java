package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Simple_Multiplication_Test_CRASH_Unsafe {
	public static void s1() {
		TrainStation s = new TrainStation(27);
		int o = 5 * 4;
		s.arrive(o);
		s.arrive(20);
	}
}
