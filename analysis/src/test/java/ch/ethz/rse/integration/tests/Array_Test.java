package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Array_Test {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		int[] arr = {0, 10};
		if (arr[0] <= j && j < arr[1]) {
			// 0<=j<10
			// s can only point to an object with 10 tracks
			s.arrive(j);
		}
	}
}
