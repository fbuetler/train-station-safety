package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Alternating_States_Test_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int toggle = 0;
		int k = 0;
		for (int i = 0; i < 2; i++) {
			if (toggle == 0) {
				k = 3;
				toggle = 1;
			} else {
				k = 7;
				toggle = 0;
			}
			s.arrive(k);
		}
	}
}