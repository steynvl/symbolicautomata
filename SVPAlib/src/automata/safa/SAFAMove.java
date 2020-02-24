package automata.safa;

import automata.safa.booleanexpression.PositiveBooleanExpression;
import theory.BooleanAlgebra;

import org.sat4j.specs.TimeoutException;

import java.util.Set;

/**
 * Abstract SAFA Move
 * @param <P> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public abstract class SAFAMove<P, S> {

    public Integer from;

    public PositiveBooleanExpression to;

    public Set<Integer> toStates;
    public int maxState;

    public String regex;

    public P guard;

    /**
     * Constructs an FSA Transition that starts from state <code>from</code> and
     * ends at state <code>to</code> with input <code>input</code>
     */
    public SAFAMove(Integer from, PositiveBooleanExpression to) {
        this.from = from;
        this.to = to;
    }

    /**
     * @return whether the transition can ever be enabled
     * @throws TimeoutException
     */
    public abstract boolean isSatisfiable(BooleanAlgebra<P, S> ba) throws TimeoutException;

    /**
     * Checks if the move is disjoint from the move <code>t</code> (they are not from same state on same predicate)
     * @throws TimeoutException
     */
    public abstract boolean isDisjointFrom(SAFAMove<P, S> t,
                                           BooleanAlgebra<P, S> ba) throws TimeoutException;

    @Override
    public abstract Object clone();

    public abstract boolean isEpsilonTransition();

    public abstract S getWitness(BooleanAlgebra<P, S> boolal) throws TimeoutException;

    public abstract boolean hasModel(S el, BooleanAlgebra<P, S> ba) throws TimeoutException;

    public abstract String toDotString();

}
