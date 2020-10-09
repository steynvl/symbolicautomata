/**
 * SVPAlib
 * automata
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package automata.eps_safa;

import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;

import java.util.Objects;

/**
 * EpsSAFAInputMove
 * @param <P> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public class EpsSAFAInputMove<P,S> extends EpsSAFAMove<P, S> {

	public EpsSAFAInputMove(Integer from, MoveType to, P guard) {
		super(from, to);
		this.guard = guard;
	}

	public EpsSAFAInputMove(Integer from, MoveType to, P guard, String regex) {
		this(from, to, guard);
		this.regex = regex;
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
		assert to.isExistentialMove();
		return String.format("%s -> %s [label=\"%s\"]", from, to.toStates().first, guard);
	}

	@Override
	public String toString() {
		return String.format("S: %s --> %s [%s] [type=∃]", from, to.toStates().first, guard);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof EpsSAFAInputMove<?, ?>) {
			EpsSAFAInputMove<?, ?> otherCasted = (EpsSAFAInputMove<?, ?>) other;
			return otherCasted.from.equals(from) && otherCasted.to.equals(to) && otherCasted.guard.equals(guard);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to);
	}

	@Override
	public boolean isDisjointFrom(EpsSAFAMove<P, S> t, BooleanAlgebra<P, S> ba) throws TimeoutException {
		System.out.println("Method 'isDisjointFrom' not supported in class 'EpsSAFAInputMove'");
		System.exit(-3);

		return true;
	}

	@Override
	public Object clone(){
		return new EpsSAFAInputMove<P, S>(from, to, guard, regex);
	}

	@Override
	public boolean isEpsilonTransition() {
		return false;
	}


}
