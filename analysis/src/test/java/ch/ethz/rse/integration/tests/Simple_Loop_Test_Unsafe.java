package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Loop_Test_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		for (int i = -5; i < 15; i++) {
			s.arrive(i);
		}
		s.arrive(5);
	}
}
