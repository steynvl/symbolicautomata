/**
 * SVPAlib
 * automata
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package automata.safa;

import java.util.Collections;
import java.util.Objects;

import org.sat4j.specs.TimeoutException;

import automata.safa.booleanexpression.PositiveBooleanExpression;
import theory.BooleanAlgebra;

/**
 * SAFAInputMove
 * @param <P> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public class SAFAInputMove<P,S> extends SAFAMove<P, S> {

	public SAFAInputMove(Integer from, PositiveBooleanExpression to, P guard) {
		super(from, to);
		toStates = to.getStates();
		if (toStates.isEmpty()) {
			maxState = -1;
		} else {
			maxState = Collections.max(toStates);
		}
		if (maxState < from) {
			maxState = from;
		}

		this.guard = guard;
	}

	public boolean isSatisfiable(BooleanAlgebra<P,S> boolal) throws TimeoutException {
		if (guard == null) {
			return true;
		} else {
			return boolal.IsSatisfiable(guard);
		}
	}

	public S getWitness(BooleanAlgebra<P, S> ba) throws TimeoutException {
		return ba.generateWitness(guard);
	}

	public boolean hasModel(S el, BooleanAlgebra<P, S> ba) throws TimeoutException {
		return ba.HasModel(guard, el);
	}

	@Override
	public String toDotString() {
		if (to.getStates().size() == 1) {
			return String.format("%s -> %s [label=\"%s\"]\n", from, to, guard);
		} else {
			StringBuilder sb = new StringBuilder();
			for (Integer to : to.getStates()) {
				sb.append(String.format("%s -> %s [label=\"%s\"]\n", from, to, guard));
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		return String.format("S: %s --> %s", from, to);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SAFAInputMove<?, ?>) {
			SAFAInputMove<?, ?> otherCasted = (SAFAInputMove<?, ?>) other;
			return otherCasted.from.equals(from) && otherCasted.to.equals(to) && otherCasted.guard.equals(guard);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to);
	}

	@Override
	public boolean isDisjointFrom(SAFAMove<P, S> t, BooleanAlgebra<P, S> ba) throws TimeoutException {
		System.out.println("Method 'isDisjointFrom' not supported in class 'SAFAInputMove'");
		System.exit(-3);

		return true;
	}

	@Override
	public Object clone(){
		return new SAFAInputMove<P, S>(from, to, guard);
	}

	@Override
	public boolean isEpsilonTransition() {
		return false;
	}


}
