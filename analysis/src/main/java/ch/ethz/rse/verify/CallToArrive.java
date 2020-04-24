package ch.ethz.rse.verify;

import java.util.LinkedList;

import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import soot.SootMethod;
import soot.jimple.internal.JVirtualInvokeExpr;

/**
 * Convenience wrapper that stores information about a specific call to arrive
 */
public class CallToArrive {

	public final SootMethod method;
	public final JVirtualInvokeExpr invokeExpr;
	public final NumericalAnalysis analysis;
	public final NumericalStateWrapper state;
	/* Enough for nonNegative and inRange */
	private NumericalStateWrapper foldedState;
	/* Needed for noCrash */
	private LinkedList<NumericalStateWrapper> states = new LinkedList<NumericalStateWrapper>();

	public CallToArrive(SootMethod method, JVirtualInvokeExpr invokeExpr, NumericalAnalysis analysis,
			NumericalStateWrapper state) {
		this.method = method;
		this.invokeExpr = invokeExpr;
		this.analysis = analysis;
		this.state = state;
		this.foldedState = state;
		this.states.add(state);
	}
	
	public LinkedList<NumericalStateWrapper> getStates(){
		return states;
	}
	
	public NumericalStateWrapper getFoldedState() {
		return foldedState;
	}
	
	public void addState(NumericalStateWrapper state) {
		states.add(state);
		foldedState = foldedState.join(state);
	}
}
