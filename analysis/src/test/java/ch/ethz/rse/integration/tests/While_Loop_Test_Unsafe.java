package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class While_Loop_Test_Unsafe {
	public static void m1(int j) {
        TrainStation s = new TrainStation(10);
    
        while (j > 0) {
            s.arrive(5);
            j--;
        }
    }
}