package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Simple_Parameter_Test_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		s.arrive(j);
	}

	public static void m2(int j) {
		TrainStation s = new TrainStation(10);
		for (int i = 0; i < 10; i++) {
			s.arrive(j);
		}
	}

	public static void m3(int j) {
		TrainStation s = new TrainStation(10);
		for (int i = 0; i < 10; i++) {
			s.arrive(j);
			j++;
		}
	}
	
	public static void m4(int j) {
		TrainStation s = new TrainStation(10);
		for (int i=0; i<3;i++) {
			s.arrive(8+i);
		}
	}
}
