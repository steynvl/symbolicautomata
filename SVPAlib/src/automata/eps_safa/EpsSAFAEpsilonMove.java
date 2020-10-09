package automata.eps_safa;

import theory.BooleanAlgebra;
import utilities.Pair;

import java.util.Objects;
import java.util.Optional;

/**
 * Epsilon move of an Epsilon SAFA
 * @param <U> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public class EpsSAFAEpsilonMove<U,S> extends EpsSAFAMove<U,S> {

    /**
     * Constructs an Epsilon SAFA Transition that starts from state <code>from</code> and ends at state
     * <code>to</code>.
     */

    public EpsSAFAEpsilonMove(Integer from, MoveType to) {
        super(from, to);
    }

    public EpsSAFAEpsilonMove(Integer from, MoveType to, String regex) {
        this(from, to);
        this.regex = regex;
    }

    @Override
    public boolean isDisjointFrom(EpsSAFAMove<U,S> t, BooleanAlgebra<U,S> ba){
        return t.from.equals(from);
    }

    @Override
    public boolean isSatisfiable(BooleanAlgebra<U, S> boolal) {
        return true;
    }

    @Override
    public String toDotString() {
        if (to.isExistentialMove()) {
            return String.format("%s -> %s [label=\"&#949;\"]\n", from, to);
        } else {
            Pair<Integer, Optional<Integer>> toStates = to.toStates();
            assert toStates.second.isPresent();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s -> %s [label=\"&#949;\"]\n", from, toStates.first));
            sb.append(String.format("%s -> %s [label=\"&#949;\"]\n", from, toStates.second));
            return sb.toString();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EpsSAFAEpsilonMove<?, ?>) {
            EpsSAFAEpsilonMove<?, ?> otherCasted = (EpsSAFAEpsilonMove<?, ?>) other;
            return otherCasted.from.equals(from) && otherCasted.to.equals(to);
        }

        return false;
    }

    @Override
    public String toString() {
        Pair<Integer, Optional<Integer>> toStates = to.toStates();
        assert toStates.second.isPresent();
        return String.format("E: %s --> (%s, %s) [type=∀]", from, toStates.first, toStates.second);
    }

    @Override
    public Object clone(){
        return new EpsSAFAEpsilonMove<U, S>(from, to, regex);
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
