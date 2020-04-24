package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_Expr_Test_Unsafe {
	public void m2(int j) {
		TrainStation s = new TrainStation(10);
		s.arrive(3+10); // 13
		s.arrive(5*-1); // -5
		s.arrive(1*2); // 2
		s.arrive(10-8); // 2
	}
}
