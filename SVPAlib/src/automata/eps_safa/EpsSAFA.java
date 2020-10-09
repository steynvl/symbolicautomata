package automata.eps_safa;

import automata.safa.SAFAInputMove;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;

import java.util.*;

public class EpsSAFA<P, S> {

    private int initialState;
    private Collection<Integer> states;
    private Collection<Integer> finalStates;
    private Collection<Integer> lookaheadFinalStates;

    protected Map<Integer, Collection<EpsSAFAInputMove<P, S>>> inputMovesFrom;
    protected Map<Integer, Collection<EpsSAFAInputMove<P, S>>> inputMovesTo;
    protected Map<Integer, Collection<EpsSAFAEpsilonMove<P, S>>> epsilonFrom;
    protected Map<Integer, Collection<EpsSAFAEpsilonMove<P, S>>> epsilonTo;

    private Integer maxStateId;
    private Integer transitionCount;

    /**
     * @return the maximum state id
     */
    public Integer getMaxStateId() {
        return maxStateId;
    }

    public void setMaxStateId(int maxStateId) {
        this.maxStateId = maxStateId;
    }

    /**
     * @return number of states in the automaton
     */
    public Integer stateCount() {
        return states.size();
    }

    /**
     * @return number of transitions in the automaton
     */
    public Integer getTransitionCount() {
        return transitionCount;
    }

    public int getInitialState() {
        return initialState;
    }

    public Collection<Integer> getStates() {
        return states;
    }

    public Collection<Integer> getFinalStates() {
        return finalStates;
    }

    public Collection<Integer> getLookaheadFinalStates() {
        return lookaheadFinalStates;
    }

    // ------------------------------------------------------
    // Constructors
    // ------------------------------------------------------

    // Initializes all the fields of the automaton
    private EpsSAFA() {
        super();
        finalStates = new HashSet<>();
        lookaheadFinalStates = new HashSet<>();
        states = new HashSet<>();
        inputMovesFrom = new HashMap<>();
        inputMovesTo = new HashMap<>();
        epsilonFrom = new HashMap<>();
        epsilonTo = new HashMap<>();
        transitionCount = 0;
        maxStateId = 0;
    }

    /*
     * Create an automaton and removes unreachable states and only removes
     * unreachable states if remUnreachableStates is true and normalizes the
     * automaton if normalize is true
     */
    public static <A, B> EpsSAFA<A, B> MkEpsSAFA(Collection<EpsSAFAMove<A, B>> transitions,
                                                 int initialState,
                                                 Collection<Integer> finalStates,
                                                 Collection<Integer> lookaheadFinalStates,
                                                 BooleanAlgebra<A, B> ba) throws TimeoutException {
        return MkEpsSAFA(
                transitions, initialState, finalStates, lookaheadFinalStates,
                ba, false, false, false
        );
    }

    public static <A, B> EpsSAFA<A, B> MkEpsSAFA(Collection<EpsSAFAMove<A, B>> transitions,
                                                int initialState,
                                                Collection<Integer> finalStates,
                                                BooleanAlgebra<A, B> ba) throws TimeoutException {
        return MkEpsSAFA(transitions, initialState, finalStates, new HashSet<>(), ba);
    }

    public static <A, B> EpsSAFA<A, B> MkEpsSAFA(Collection<EpsSAFAMove<A, B>> transitions,
                                                 int initialState,
                                                 Collection<Integer> finalStates,
                                                 BooleanAlgebra<A, B> ba,
                                                 boolean normalize,
                                                 boolean simplify,
                                                 boolean complete) throws TimeoutException {
        return MkEpsSAFA(
                transitions, initialState, finalStates, new HashSet<>(), ba, normalize, simplify, complete
        );
    }

