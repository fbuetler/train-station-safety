package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Alternating_States_Test_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int toggle = 1;
		int k = 1;
		for (int i = 0; i < 5; i++) {
			if (toggle == 1) {
				k = -1;
				toggle = 2;
			} else if (toggle == 2) {
				k = 11;
				toggle = 1;
			} else {
				k = 5;
			}
			s.arrive(k);
		}
	}
}
