package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class While_Loop_Test_Unsafe {
    // See: https://moodle-app2.let.ethz.ch/mod/forum/discuss.php?d=51019
	public static void m1(int j) {
        TrainStation s = new TrainStation(10);
    
        while (j > 0) {
            s.arrive(5);
            j--;
        }
    }
}