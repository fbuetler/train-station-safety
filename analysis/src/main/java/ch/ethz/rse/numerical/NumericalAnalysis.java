package ch.ethz.rse.numerical;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
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
import ch.ethz.rse.verify.EnvironmentGenerator;
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

	/**
	 * Numerical abstract domain to use for analysis: Convex polyhedra
	 */
	public final Manager man = new Polka(true);

	public final Environment env;

	/**
	 * We apply widening after updating the state at a given merge point for the
	 * {@link WIDENING_THRESHOLD}th time
	 * 
	 * (lmeinen) We could probably increase precision by increasing this threshold, at the cost of runtime
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
	protected void merge(Unit succNode, NumericalStateWrapper in1, NumericalStateWrapper in2, NumericalStateWrapper out) {
		logger.debug("in merge: " + succNode);

		logger.debug("join: ");
		out = in1.join(in2);
	}

	@Override
	protected void merge(NumericalStateWrapper src1, NumericalStateWrapper src2, NumericalStateWrapper trg) {
		// this method is never called, we are using the other merge instead
		throw new UnsupportedOperationException();
	}

	@Override
	/**
	 * "Brains" of the analysis. Determines the OUT state depending on the IN state.
	 * Need to differentiate between branch and fall-through OUT set.
	 * 
	 * TODO: (lmeinen) I believe we still need to actually modify the OUT sets to contain updated values?
	 * 
	 * @param	inWrapper			incoming numerical state
	 * @param	op					statement to be analyzed
	 * @param	fallOutWrappers 	is a one-element list
	 * @param	branchOutWrappers 	contains a FlowSet for each non-fall-through successor
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
				handleIf(jIfStmt, fallOutWrapper, branchOutWrapper);
			} else if (s instanceof JInvokeStmt && ((JInvokeStmt) s).getInvokeExpr() instanceof JVirtualInvokeExpr) {
				// handle invocations
				JInvokeStmt jInvStmt = (JInvokeStmt) s;
				handleInvoke(jInvStmt, fallOutWrapper);
			}

			// log outcome
			if (fallOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[fallout] " + fallOutWrapper);
			}
			if (branchOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[branchout] " + branchOutWrapper);
			}

		} catch (ApronException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * Handle method invocations
	 * 
	 * TODO: (lmeinen) Inquire regarding possible method invocations (i.e. just arrive or also other methods possible?)
	 * 
	 * @param jInvStmt			Statement to be handled
	 * @param fallOutWrapper	Numerical state at time of invocation
	 * @throws ApronException	
	 */
	public void handleInvoke(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		// TODO: FILL THIS OUT
		// example input: virtualinvoke $r2.<ch.ethz.rse.TrainStation: void arrive(int)>(i0) <Top>
	}
	
	/**
	 * 
	 * Handle branches: Need to differentiate between taken and fall-through branch
	 * Condition can be of type ==, >=, >, <=, <, or !=, with LHS and RHS being constants or local variables
	 * 
	 * TODO: Need to change loopHeads and loopHeadState (compare stmt with loophead) and widen when threshold reached
	 * 
	 * @param jIfStmt			Statement to be handled
	 * @param fallOutWrapper	Numerical state if branch condition evaluates to false
	 * @param branchOutWrapper	Numerical state if branch condition evaluates to true
	 * @throws ApronException	
	 */
	public void handleIf(JIfStmt jIfStmt, NumericalStateWrapper fallOutWrapper, NumericalStateWrapper branchOutWrapper) throws ApronException {
		// TODO: FILL THIS OUT
		// example input: if i0 > 10 goto return <Top>
	}


	/**
	 *
	 * Statements of the sort x = y, x = 5, or x = EXPR, where EXPR is one of the three binary expressions: *, +, or -
	 * EXPR only uses local variables or constants
	 *
	 * @param outWrapper	Will contain updated numerical state after assignment has been handled
	 * @param left			LHS of equation, i.e. variable being assigned to
	 * @param right			RHS of equation, i.e. a variable, a constant, or a binary expression
	 */
	private void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		// TODO: FILL THIS OUT
		// example input: <Top> i0 := @parameter0: int
	}

}
