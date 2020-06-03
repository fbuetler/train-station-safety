package ch.ethz.rse.utils;

import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.verify.ClassToVerify;
import ch.ethz.rse.verify.Verifier;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class TestClass {
    private final String packageName;
    private final ClassToVerify tc;

    public TestClass(String packageName) {
        try {
            this.packageName = packageName;
            String basedir = Configuration.props.getBasedir();
            File classPath = new File(basedir + "/target/test-classes");
            this.tc = new ClassToVerify(classPath, packageName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Did you compile your test, e.g., using `mvn test-compile`?", e);
        }
    }

    public SootClass getSootClass() {
        return SootHelper.loadClass(this.tc);
    }

    public Map<String, NumericalAnalysis> getNumericalAnalysis() {
        SootClass c = this.getSootClass();
        PointsToInitializer pointsTo = new PointsToInitializer(c);
        Map<String, NumericalAnalysis> amap = new HashMap<String, NumericalAnalysis>();

        for (SootMethod m: c.getMethods()) {
            String methodName = m.getName();
            if (methodName.contains("<init>")) {
                continue; // skip constructor
            }

            amap.put(methodName, new NumericalAnalysis(m, SootHelper.getUnitGraph(m), pointsTo));
        }

        return amap;
    }

    public Verifier getVerifier() {
        return new Verifier(this.getSootClass());
    }
}
