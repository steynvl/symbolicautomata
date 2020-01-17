package automata.safa;

import automata.safa.booleanexpression.PositiveBooleanExpression;
import theory.BooleanAlgebra;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

/**
 * Epsilon move of an SAFA
 * @param <U> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public class SAFAEpsilon<U,S> extends SAFAMove<U,S> {

    /**
     * Constructs an FSA Transition that starts from state <code>from</code> and ends at state
     * <code>to</code> with input <code>input</code>
     */

    public SAFAEpsilon(Integer from, PositiveBooleanExpression to) {
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

        toStates = new HashSet<>();
        toStates.addAll(to.getStates());
    }

    @Override
    public boolean isDisjointFrom(SAFAMove<U,S> t, BooleanAlgebra<U,S> ba){
        return t.from.equals(from);
    }

    @Override
    public boolean isSatisfiable(BooleanAlgebra<U, S> boolal) {
        return true;
    }

    @Override
    public String toDotString() {
        if (to.getStates().size() == 1) {
            return String.format("%s -> %s [label=\"&#949;\"]\n", from,to);
        } else if (to.getStates().size() == 2) {
            Iterator<Integer> it = to.getStates().iterator();
            String first = String.format("%s -> %s [label=\"&#949;\"]\n", from, it.next());
            String second = String.format("%s -> %s [label=\"&#949;\"]\n", from, it.next());
            return first + second;
        } else {
            return "";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SAFAEpsilon<?, ?>) {
            SAFAEpsilon<?, ?> otherCasted = (SAFAEpsilon<?, ?>) other;
            return otherCasted.from.equals(from) && otherCasted.to.equals(to);
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("E: %s --> %s", from, to);
    }

    @Override
    public Object clone(){
        return new SAFAEpsilon<U, S>(from, to);
    }

    @Override
    public boolean isEpsilonTransition() {
        return true;
    }

    @Override
    public S getWitness(BooleanAlgebra<U, S> boolal) {
        return null;
    }

    @Override
    public boolean hasModel(S el, BooleanAlgebra<U, S> ba) {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from);
    }
}
