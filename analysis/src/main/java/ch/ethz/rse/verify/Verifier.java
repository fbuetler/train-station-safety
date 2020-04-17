package ch.ethz.rse.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Environment;
import apron.MpqScalar;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import ch.ethz.rse.utils.Constants;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;

/**
 * Main class handling verification
 * 
 */
public class Verifier extends AVerifier {

	private static final Logger logger = LoggerFactory.getLogger(Verifier.class);

	private final SootClass c;
	private final PointsToInitializer pointsTo;
	private final Map<SootMethod, NumericalAnalysis> numericalAnalysis = new HashMap<SootMethod, NumericalAnalysis>();

	/**
	 * 
	 * @param c class to verify
	 */
	public Verifier(SootClass c) {
		logger.debug("Analyzing {}", c.getName());

		this.c = c;
		// pointer analysis
		this.pointsTo = new PointsToInitializer(this.c);
		// numerical analysis
		this.runNumericalAnalysis();
	}

	private void runNumericalAnalysis() {
		// TODO (flbuetle) start analysis here and then only read results for the specific cases from it
	}

	@Override
	public boolean checkTrackNonNegative() {
		logger.debug("Analyzing checkTrackNonNegative for {}", c.getName());
		// TODO FILL THIS OUT
		for (SootMethod m : c.getMethods()) {
			logger.debug("Analyzing checkTrackNonNegative for {} method {}", c.getName(), m.getName());
			NumericalAnalysis na = new NumericalAnalysis(m, SootHelper.getUnitGraph(m), pointsTo);
			
			// TODO (flbuetle) the following is just something from the soot survival guide
			for (Unit s : m.getActiveBody().getUnits()) {
				
				NumericalStateWrapper fb = na.getFlowBefore(s);
				NumericalStateWrapper ffa;
				List<NumericalStateWrapper> bfa;
				if (s.fallsThrough()) {
					ffa = na.getFallFlowAfter(s);
				}
				if (s.branches()) {
					bfa = na.getBranchFlowAfter(s);
				}
			}
		}

		return true;
	}

	@Override
	public boolean checkTrackInRange() {
		logger.debug("Analyzing checkTrackInRange for {}", c.getName());
		// TODO: FILL THIS OUT
		
		return true;
	}

	@Override
	public boolean checkNoCrash() {
		logger.debug("Analyzing checkNoCrash for {}", c.getName());
		// TODO: FILL THIS OUT
		
		return true;
	}


}
