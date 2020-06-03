package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// DISABLED
// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Simple_NoCrash_Test_Safe {
    public static void m2(int j) {
        TrainStation s = new TrainStation(10);
        if(j==0) {
            s.arrive(j);
            j++;
            s.arrive(j);
        }
    }
    public static void m3(int j){
        TrainStation s = new TrainStation(10);
        if(j==0 || j==1){
            s.arrive(j);
        }
        if(j==2 || j==3){
            s.arrive(j);
        }
    }
}
