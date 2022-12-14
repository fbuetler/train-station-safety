package ch.ethz.rse.verify;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import ch.ethz.rse.pointer.TrainStationInitializer;
import soot.SootMethod;
import soot.jimple.internal.JVirtualInvokeExpr;

/**
 * Convenience wrapper that stores information about a specific call to arrive
 */
public class CallToArrive {

	private static final Logger logger = LoggerFactory.getLogger(NumericalStateWrapper.class);

	public final SootMethod method;
	public final JVirtualInvokeExpr invokeExpr;
	public final NumericalAnalysis analysis;
	public final NumericalStateWrapper state;
	public final TrainStationInitializer init;
	/* Enough for nonNegative and inRange */
	private NumericalStateWrapper foldedState;
	/* Needed for noCrash */
	private LinkedList<NumericalStateWrapper> states = new LinkedList<NumericalStateWrapper>();

	public CallToArrive(SootMethod method, JVirtualInvokeExpr invokeExpr, NumericalAnalysis analysis,
						NumericalStateWrapper state, TrainStationInitializer init) {
		this.method = method;
		this.invokeExpr = invokeExpr;
		this.analysis = analysis;
		this.state = state;
		this.foldedState = state;
		this.states.add(state);
		this.init = init;
		logger.debug("Added CtA "+invokeExpr+" with state "+state);
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
