package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Simple_NoCrash_Test_Unsafe {
    public static void m1(int j){
        TrainStation s = new TrainStation(10);
        if(j>=0 && j<=1){
            s.arrive(j);
        }
        if(j>=1 && j<=3){
            s.arrive(j);
        }
    }
}