    /*
     * Create an automaton and removes unreachable states and only removes
     * unreachable states if remUnreachableStates is true and normalizes the
     * automaton if normalize is true
     */
    public static <A, B> EpsSAFA<A, B> MkEpsSAFA(Collection<EpsSAFAMove<A, B>> transitions,
                                                 int initialState,
                                                 Collection<Integer> finalStates,
                                                 Collection<Integer> lookaheadFinalStates,
                                                 BooleanAlgebra<A, B> ba,
                                                 boolean normalize,
                                                 boolean simplify,
                                                 boolean complete) throws TimeoutException {
        EpsSAFA<A, B> aut = new EpsSAFA<>();

        aut.states = new HashSet<>();
        aut.states.add(initialState);
        aut.states.addAll(finalStates);
        aut.states.addAll(lookaheadFinalStates);

        aut.initialState = initialState;

        /* make sure two final state sets are mutually exclusive  */
        assert finalStates.stream().noneMatch(lookaheadFinalStates::contains);

        aut.finalStates = new HashSet<>(finalStates);
        aut.lookaheadFinalStates = new HashSet<>(lookaheadFinalStates);

        //Hack
        aut.maxStateId = 0;
        for (int state : aut.finalStates)
            aut.maxStateId = Integer.max(aut.maxStateId, state);

        for (EpsSAFAMove<A, B> t : transitions)
            aut.addTransition(t, ba, false);

        return aut;
//        if (complete && !normalize)
//            aut = aut.complete(ba);
//
//        if (simplify)
//            aut = aut.simplify(ba);
//
//        if (normalize) {
//            return aut.normalize(ba);
//        } else
//            return aut;
    }

    /* Adds a transition to the EpsSAFA */
    public void addTransition(EpsSAFAMove<P, S> transition, BooleanAlgebra<P, S> ba, boolean skipSatCheck) throws TimeoutException {
        if (transition.isSatisfiable(ba)) {
            transitionCount++;

            /* we remove the transition.isSatisfiable(ba) check to show infeasible paths in the EpsSAFA */
            if (transition.getFrom() > maxStateId)
                maxStateId = transition.getFrom();
            if (transition.maxState > maxStateId)
                maxStateId = transition.maxState;

            states.add(transition.getFrom());
            states.addAll(transition.getToStates());

            if (!transition.isEpsilonTransition()) {
                getInputMovesFrom(transition.getFrom()).add((EpsSAFAInputMove<P, S>) transition);
                for (Integer to : transition.getToStates()) {
                    getInputMovesTo(to).add((EpsSAFAInputMove<P, S>) transition);
                }
            } else {
                getEpsilonFrom(transition.getFrom()).add((EpsSAFAEpsilonMove<P, S>) transition);
                for (Integer to : transition.getToStates()) {
                    getEpsilonTo(to).add((EpsSAFAEpsilonMove<P, S>) transition);
                }
            }
        }
    }

    public Map<Integer, Collection<EpsSAFAInputMove<P, S>>> getInputMovesFrom() {
        return inputMovesFrom;
    }

    public Map<Integer, Collection<EpsSAFAInputMove<P, S>>> getInputMovesTo() {
        return inputMovesTo;
    }

    public Map<Integer, Collection<EpsSAFAEpsilonMove<P, S>>> getEpsilonFrom() {
        return epsilonFrom;
    }

    public Map<Integer, Collection<EpsSAFAEpsilonMove<P, S>>> getEpsilonTo() {
        return epsilonTo;
    }

    // ------------------------------------------------------
    // Properties accessing methods
    // ------------------------------------------------------

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAInputMove<P, S>> getInputMoves() {
        return getInputMovesFrom(states);
    }

