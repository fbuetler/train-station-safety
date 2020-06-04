package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Coverage_Comb_Sides {
	public static void m1(int k) {
		TrainStation s = new TrainStation(10);
		int i = 1;
		int j = 2;

		if (i != j) {
			s.arrive(i); // 1
		}
		i++;
		if (i == i) {
			j = 3;
		}
		if (i == k) {
			s.arrive(k); // 2
		}
		j++;
		i = j + 1;
		if (0 < j) {
			s.arrive(j); // 4
			j = 6;
		}
		if (i < 5) {
			s.arrive(i); // 5
		}
		if (4 != 4) {
			s.arrive(k);
		}
		if (k == j) {
			s.arrive(k); // 6
		}
		if (8 == k) {
			s.arrive(k); // 8
		}
		s.arrive(9);
	}
}
