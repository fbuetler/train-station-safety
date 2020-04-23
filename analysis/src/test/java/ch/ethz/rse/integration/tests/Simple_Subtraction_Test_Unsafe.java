package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Subtraction_Test_Unsafe {
	public static void s1(int j) {
		TrainStation s = new TrainStation(27);
		int i = 0;
		if (j > 3) {
			i = 100;
		} else if (j == 2) {
			i = -100;
		}
		int n = 3 - i;
		s.arrive(n);
		s.arrive(3);
	}
}
