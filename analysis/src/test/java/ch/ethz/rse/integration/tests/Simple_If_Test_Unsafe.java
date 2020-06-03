package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_If_Test_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int i = 0;
		if (j > 10) {
			i = 1;
		} else if (j >= 10) {
			i = 1;
		} else if (j < 8) {
			i = 20;
		} else if (j <= 8) {
			i = -10;
		} else if (j == 9) {
			i = 5;
		} else if (j != 9) {
			i = 27;
		}
		s.arrive(i);
		s.arrive(1);
	}
}
