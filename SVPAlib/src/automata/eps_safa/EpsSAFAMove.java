package automata.eps_safa;

import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract Epsilon SAFA Move
 * @param <P> set of predicates over the domain S
 * @param <S> domain of the automaton alphabet
 */
public abstract class EpsSAFAMove<P, S> {

    protected Integer from;

    protected MoveType to;

    protected String regex;

    protected Integer maxState;

    protected P guard;

    /**
     * Constructs a Epsilon SAFA Transition that starts from state <code>from</code> and
     * ends at state <code>to</code>
     */
    public EpsSAFAMove(Integer from, MoveType to) {
        this.from = from;
        this.to = to;
    }

    public Set<Integer> getToStates() {
        Set<Integer> toStates = new HashSet<>();
        toStates.add(to.toStates().first);
        if (to.isUniversalMove()) {
            toStates.add(to.toStates().second.get());
        }
        return toStates;
    }

    public Integer getFrom() {
        return from;
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
    public abstract boolean isDisjointFrom(EpsSAFAMove<P, S> t,
                                           BooleanAlgebra<P, S> ba) throws TimeoutException;

    @Override
    public abstract Object clone();

    public abstract boolean isEpsilonTransition();

    public abstract S getWitness(BooleanAlgebra<P, S> boolal) throws TimeoutException;

    public abstract boolean hasModel(S el, BooleanAlgebra<P, S> ba) throws TimeoutException;

    public abstract String toDotString();

}
