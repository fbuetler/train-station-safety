package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_If_Test_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int i = 0;
		if (j > 10) {
			i = 1;
		} else if (j >= 10) {
			i = 2;
		} else if (j < 8) {
			i = 3;
		} else if (j <= 8) {
			i = 4;
		} else if (j == 9) {
			i = 5;
		} else if (j != 9) {
			i = 6;
		}
		s.arrive(i);
	}
}