    /**
     * Returns the set of transitions starting at state <code>s</code>
     */
    public Collection<EpsSAFAMove<P, S>> getTransitionsFrom(Integer state) {
        Collection<EpsSAFAMove<P, S>> moves = new HashSet<>();
        moves.addAll(getInputMovesFrom(state));
        moves.addAll(getEpsilonFrom(state));
        return moves;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAMove<P, S>> getTransitionsFrom(Collection<Integer> stateSet) {
        Collection<EpsSAFAMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : stateSet)
            transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAInputMove<P, S>> getInputMovesFrom(Collection<Integer> stateSet) {
        Collection<EpsSAFAInputMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : stateSet)
            transitions.addAll(getInputMovesFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<EpsSAFAEpsilonMove<P, S>> getEpsilonFrom(Integer state) {
        Collection<EpsSAFAEpsilonMove<P, S>> trset = epsilonFrom.get(state);
        if (trset == null) {
            trset = new HashSet<>();
            epsilonFrom.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAEpsilonMove<P, S>> getEpsilonFrom(Collection<Integer> stateSet) {
        Collection<EpsSAFAEpsilonMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : stateSet)
            transitions.addAll(getEpsilonFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<EpsSAFAInputMove<P, S>> getInputMovesFrom(Integer state) {
        Collection<EpsSAFAInputMove<P, S>> trset = inputMovesFrom.get(state);
        if (trset == null) {
            trset = new HashSet<>();
            inputMovesFrom.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAMove<P, S>> getMoves() {
        return getMovesFrom(getStates());
    }

    /**
     * Set of moves from set of states
     */
    public Collection<EpsSAFAMove<P, S>> getMovesFrom(Collection<Integer> states) {
        Collection<EpsSAFAMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : states)
            transitions.addAll(getMovesFrom(state));
        return transitions;
    }

    public Collection<EpsSAFAMove<P, S>> getMovesFrom(Integer state) {
        Collection<EpsSAFAMove<P, S>> transitions = new LinkedList<>();
        transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAMove<P, S>> getTransitions() {
        Collection<EpsSAFAMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : states)
            transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<EpsSAFAMove<P, S>> getTransitionsTo(Integer state) {
        Collection<EpsSAFAMove<P, S>> moves = new HashSet<>();
        moves.addAll(getInputMovesTo(state));
        moves.addAll(getEpsilonTo(state));
        return moves;
    }

    /**
     * Returns the set of transitions to a set of states
     */
    public Collection<EpsSAFAMove<P, S>> getTransitionsTo(Collection<Integer> stateSet) {
        Collection<EpsSAFAMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : stateSet)
            transitions.addAll(getTransitionsTo(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<EpsSAFAEpsilonMove<P, S>> getEpsilonTo(Integer state) {
        Collection<EpsSAFAEpsilonMove<P, S>> trset = epsilonTo.get(state);
        if (trset == null) {
            trset = new HashSet<>();
            epsilonTo.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<EpsSAFAInputMove<P, S>> getInputMovesTo(Integer state) {
        Collection<EpsSAFAInputMove<P, S>> trset = inputMovesTo.get(state);
        if (trset == null) {
            trset = new HashSet<>();
            inputMovesTo.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<EpsSAFAInputMove<P, S>> getInputMovesTo(Collection<Integer> stateSet) {
        Collection<EpsSAFAInputMove<P, S>> transitions = new LinkedList<>();
        for (Integer state : stateSet)
            transitions.addAll(getInputMovesTo(state));
        return transitions;
    }

    @Override
    public Object clone() {
        EpsSAFA<P, S> cl = new EpsSAFA<>();

        cl.maxStateId = maxStateId;
        cl.transitionCount = transitionCount;

        cl.states = new HashSet<>(states);
        cl.initialState = initialState;
        cl.finalStates = new HashSet<>(finalStates);
        cl.lookaheadFinalStates = new HashSet<>(lookaheadFinalStates);

        cl.inputMovesFrom = new HashMap<>(inputMovesFrom);

        return cl;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String s = "Automaton: " + getTransitionCount() + " transitions, " + stateCount() + " states" + "\n";
        s += "Transitions \n";
        for (EpsSAFAMove<P, S> t : getMoves())
            s = s + t + "\n";

        s += "Initial State \n";
        s = s + initialState + "\n";

        s += "Final States \n";
        for (Integer fs : finalStates)
            s = s + fs + "\n";

        s += "Lookahead Final States \n";
        for (Integer lfs : lookaheadFinalStates)
            s = s + lfs + "\n";

        return s;
    }

    /**
     * If <code>state<code> belongs to reached returns reached(state) otherwise
     * add state to reached and to toVisit and return corresponding id
     */
    public static <A, B> int getStateId(A state, Map<A, Integer> reached, LinkedList<A> toVisit) {
        if (!reached.containsKey(state)) {
            int newId = reached.size();
            reached.put(state, newId);
            toVisit.add(state);
            return newId;
        } else
            return reached.get(state);
    }

}
