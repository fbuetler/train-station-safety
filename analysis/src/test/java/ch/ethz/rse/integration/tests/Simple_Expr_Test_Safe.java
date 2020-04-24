package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Expr_Test_Safe {
	public void m2(int j) {
		TrainStation s = new TrainStation(10);
		s.arrive(3+4); // 7
		s.arrive(1*2); // 2
		s.arrive(10-4); // 6
	}
}
