package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Simple_MultipleInitializers_Test_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if(j == 11) {
			s = new TrainStation(11);		
		}
		s.arrive(10);
	}
}
