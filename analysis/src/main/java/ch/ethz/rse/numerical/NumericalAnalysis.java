package ch.ethz.rse.numerical;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Lincons1;
import apron.Linexpr1;
import apron.Manager;
import apron.MpqScalar;
import apron.Polka;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import ch.ethz.rse.utils.Constants;
import ch.ethz.rse.verify.CallToArrive;
import ch.ethz.rse.verify.EnvironmentGenerator;
import gmp.Mpfr;
import gmp.Mpq;
import soot.ArrayType;
import soot.DoubleType;
import soot.IntegerType;
import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.MulExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JDivExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public class NumericalAnalysis extends ForwardBranchedFlowAnalysis<NumericalStateWrapper> {

	private static final Logger logger = LoggerFactory.getLogger(NumericalAnalysis.class);

	private final SootMethod method;

	private final PointsToInitializer pointsTo;

	/**
	 * number of times this loop head was encountered during analysis
	 */
	private HashMap<Unit, IntegerWrapper> loopHeads = new HashMap<Unit, IntegerWrapper>();
	/**
	 * Previously seen abstract state for each loop head
	 */
	private HashMap<Unit, NumericalStateWrapper> loopHeadState = new HashMap<Unit, NumericalStateWrapper>();
	
	public List<CallToArrive> arrivals = new LinkedList<CallToArrive>();
	/**
	 * the CallToArrive objects in this map store all states which went through Unit
	 * --> Allows for more efficient iteration of calls to arrive()
	 */
	public HashMap<Unit, CallToArrive> arrivalsMap = new HashMap<Unit, CallToArrive>();

	/**
	 * Numerical abstract domain to use for analysis: Convex polyhedra
	 */
	public final Manager man = new Polka(true);

	public final Environment env;

	/**
	 * We apply widening after updating the state at a given merge point for the
	 * {@link WIDENING_THRESHOLD}th time
	 * 
	 * (lmeinen) We could probably increase precision by increasing this threshold,
	 * at the cost of runtime (lmeinen) Should we reset the number of times a head
	 * is met when the branch falls through 100%, e.g. for nested loops?
	 */
	private static final int WIDENING_THRESHOLD = 6;

	/**
	 * 
	 * @param method   method to analyze
	 * @param g        control flow graph of the method
	 * @param pointsTo result of points-to analysis
	 */
	public NumericalAnalysis(SootMethod method, UnitGraph g, PointsToInitializer pointsTo) {
		super(g);

		this.method = method;
		this.pointsTo = pointsTo;

		this.env = new EnvironmentGenerator(method, this.pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class
		logger.info("Analyzing {} in {}", method.getName(), method.getDeclaringClass().getName());
		doAnalysis();
	}

	/**
	 * Report unhandled instructions, types, cases, etc.
	 * 
	 * @param task description of current task
	 * @param what
	 */
	public static void unhandled(String task, Object what, boolean raiseException) {
		String description = task + ": Can't handle " + what.toString() + " of type " + what.getClass().getName();

		if (raiseException) {
			throw new UnsupportedOperationException(description);
		} else {
			logger.error(description);

			// print stack trace
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stackTrace.length; i++) {
				logger.error(stackTrace[i].toString());
			}
		}
	}

	@Override
	// copy brings OUT set to predecessor's IN set
	protected void copy(NumericalStateWrapper source, NumericalStateWrapper dest) {
		source.copyInto(dest);
	}

	@Override
	protected NumericalStateWrapper newInitialFlow() {
		// should be bottom (only entry flows are not bottom originally)
		return NumericalStateWrapper.bottom(man, env);
	}

	@Override
	protected NumericalStateWrapper entryInitialFlow() {
		// state of entry points into function
		return NumericalStateWrapper.top(man, env);
	}

	@Override
	// merge joins two out sets to make a new in set
	protected void merge(Unit succNode, NumericalStateWrapper in1, NumericalStateWrapper in2,
			NumericalStateWrapper out) {
		logger.debug("in merge: " + succNode);

		logger.debug("join: {} with {}", in1, in2);
//		out = in1.join(in2);
		NumericalStateWrapper out_new = in1.join(in2);
		out_new.copyInto(out);
	}

	@Override
	protected void merge(NumericalStateWrapper src1, NumericalStateWrapper src2, NumericalStateWrapper trg) {
		// this method is never called, we are using the other merge instead
		throw new UnsupportedOperationException();
	}

	@Override
	/**
	 * "Brains" of the analysis. Determines the OUT state depending on the IN state
	 * for one Unit. Need to differentiate between branch and fall-through OUT set.
	 * 
	 * TODO: (lmeinen) I believe we still need to actually modify the OUT sets to
	 * contain updated values?
	 * 
	 * @param inWrapper         incoming numerical state
	 * @param op                statement to be analyzed
	 * @param fallOutWrappers   is a one-element list
	 * @param branchOutWrappers contains a NumericalStateWrapper for each
	 *                          non-fall-through successor
	 */
	protected void flowThrough(NumericalStateWrapper inWrapper, Unit op, List<NumericalStateWrapper> fallOutWrappers,
			List<NumericalStateWrapper> branchOutWrappers) {
		logger.debug(inWrapper + " " + op + " => ?");

		Stmt s = (Stmt) op;

		// wrapper for state after running op, assuming we move to the next statement
		assert fallOutWrappers.size() <= 1;
		NumericalStateWrapper fallOutWrapper = null;
		if (fallOutWrappers.size() == 1) {
			fallOutWrapper = fallOutWrappers.get(0);
			inWrapper.copyInto(fallOutWrapper);
		}

		// wrapper for state after running op, assuming we follow a jump
		assert branchOutWrappers.size() <= 1;
		NumericalStateWrapper branchOutWrapper = null;
		if (branchOutWrappers.size() == 1) {
			branchOutWrapper = branchOutWrappers.get(0);
			inWrapper.copyInto(branchOutWrapper);
		}

		try {
			if (s instanceof DefinitionStmt) {
				// handle assignment

				DefinitionStmt sd = (DefinitionStmt) s;
				Value left = sd.getLeftOp();
				Value right = sd.getRightOp();

				// We are not handling these cases:
				if (!(left instanceof JimpleLocal)) {
					unhandled("Assignment to non-local variable", left, true);
				} else if (left instanceof JArrayRef) {
					unhandled("Assignment to a non-local array variable", left, true);
				} else if (left.getType() instanceof ArrayType) {
					unhandled("Assignment to Array", left, true);
				} else if (left.getType() instanceof DoubleType) {
					unhandled("Assignment to double", left, true);
				} else if (left instanceof JInstanceFieldRef) {
					unhandled("Assignment to field", left, true);
				}

				if (left.getType() instanceof RefType) {
					// assignments to references are handled by pointer analysis
					// no action necessary
				} else {
					// handle assignment
					handleDef(fallOutWrapper, left, right);
				}
			} else if (s instanceof JIfStmt) {
				// handle if

				JIfStmt jIfStmt = (JIfStmt) s;
				handleIf(jIfStmt, inWrapper, fallOutWrapper, branchOutWrapper);
			} else if (s instanceof JInvokeStmt && ((JInvokeStmt) s).getInvokeExpr() instanceof JVirtualInvokeExpr) {
				// handle invocations

				JInvokeStmt jInvStmt = (JInvokeStmt) s;
				handleInvoke(jInvStmt, fallOutWrapper);
			}

			// log outcome
			if (fallOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[fallout] " + fallOutWrapper);
				fallOutWrapper.copyInto(fallOutWrappers.get(0));
			}
			if (branchOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[branchout] " + branchOutWrapper);
				branchOutWrapper.copyInto(branchOutWrappers.get(0));
			}

		} catch (ApronException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * Handle method invocations. Only arrive method is invoked
	 * 
	 * example input: virtualinvoke $r2.<ch.ethz.rse.TrainStation: void
	 * arrive(int)>(i0) <Top>
	 * 
	 * @param jInvStmt       Statement to be handled (scope of this project: only
	 *                       invoke stmts to arrive()
	 * @param fallOutWrapper Numerical state at time of invocation
	 * @throws ApronException
	 */
	private void handleInvoke(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		// TODO (flbuetle) currently we add up to WIDENING_THRESHOLD+1 times a CallToArrive if arrive is called in a loop body
		CallToArrive arrival = new CallToArrive(this.method, (JVirtualInvokeExpr) jInvStmt.getInvokeExpr(), this,
				fallOutWrapper);
		arrivals.add(arrival);
		if(arrivalsMap.containsKey(jInvStmt)) {
			arrivalsMap.get(jInvStmt).addState(fallOutWrapper);
		} else {
			arrivalsMap.put(jInvStmt, arrival);
		}
	}

	/**
	 * 
	 * Handle branches: Need to differentiate between taken and fall-through branch
	 * Condition can be of type ==, >=, >, <=, <, or !=, with LHS and RHS being
	 * constants or local variables
	 * 
	 * example input: if i0 > 10 goto return <Top>
	 * 
	 * TODO: Make pretty, eliminate excessive copies NOTE: When widening, need to
	 * carefully choose outgoing numerical states such that fixed-point can be
	 * reached
	 * 
	 * @param jIfStmt          Statement to be handled
	 * @param fallOutWrapper   Numerical state if branch condition evaluates to
	 *                         false
	 * @param branchOutWrapper Numerical state if branch condition evaluates to true
	 * @throws ApronException
	 */
	private void handleIf(JIfStmt jIfStmt, NumericalStateWrapper inWrapper, NumericalStateWrapper fallOutWrapper,
			NumericalStateWrapper branchOutWrapper) throws ApronException {
		assert (fallOutWrapper != null && branchOutWrapper != null);

		if (loopHeads.containsKey(jIfStmt)) { // decide if its an if or loop statement
			int iter = loopHeads.get(jIfStmt).increment();
			if (iter > WIDENING_THRESHOLD) {
				NumericalStateWrapper prevState = loopHeadState.get(jIfStmt);
				inWrapper = prevState.widen(inWrapper);
			}
		}

		// Need the copy because Soot apparently reuses the passed inWrapper object
		// --> Up until now we needed to widen twice, the first one to create our own
		// copy of the object,
		// the second to actually obtain a fixpoint
		loopHeadState.put(jIfStmt, inWrapper.copy());

		Value condition = jIfStmt.getCondition();
		Linexpr1 expr = combSides(condition, false);
		Linexpr1 exprInv = combSides(condition, true);
		Lincons1 aprCondition, aprConditionInv;

		if (condition instanceof JEqExpr) {
			aprCondition = new Lincons1(Lincons1.EQ, expr);
			aprConditionInv = new Lincons1(Lincons1.DISEQ, expr);
		} else if (condition instanceof JGeExpr || condition instanceof JLeExpr) {
			aprCondition = new Lincons1(Lincons1.SUPEQ, expr);
			aprConditionInv = new Lincons1(Lincons1.SUP, exprInv);
		} else if (condition instanceof JGtExpr || condition instanceof JLtExpr) {
			aprCondition = new Lincons1(Lincons1.SUP, expr);
			aprConditionInv = new Lincons1(Lincons1.SUPEQ, exprInv);
		} else if (condition instanceof JNeExpr) {
			aprCondition = new Lincons1(Lincons1.DISEQ, expr);
			aprConditionInv = new Lincons1(Lincons1.EQ, expr);
		} else {
			throw new ApronException();
		}

		// See L328 of ApronTest to see how meetCopy works
		if (branchOutWrapper != null) {
			branchOutWrapper.set(inWrapper.get().meetCopy(man, aprCondition)); // Condition holds
		}
		if (fallOutWrapper != null) {
			fallOutWrapper.set(inWrapper.get().meetCopy(man, aprConditionInv)); // Condition doesn't hold
		}

		/*
		 * If we don't branch back for sure, reset No of loop iterations to prevent loss
		 * of precision for inner loops --> Doesn't work! When there are several loop
		 * conditions, e.g. 0<=j && j<10, Soot seperates it into two seperate if stmts,
		 * where only the first one is counted as a loop, i.e. when the first one is
		 * trivially true (branchOut always empty) we never widen
		 */
//		if(branchOutWrapper.get().isBottom(man) && loopHeads.containsKey(jIfStmt)) {
//			loopHeads.get(jIfStmt).value = 0;
//		}
	}

	/**
	 *
	 * Statements of the sort x = y, x = 5, or x = EXPR, where EXPR is one of the
	 * three binary expressions: *, +, or - EXPR only uses local variables or
	 * constants
	 * 
	 * example input: <Top> i0 := @parameter0: int
	 *
	 * @param outWrapper Will contain updated numerical state after assignment has
	 *                   been handled
	 * @param left       LHS of equation, i.e. variable being assigned to
	 * @param right      RHS of equation, i.e. a variable, a constant, or a binary
	 *                   expression
	 */
	private void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		String varName = ((JimpleLocal) left).getName();
		Texpr1Node expr;
		if (right instanceof BinopExpr) {
			BinopExpr rExpr = (BinopExpr) right;
			Texpr1Node lArg = Texpr1Node.fromLinexpr1(atomic(rExpr.getOp1()));
			Texpr1Node rArg = Texpr1Node.fromLinexpr1(atomic(rExpr.getOp2()));

			if (rExpr instanceof AddExpr) {
				// RHS is an addition
				expr = new Texpr1BinNode(Texpr1BinNode.OP_ADD, lArg, rArg);
			} else if (rExpr instanceof MulExpr) {
				// RHS is a multiplication
				expr = new Texpr1BinNode(Texpr1BinNode.OP_MUL, lArg, rArg);
			} else {
				// RHS is a subtraction
				expr = new Texpr1BinNode(Texpr1BinNode.OP_SUB, lArg, rArg);
			}
		} else {
			// RHS is a variable, a constant or a parameter
			expr = Texpr1Node.fromLinexpr1(atomic(right));
		}
		Texpr1Intern value = new Texpr1Intern(env, expr);
		outWrapper.assign(varName, value);
	}

	/**
	 * Changes a value into a linear expression, such that it can be used to create
	 * an expression tree
	 * 
	 * @param val Value representing either a Local, a Constant or a Parameter
	 * @return Linexpr1 representing val
	 * @throws ApronException
	 */
	private Linexpr1 atomic(Value value) {
		Linexpr1 expr = new Linexpr1(env);
		MpqScalar sclr = new MpqScalar();

		if (value instanceof JimpleLocal) {
			String varname = ((JimpleLocal) value).getName();
			sclr.set(1);
			expr.setCoeff(varname, sclr);
		} else if (value instanceof IntConstant) {
			int val = ((IntConstant) value).value;
			sclr.set(val);
			expr.setCst(sclr);
		} else if (value instanceof ParameterRef) {
			Interval intv = new Interval();
			intv.setTop(); // (flbuetle) or where else do we get top from?
			expr.setCst(intv);
		} else {
			logger.warn("unhandled value");
		}
		return expr;
	}

	/**
	 * Turns Soot expression of the form (left op right) into an Apron expression of
	 * the form (left-right op 0) Left and right are inverted when op is < or <= due
	 * to Apron only offering <>, ==, >, >=
	 * 
	 * @param condition Conditional expression to be turned into Apron expression
	 * @param inverse   Do we need the inverse expression (useful when you want to
	 *                  obtain the inverse of a condition without painfully
	 *                  inverting its coefficients)
	 */
	private Linexpr1 combSides(Value condition, boolean inverse) {
		Value left = ((BinopExpr) condition).getOp1();
		Value right = ((BinopExpr) condition).getOp2();
		if (inverse ^ (condition instanceof JLeExpr || condition instanceof JLtExpr)) {
			Value tmp = left;
			left = right;
			right = tmp;
		}
		Linexpr1 expr = new Linexpr1(env);
		MpqScalar sclr = new MpqScalar();

		// we store left - right in expr
		if (left instanceof JimpleLocal) {
			String varNameLeft = ((JimpleLocal) left).getName();
			sclr.set(1);
			expr.setCoeff(varNameLeft, sclr);
			if (right instanceof JimpleLocal) { // variable - variable
				String varNameRight = ((JimpleLocal) right).getName();
				if (varNameLeft == varNameRight) {
					sclr.set(0);
					expr.setCst(sclr);
				} else {
					sclr.set(-1);
					expr.setCoeff(varNameRight, sclr);
				}
			} else if (right instanceof IntConstant) { // variable - value
				int valRight = ((IntConstant) right).value;
				sclr.set(-valRight);
				expr.setCst(sclr);
			} else if (right instanceof ParameterRef) {
				Interval intv = new Interval();
				intv.setTop(); // (flbuetle) or where else do we get top from?
				expr.setCst(intv);
			} else {
				logger.warn("unhandled case for RHS of a condition");
			}
		} else if (left instanceof IntConstant) {
			int valLeft = ((IntConstant) left).value;
			if (right instanceof JimpleLocal) { // value - variable
				String varNameRight = ((JimpleLocal) right).getName();
				sclr.set(valLeft);
				expr.setCst(sclr);
				sclr.set(-1);
				expr.setCoeff(varNameRight, sclr);
			} else if (right instanceof IntConstant) { // value - value
				int valRight = ((IntConstant) right).value;
				sclr.set(valLeft - valRight);
				expr.setCst(sclr);
			} else if (right instanceof ParameterRef) {
				Interval intv = new Interval();
				intv.setTop(); // (flbuetle) or where else do we get top from?
				expr.setCst(intv);
			} else {
				logger.warn("unhandled case for RHS of a condition");
			}
		} else if (left instanceof ParameterRef) {
			Interval intv = new Interval();
			intv.setTop(); // (flbuetle) or where else do we get top from?
			expr.setCst(intv);
		} else {
			logger.warn("unhandled case for LHS of a condition");
		}

		return expr;
	}

}
