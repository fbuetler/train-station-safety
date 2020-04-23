package ch.ethz.rse.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Manager;
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
import soot.jimple.IntConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.spark.pag.Node;

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
		for (SootMethod method : this.c.getMethods()) {
			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			logger.debug(method.getActiveBody().toString());
			this.numericalAnalysis.put(method,
					new NumericalAnalysis(method, SootHelper.getUnitGraph(method), this.pointsTo));
		}
	}

	@Override
	public boolean checkTrackNonNegative() {
		logger.debug("Analyzing checkTrackNonNegative for {}", c.getName());

		for (SootMethod method : this.c.getMethods()) {
			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			NumericalAnalysis na = numericalAnalysis.get(method);

			boolean nonNegative = true;
			for (CallToArrive callToArrive : na.arrivals) {
				logger.debug("A callToArrive: {}", callToArrive);
				Abstract1 state = callToArrive.state.get();
				if (state == null) {
					logger.error("CallToArrive state is empty: {}", callToArrive);
				}

				VirtualInvokeExpr invokeExpr = callToArrive.invokeExpr;
				Value arg = invokeExpr.getArg(0);
				nonNegative &= checkConstraint(0, Integer.MAX_VALUE, arg, state, na.man);
			}
//			return nonNegative;
		}
		return false;
	}

	@Override
	public boolean checkTrackInRange() {
		logger.debug("Analyzing checkTrackInRange for {}", c.getName());

		for (SootMethod method : this.c.getMethods()) {
			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			NumericalAnalysis na = numericalAnalysis.get(method);
			boolean inRange = true;
			for (CallToArrive callToArrive : na.arrivals) {
				Abstract1 state = callToArrive.state.get();
				if (state == null) {
					logger.error("CallToArrive state is empty: {}", callToArrive);
				}

				VirtualInvokeExpr invokeExpr = callToArrive.invokeExpr;
				Value arg = callToArrive.invokeExpr.getArg(0);

				Value base = invokeExpr.getBase();
				Collection<Node> nodes = pointsTo.getNodes((JimpleLocal) base);
				if (nodes.size() > 1) {
					logger.warn("Size of objects the variable '{}' points to has size {} and contains {}",
							base.toString(), nodes.size(), nodes.toString());
					// TODO (flbuetle) can there be more than one node?
				}
				int nTracks = 0;
				for (Node n : nodes) {
					TrainStationInitializer tsi = pointsTo.getTSInitializer(n);
					nTracks = tsi.nTracks;
				}

				inRange &= checkConstraint(Integer.MIN_VALUE, nTracks - 1, arg, state, na.man);
			}
			return inRange;
		}
		return false;
	}

	/*
	 * checkConstraint checks if the given arg with the allowed interval
	 * [lowerBound, upperBound] is satisfied in the given state
	 * 
	 */
	private boolean checkConstraint(int lowerBound, int upperBound, Value arg, Abstract1 state, Manager man) {
		logger.debug("state: {}", state);
		logger.debug("arg: {}", arg);
		logger.debug("interval: [{}, {}]", lowerBound, upperBound);

		boolean inInterval = false;
		if (arg instanceof JimpleLocal) {
			String varName = ((JimpleLocal) arg).getName();
			try {
				inInterval = state.satisfy(man, varName, new Interval(lowerBound, upperBound));
			} catch (ApronException e) {
				logger.error("Check for constraint satisfiability threw an exception");
				e.printStackTrace();
			}
		} else if (arg instanceof IntConstant) {
			int val = ((IntConstant) arg).value;
			inInterval = lowerBound <= val && val <= upperBound;
		} else {
			logger.error("Unhandled arg type in checkConstraint: {}", arg);
		}
		return inInterval;
	}

	@Override
	public boolean checkNoCrash() {
		logger.debug("Analyzing checkNoCrash for {}", c.getName());
		// TODO: FILL THIS OUT

		return true;
	}

}
