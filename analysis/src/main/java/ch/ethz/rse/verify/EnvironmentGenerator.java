package ch.ethz.rse.verify;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import apron.Environment;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.pointer.TrainStationInitializer;
import soot.IntType;
import soot.IntegerType;
import soot.Local;
import soot.SootMethod;
import soot.SootHelper;
import soot.Value;
import soot.jimple.ParameterRef;
import soot.jimple.internal.JimpleLocal;
import soot.util.Chain;

/**
 * Container for environment containing all relevant values
 *
 */
public class EnvironmentGenerator {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentGenerator.class);

	private final SootMethod method;
	private final PointsToInitializer pointsTo;

	/**
	 * List of names for integer variables relevant when analyzing the program
	 */
	private List<String> ints = new LinkedList<String>();

	private final Environment env;

	/**
	 * 
	 * @param method
	 * @param pointsTo
	 */
	public EnvironmentGenerator(SootMethod method, PointsToInitializer pointsTo) {
		this.method = method;
		this.pointsTo = pointsTo;

		// add variables
		logger.debug("adding local variables");
		for (Local var : method.getActiveBody().getLocals()) {
			addToInts(var);
		}

		// add parameters
		logger.debug("adding method parameters");
		for (Local var : method.getActiveBody().getParameterLocals()) {
			addToInts(var);
		}

		// add initial track constraint
		logger.debug("adding track constraint");
		for (TrainStationInitializer ts: pointsTo.getTSInitPerMethod(method)) {
			int uwu = ts.getUniqueNumber();
			this.ints.add("track_"+uwu);
		}

		String ints_arr[] = Iterables.toArray(this.ints, String.class);
		String reals[] = {}; // we are not analyzing real numbers
		this.env = new Environment(ints_arr, reals);
	}

	private void addToInts(Local var) {
		String varname = var.getName();
		if (SootHelper.isIntValue(var)) {
			if (!this.ints.contains(varname)) {
				logger.debug("Adding variable to env: {}", varname);
				this.ints.add(varname);
			} else {
				logger.error("Illegal duplication of variable declaration found: {} (current declarations: {})",
						varname, ints.toString());
			}
		} else {
			logger.warn("Non integer typed variable found: {} (with type: {})", var, var.getType());
		}
	}

	public Environment getEnvironment() {
		return this.env;
	}

}
