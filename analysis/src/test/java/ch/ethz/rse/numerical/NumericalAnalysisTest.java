package ch.ethz.rse.numerical;

import ch.ethz.rse.utils.TestClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class NumericalAnalysisTest {
    String classpath = "ch.ethz.rse.numerical.testcases.";

    private class UnhandledExecutor implements Executable {
        private boolean raiseException;

        public UnhandledExecutor(boolean raiseException) {
            this.raiseException = raiseException;
        }

        @Override
        public void execute() throws Throwable {
            Object obj = new Object();
            NumericalAnalysis.unhandled("task", obj, this.raiseException);
        }
    }

    @Test
    void unhandledRaiseException() {
        Assertions.assertThrows(UnsupportedOperationException.class, new UnhandledExecutor(true));
    }
    @Test
    void unhandledNoException() {
        Assertions.assertDoesNotThrow(new UnhandledExecutor(false));
    }

    @Test
    void copy() {
        TestClass tc = new TestClass(classpath +"SimpleMethod");
        NumericalAnalysis na = tc.getNumericalAnalysis().get("method");
        NumericalStateWrapper wExp = na.newInitialFlow();
        NumericalStateWrapper wMut = na.entryInitialFlow();

        na.copy(wExp, wMut);
        Assertions.assertEquals(wExp, wMut);
    }

}
