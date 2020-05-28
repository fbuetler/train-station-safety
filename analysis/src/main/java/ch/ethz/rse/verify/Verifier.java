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

		boolean nonNegative = true;
		for (SootMethod method : this.c.getMethods()) {
			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			NumericalAnalysis na = numericalAnalysis.get(method);

			for (Map.Entry elem: na.arrivalsMap.entrySet()) {
				CallToArrive callToArrive = (CallToArrive) elem.getValue();
				Abstract1 state = callToArrive.getFoldedState().get();
				if (state == null) {
					logger.error("CallToArrive state is empty: {}", callToArrive);
				}
				
				VirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) ((JInvokeStmt)elem.getKey()).getInvokeExpr();
				Value arg = invokeExpr.getArg(0);
				nonNegative &= checkConstraint(0, Integer.MAX_VALUE, arg, state, na.man);
				
				if(!nonNegative) { // Stop early
					return false;
				}				
			}
		}
		return true;
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
			
			for (Map.Entry elem: na.arrivalsMap.entrySet()) {
				CallToArrive callToArrive = (CallToArrive) elem.getValue();
				Abstract1 state = callToArrive.getFoldedState().get();
				if (state == null) {
					logger.error("CallToArrive state is empty: {}", callToArrive);
				}
				
				VirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) ((JInvokeStmt)elem.getKey()).getInvokeExpr();
				Value arg = invokeExpr.getArg(0);
				
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
					inRange &= checkConstraint(Integer.MIN_VALUE, nTracks - 1, arg, state, na.man);
				}

				
				if(!inRange) { // Stop early
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * checkConstraint checks if the given arg is FULLY within the allowed interval
	 * [lowerBound, upperBound]
	 * NOTE: Overlapping intervals aren't enough to return true here
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

		for (SootMethod method : this.c.getMethods()) {
			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			NumericalAnalysis na = numericalAnalysis.get(method);
			// TODO (flbuetle) rm duplicate calls in arrivals: See comment in numericalAnalysis 
			// (lmeinen) Removing duplicate calls to arrivals is the last thing you'd want to do,
			// seeing as they are a violation of the property we're checking
			// TODO: (lmeinen) Change method such that we pass set of constraints describing set of
			//				   taken tracks forward --> Linear instead of quadratic, explains why initially -1
			logger.debug("all arrivals: {}", na.arrivals);
			boolean noCrash = true;
			int arrivalsSize = na.arrivals.size();
			for (int i = 0; i < arrivalsSize; i++) {
				for (int j = i + 1; j < arrivalsSize; j++) { // ensures no two calls are compared twice
					CallToArrive outerCtA = na.arrivals.get(i);
					CallToArrive innerCtA = na.arrivals.get(j);

					Abstract1 outerState = outerCtA.state.get();
					Abstract1 innerState = innerCtA.state.get();

					Value outerArg = outerCtA.invokeExpr.getArg(0);
					Value innerArg = innerCtA.invokeExpr.getArg(0);

					logger.debug("innter state: {}", innerState.toString());
					logger.debug("inner arg: {}", innerArg);

					logger.debug("outer state: {}", outerState.toString());
					logger.debug("outer arg: {}", outerArg);

					if (outerArg instanceof JimpleLocal) {
						String outerVarName = ((JimpleLocal) outerArg).getName();
						if (innerArg instanceof JimpleLocal) {
							// If their intervals overlap: return false
							// TODO: (lmeinen) Use polyhedra domain instead, as more precise
							String innerVarName = ((JimpleLocal) innerArg).getName();
							Interval outerBound, innerBound;
							try{
								outerBound = outerState.getBound(na.man, outerVarName);
								innerBound = innerState.getBound(na.man, innerVarName);
							}catch(ApronException e){
								throw new RuntimeException(e);
							}
							logger.debug("Inner bound: "+innerBound);
							System.out.println("Outer bound: "+outerBound);
							int comparedLowerInner = innerBound.cmp(outerBound.inf());
							int comparedUpperInner = innerBound.cmp(outerBound.sup());
							int comparedLowerOuter = outerBound.cmp(innerBound.inf());
							int comparedUpperOuter = outerBound.cmp(innerBound.sup());
							logger.debug("Comparision results: "+comparedLowerInner+" "+comparedUpperInner+" "+comparedLowerOuter+" "+comparedUpperOuter);
							// Either the inner interval is bigger than the outer, or vica-versa
							noCrash &= (comparedLowerInner == 2 && comparedUpperInner == 2) || (comparedLowerOuter == 2 && comparedUpperOuter == 2);
						} else if (innerArg instanceof IntConstant) {
							int innerVal = ((IntConstant) innerArg).value;
							noCrash &= checkVarCnst(innerVal, outerVarName, outerState, na.env, na.man);
						} else {
							logger.error("unsupported type in checkNoCrash: {}", innerArg);
						}
					} else if (outerArg instanceof IntConstant) {
						int outerVal = ((IntConstant) outerArg).value;
						if (innerArg instanceof JimpleLocal) {
							String innerVarName = ((JimpleLocal)innerArg).getName();
							noCrash &= checkVarCnst(outerVal, innerVarName, innerState, na.env, na.man);
						} else if (innerArg instanceof IntConstant) {
							int innerVal = ((IntConstant) innerArg).value;
							noCrash &= outerVal != innerVal;
						} else {
							logger.error("unsupported type in checkNoCrash: {}", innerArg);
						}
					} else {
						logger.error("unsupported type in checkNoCrash: {}", outerArg);
					}
					
					if(!noCrash) { // Stop early
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks if the given variable might equal the given constant
	 * 
	 * @param cnst		Constant value to check
	 * @param varName	Variable specified in state to check
	 * @param state		State specifying possible variable values
	 * @param env
	 * @param man
	 * @return			True if var definitely doesn't equal const, false otherwise
	 */
	public boolean checkVarCnst(int cnst, String varName, Abstract1 state, Environment env, Manager man){
		Interval sclr = new Interval(cnst, cnst);
		Texpr1VarNode variable = new Texpr1VarNode(varName);
		Texpr1CstNode constant = new Texpr1CstNode(sclr);
		Texpr1Node lArg, rArg;
		if(cnst > 0){
			lArg = variable;
			rArg = constant;
		} else {
			lArg = constant;
			rArg = variable;
		}
		Texpr1BinNode expr = new Texpr1BinNode(Texpr1BinNode.OP_SUB, lArg, rArg);
		Tcons1 constraint = new Tcons1(env, Tcons1.DISEQ, expr);
		try {
			return state.satisfy(man, constraint);
		} catch(ApronException e){
			logger.error("unsupported operation");
		}
		return true;
	}

}
