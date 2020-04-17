package ch.ethz.rse.pointer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import ch.ethz.rse.utils.Constants;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.Node;

public class PointsToInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PointsToInitializer.class);

	/**
	 * Internally used points-to analysis
	 */
	private final PointsToAnalysisWrapper pointsTo;

	/**
	 * class for which we are running points-to
	 */
	private final SootClass c;

	/**
	 * Maps abstract object indices to initializers
	 */
	private final Map<Node, TrainStationInitializer> initializers = new HashMap<Node, TrainStationInitializer>();

	/**
	 * All {@link TrainStationInitializer}s, keyed by method
	 */
	private final Multimap<SootMethod, TrainStationInitializer> perMethod = HashMultimap.create();

	public PointsToInitializer(SootClass c) {
		this.c = c;
		logger.debug("Running points-to analysis on " + c.getName());
		this.pointsTo = new PointsToAnalysisWrapper(c);
		logger.debug("Analyzing initializers in " + c.getName());
		this.analyzeAllInitializers();
	}

	private void analyzeAllInitializers() {
		int uniqueNumber = 0;
		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			for (Unit s : method.getActiveBody().getUnits()) {
				/*
				 * we assume here that there is only one kind of constructor call namely:
				 * 'TrainStation s = new TrainStation(10)' in Jimple the above statement looks
				 * like: $r2 = new ch.ethz.rse.TrainStation specialinvoke
				 * $r2.<ch.ethz.rse.TrainStation: void <init>(int)>(10) The following code
				 * extracts r2 as Local base and 10 as int nTracks
				 */
				if ((s instanceof JInvokeStmt) && ((JInvokeStmt) s).getInvokeExpr() instanceof JSpecialInvokeExpr) {
					// s is a constructor
					JInvokeStmt is = (JInvokeStmt) s;
					JSpecialInvokeExpr sie = (JSpecialInvokeExpr) is.getInvokeExpr();

					if (sie.getMethodRef().getDeclaringClass().getName() == Constants.trainStationClassName) {
						/*
						 * we know its a JimpleLocal because this is given as part of the project
						 * description: assignment only to local variables
						 */
						Local base = (JimpleLocal) sie.getBase();

						Value arg = sie.getArg(0);
						if (arg instanceof IntConstant) {
							IntConstant ic = (IntConstant) arg;
							int nTracks = ic.value;

							TrainStationInitializer tsi = new TrainStationInitializer(is, uniqueNumber, nTracks);
							uniqueNumber++;

							perMethod.put(method, tsi);

							Collection<Node> nodes = pointsTo.getNodes(base);
							for (Node n : nodes) {
								initializers.put(n, tsi);
							}
						}
					}
				}
			}

			// TODO populate data structures. (flbuetle) are the more?
		}
	}

	// TODO FILL THIS OUT. (flbuetle) what should go here?
}
