package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// ENABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Control_Flow_Test_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if(j>0) {
            s.arrive(1);
        } else {
            s.arrive(1);
        }
	}
}