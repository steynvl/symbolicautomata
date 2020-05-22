/**
 * SVPAlib
 * automata.sfa
 * Apr 21, 2015
 *
 * @author Loris D'Antoni
 */
package automata.safa;

import java.util.*;
import java.util.stream.Collectors;

import automata.safa.booleanexpression.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Triple;
import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.BooleanAlgebra;
import utilities.Pair;
import utilities.Timers;
import utilities.UnionFindHopKarp;

/**
 * Symbolic finite automaton
 *
 * @param
 * 			<P>
 *            set of predicates over the domain S
 * @param <S>
 *            domain of the automaton alphabet
 */
public class SAFA<P, S> {

    // ------------------------------------------------------
    // Automata properties
    // ------------------------------------------------------

    private static BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = null;

    private PositiveBooleanExpression initialState;
    private Collection<Integer> states;
    private Collection<Integer> finalStates;
    private Collection<Integer> lookaheadFinalStates;

    protected Map<Integer, Collection<SAFAInputMove<P, S>>> inputMovesFrom;
    protected Map<Integer, Collection<SAFAInputMove<P, S>>> inputMovesTo;
    protected Map<Integer, Collection<SAFAEpsilon<P, S>>> epsilonFrom;
    protected Map<Integer, Collection<SAFAEpsilon<P, S>>> epsilonTo;

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

    public PositiveBooleanExpression getInitialState() {
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

    public static BooleanExpressionFactory<PositiveBooleanExpression> getBooleanExpressionFactory() {
        if (boolexpr == null) {
            boolexpr = new PositiveBooleanExpressionFactory();
        }
        return boolexpr;
    }

    // ------------------------------------------------------
    // Constructors
    // ------------------------------------------------------

    // Initializes all the fields of the automaton
    private SAFA() {
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
    public static <A, B> SAFA<A, B> MkSAFA(Collection<SAFAMove<A, B>> transitions,
                                           PositiveBooleanExpression initialState,
                                           Collection<Integer> finalStates,
                                           Collection<Integer> lookaheadFinalStates,
                                           BooleanAlgebra<A, B> ba) throws TimeoutException {
        return MkSAFA(
                transitions, initialState, finalStates, lookaheadFinalStates,
                ba, false, false, false
        );
    }

    public static <A, B> SAFA<A, B> MkSAFA(Collection<SAFAMove<A, B>> transitions,
                                           PositiveBooleanExpression initialState,
                                           Collection<Integer> finalStates,
                                           BooleanAlgebra<A, B> ba) throws TimeoutException {
        return MkSAFA(transitions, initialState, finalStates, new HashSet<>(), ba);
    }

    public static <A, B> SAFA<A, B> MkSAFA(Collection<SAFAMove<A, B>> transitions,
                                           PositiveBooleanExpression initialState,
                                           Collection<Integer> finalStates,
                                           BooleanAlgebra<A, B> ba,
                                           boolean normalize,
                                           boolean simplify,
                                           boolean complete) throws TimeoutException {
        return MkSAFA(
                transitions, initialState, finalStates, new HashSet<>(), ba, normalize, simplify, complete
        );
    }

    /*
     * Create an automaton and removes unreachable states and only removes
     * unreachable states if remUnreachableStates is true and normalizes the
     * automaton if normalize is true
     */
    public static <A, B> SAFA<A, B> MkSAFA(Collection<SAFAMove<A, B>> transitions,
                                           PositiveBooleanExpression initialState,
                                           Collection<Integer> finalStates,
                                           Collection<Integer> lookaheadFinalStates,
                                           BooleanAlgebra<A, B> ba,
                                           boolean normalize,
                                           boolean simplify,
                                           boolean complete) throws TimeoutException {

        SAFA<A, B> aut = new SAFA<>();

        aut.states = new HashSet<>();
        aut.states.addAll(initialState.getStates());
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

        for (SAFAMove<A, B> t : transitions)
            aut.addTransition(t, ba, false);

        if (complete && !normalize)
            aut = aut.complete(ba);

        if (simplify)
            aut = aut.simplify(ba);

        if (normalize) {
            return aut.normalize(ba);
        } else
            return aut;
    }

    // Adds a transition to the SAFA
    public void addTransition(SAFAMove<P, S> transition, BooleanAlgebra<P, S> ba, boolean skipSatCheck) throws TimeoutException {
        if (transition.isSatisfiable(ba)) {

            transitionCount++;

            /* we remove the transition.isSatisfiable(ba) check to show infeasible paths in the SAFA */
            if (transition.from > maxStateId)
                maxStateId = transition.from;
            if (transition.maxState > maxStateId)
                maxStateId = transition.maxState;

            states.add(transition.from);
            states.addAll(transition.to.getStates());

            if (!transition.isEpsilonTransition()) {
                getInputMovesFrom(transition.from).add((SAFAInputMove<P, S>) transition);
                for (Integer to : transition.to.getStates()) {
                    getInputMovesTo(to).add((SAFAInputMove<P, S>) transition);
                }
            } else {
                getEpsilonFrom(transition.from).add((SAFAEpsilon<P, S>) transition);
                for (Integer to : transition.to.getStates()) {
                    getEpsilonTo(to).add((SAFAEpsilon<P, S>) transition);
                }
            }
        }
    }

    public Map<Integer, Collection<SAFAInputMove<P, S>>> getInputMovesFrom() {
        return inputMovesFrom;
    }

    public Map<Integer, Collection<SAFAInputMove<P, S>>> getInputMovesTo() {
        return inputMovesTo;
    }

    public Map<Integer, Collection<SAFAEpsilon<P, S>>> getEpsilonFrom() {
        return epsilonFrom;
    }

    public Map<Integer, Collection<SAFAEpsilon<P, S>>> getEpsilonTo() {
        return epsilonTo;
    }

    /**
     * Computes the union of <code>aut1</code> and <code>aut2</code> as a new
     * SAFA
     *
     * @throws TimeoutException
     */
    public static <A, B> SAFA<A, B> union(SAFA<A, B> aut1,
                                          SAFA<A, B> aut2,
                                          BooleanAlgebra<A, B> ba) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        // if (aut1.isEmpty && aut2.isEmpty) TODO
        // return getEmptySAFA(ba); TODO

        // components of new SAFA
        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();
        Integer initialState, finalState;
        Collection<Integer> finalStates = new HashSet<>();
        Collection<Integer> lookaheadFinalStates = new HashSet<>();

        // Offset will be add to all states of aut2
        // to ensure that the states of aut1 and aut2 are disjoint
        int offSet = aut1.maxStateId + 2;

        BooleanExpressionMorphism<PositiveBooleanExpression> bem = new BooleanExpressionMorphism<>(
                (x) -> boolExpr.MkState(x + offSet), boolExpr
        );

        // Copy the moves of aut1 in transitions
        for (SAFAMove<A, B> t : aut1.getTransitions()) {
            @SuppressWarnings("unchecked")
            SAFAMove<A, B> newMove = (SAFAMove<A, B>) t.clone();
            transitions.add(newMove);
        }

        // Copy the moves of aut2 in transitions
        // and shift the states by offset
        for (SAFAMove<A, B> t : aut2.getTransitions()) {
            @SuppressWarnings("unchecked")
            SAFAMove<A, B> newMove = (SAFAMove<A, B>) t.clone();

            newMove.from += offSet;

            newMove.to = bem.apply(newMove.to);
            transitions.add(newMove);
        }

        // the new initial state is the first available id
        initialState = aut2.maxStateId + offSet + 1;

        // Add transitions from new initial state to
        // the the initial state of aut1 and
        // the initial state of aut2 shifted by offset
        transitions.add(new SAFAEpsilon<>(initialState, aut1.initialState));
        transitions.add(new SAFAEpsilon<>(initialState, bem.apply(aut2.initialState)));

        // the new final state
        finalState = aut2.maxStateId + offSet + 2;
        finalStates.add(finalState);

        for (Integer fs : aut1.finalStates) {
            transitions.add(new SAFAEpsilon<>(fs, boolExpr.MkState(finalState)));
        }

        for (Integer fs : aut2.finalStates) {
            transitions.add(new SAFAEpsilon<>(fs + offSet, boolExpr.MkState(finalState)));
        }

        /* do the same for the lookahead final states */
        lookaheadFinalStates.addAll(aut1.lookaheadFinalStates);
        for (Integer state : aut2.lookaheadFinalStates)
            lookaheadFinalStates.add(state + offSet);

        return MkSAFA(
                transitions, boolExpr.MkState(initialState), finalStates, lookaheadFinalStates, ba
        );
    }

    /**
     * concatenates aut1 with aut2
     *
     * @throws TimeoutException
     */
    @SuppressWarnings("unchecked")
    public static <A, B> SAFA<A, B> concatenate(SAFA<A, B> aut1,
                                                SAFA<A, B> aut2,
                                                BooleanAlgebra<A, B> ba) throws TimeoutException {
        // if (aut1.isEmpty || aut2.isEmpty) TODO
        // return getEmptySAFA(ba); TODO

        if (aut2.getStates().size() == 0) {
            return aut1;
        }

        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();
        PositiveBooleanExpression initialState = aut1.initialState;
        Collection<Integer> finalStates = new HashSet<>();
        Collection<Integer> finalLookaheadStates = new HashSet<>();

        int offSet = aut1.maxStateId + 1;

        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = getBooleanExpressionFactory();
        BooleanExpressionMorphism<PositiveBooleanExpression> bem = new BooleanExpressionMorphism<>(
                (x) -> boolExpr.MkState(x + offSet), boolExpr
        );

        for (SAFAMove<A, B> t : aut1.getTransitions()) {
            transitions.add((SAFAMove<A, B>) t.clone());
        }

        for (SAFAMove<A, B> t : aut2.getTransitions()) {
            SAFAMove<A, B> newMove = (SAFAMove<A, B>) t.clone();

            newMove.from += offSet;
            newMove.to = bem.apply(newMove.to);
            transitions.add(newMove);
        }

        for (Integer state1 : aut1.finalStates) {
            PositiveBooleanExpression toState = bem.apply(aut2.initialState);
            transitions.add(new SAFAEpsilon<>(state1, toState));
        }

        for (Integer state : aut2.finalStates) {
            finalStates.add(state + offSet);
        }

        finalLookaheadStates.addAll(aut1.lookaheadFinalStates);
        for (Integer state : aut2.lookaheadFinalStates) {
            finalLookaheadStates.add(state + offSet);
        }

        return MkSAFA(transitions, initialState, finalStates, finalLookaheadStates, ba);
    }

    /**
     * language star
     *
     * @throws TimeoutException
     */
    @SuppressWarnings("unchecked")
    public static <A, B> SAFA<A, B> star(SAFA<A, B> aut, BooleanAlgebra<A, B> ba) throws TimeoutException {
        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();
        PositiveBooleanExpression initialState;
        Integer init;
        Collection<Integer> finalStates = new HashSet<>();
        Collection<Integer> lookaheadFinalStates = new HashSet<>(aut.lookaheadFinalStates);

        init = aut.maxStateId + 1;
        initialState = SAFA.getBooleanExpressionFactory().MkState(init);

        for (SAFAMove<A, B> t : aut.getTransitions())
            transitions.add((SAFAMove<A, B>) t.clone());

        // add eps transition from finalStates to initial state
        for (Integer finState : aut.finalStates)
            transitions.add(new SAFAEpsilon<>(finState, initialState));

        // add eps transition from new initial state to old initial state
        assert aut.initialState instanceof PositiveId;
        transitions.add(new SAFAEpsilon<>(init, aut.initialState));

        // The only final state is the new initial state
        finalStates.add(init);

        return MkSAFA(transitions, initialState, finalStates, lookaheadFinalStates, ba);
    }

    /**
     * Performs a positive lookahead
     *
     * @throws TimeoutException
     */
    @SuppressWarnings("unchecked")
    public static <A, B> SAFA<A, B> positiveLookAhead(SAFA<A, B> aut,
                                                      BooleanAlgebra<A, B> ba) throws TimeoutException {
        /* TODO handle empty case */

        /* components of new SFA */
        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();
        Collection<Integer> finalStates = new HashSet<>();
        Collection<Integer> lookaheadFinalStates = new HashSet<>(aut.lookaheadFinalStates);
        int initial = aut.maxStateId + 1;
        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();
        PositiveBooleanExpression initialState = pb.MkState(initial);
        int afterLookAhead = aut.maxStateId + 2;

        /* copy all transitions */
        for (SAFAMove<A, B> t : aut.getTransitions())
            transitions.add((SAFAMove<A, B>) t.clone());

        /* make "and" node in SAFA */
        PositiveBooleanExpression andNode = pb.MkAnd(
                aut.initialState, SAFA.getBooleanExpressionFactory().MkState(afterLookAhead)
        );
        transitions.add(new SAFAEpsilon<>(initial, andNode));

        finalStates.add(afterLookAhead);

        lookaheadFinalStates.addAll(aut.finalStates);

        return MkSAFA(transitions, initialState, finalStates, lookaheadFinalStates, ba);
    }

    public static <A, B> SAFA<A, B> endAnchor(BooleanAlgebra<A, B> ba) throws TimeoutException {
        /* TODO handle empty case */

        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

        /* (?!.) */
        Collection<SAFAMove<A, B>> lookaheadTransitions = new ArrayList<>();
        PositiveBooleanExpression lookaheadInitial = pb.MkState(0);
        Collection<Integer> lookaheadFinal = new HashSet<>();
        lookaheadFinal.add(0);
//        lookaheadTransitions.add(new SAFAInputMove<>(0, pb.MkState(1), ba.True()));
//        lookaheadTransitions.add(new SAFAInputMove<>(0, pb.MkState(1), ba.True()));

        SAFA<A, B> aut = MkSAFA(lookaheadTransitions, lookaheadInitial, lookaheadFinal, new HashSet<>(), ba);

        return aut;

//        return positiveLookAhead(aut, ba);
    }

    public static <A, B> SAFA<A, B> dot(BooleanAlgebra<A, B> booleanAlgebra) throws TimeoutException {
        // make a SAFA that has a transition which accepts TRUE
        Collection<SAFAMove<A, B>> transitions = new LinkedList<>();

        PositiveBooleanExpression initial = SAFA.getBooleanExpressionFactory().MkState(0);
        PositiveBooleanExpression to = SAFA.getBooleanExpressionFactory().MkState(1);

        transitions.add(new SAFAInputMove<>(0, to, booleanAlgebra.True()));
        Collection<Integer> finalStates = new LinkedList<>();
        finalStates.add(1);

        return SAFA.MkSAFA(transitions, initial, finalStates, new HashSet<>(), booleanAlgebra);
    }

    // ------------------------------------------------------
    // Constant automata
    // ------------------------------------------------------

    /**
     * Returns the empty SFA for the Boolean algebra <code>ba</code>
     */
    public static <A, B> SAFA<A, B> getEmptySAFA(BooleanAlgebra<A, B> ba) {
        SAFA<A, B> aut = new SAFA<>();
        BooleanExpressionFactory<PositiveBooleanExpression> bexpr = getBooleanExpressionFactory();
        aut.initialState = bexpr.False();
        return aut;
    }

    /**
     * Returns the true SFA for the Boolean algebra <code>ba</code>
     */
    public static <A, B> SAFA<A, B> getFullSAFA(BooleanAlgebra<A, B> ba) {
        SAFA<A, B> aut = new SAFA<>();
        BooleanExpressionFactory<PositiveBooleanExpression> bexpr = getBooleanExpressionFactory();
        aut.initialState = bexpr.True();
        return aut;
    }

    // ------------------------------------------------------
    // Runnable operations
    // ------------------------------------------------------

    /**
     * Returns true if the SAFA accepts the input list
     *
     * @param input
     * @param ba
     * @return true if accepted false otherwise
     * @throws TimeoutException
     */
    public boolean accepts(List<S> input, BooleanAlgebra<P, S> ba) throws TimeoutException {
        List<S> revInput = Lists.reverse(input);

        Set<Integer> currConf = new HashSet<>(finalStates);
        currConf.addAll(lookaheadFinalStates);

        currConf = getBackwardsEpsilonClosure(currConf, ba);

        for (S el : revInput) {
            currConf = getPrevState(currConf, el, ba);
            currConf = getBackwardsEpsilonClosure(currConf, ba);
        }

        return initialState.hasModel(currConf);
    }

    private Set<Integer> getPrevState(Set<Integer> currState, S inputElement,
                                      BooleanAlgebra<P, S> ba) throws TimeoutException {
        Set<Integer> prevStates = new HashSet<>();

        for (SAFAInputMove<P, S> t : getInputMoves()) {
            BooleanExpression b = t.to;
            if (b.hasModel(currState) && ba.HasModel(t.guard, inputElement)) {
                prevStates.add(t.from);
            }
        }

        return prevStates;
    }

    private Set<Integer> getBackwardsEpsilonClosure(Set<Integer> currState,
                                                    BooleanAlgebra<P, S> ba) throws TimeoutException {
        Set<Integer> prevStates = new HashSet<>(currState);
        LinkedList<Integer> toVisit = new LinkedList<>(currState);

        while (toVisit.size() > 0) {
            Integer state = toVisit.removeFirst();

            for (SAFAEpsilon<P, S> toMove : getEpsilonTo(state)) {
                if (!prevStates.contains(toMove.from) && toMove.to.hasModel(prevStates)) {
                    prevStates.add(toMove.from);
                    toVisit.add(toMove.from);
                }
            }
        }

        return prevStates;
    }

    // ------------------------------------------------------
    // Reachability methods
    // ------------------------------------------------------

    // creates a new SAFA where all unreachable or dead states have been removed
    private static <A, B> SAFA<A, B> removeDeadOrUnreachableStates(SAFA<A, B> aut, BooleanAlgebra<A, B> ba)
            throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();
        // components of new SAFA
        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();

        Set<Integer> allFinalStates = new HashSet<>(aut.finalStates);
        allFinalStates.addAll(aut.lookaheadFinalStates);

        Set<Integer> initStates = aut.initialState.getStates();

        Set<Integer> reachableFromInit = aut.getReachableStatesFrom(initStates);
        Set<Integer> reachingFinal = aut.getReachingStates(allFinalStates);

        Collection<Integer> aliveStates = new HashSet<>();

        // Computes states that reachable from initial state and can reach a
        // final state
        for (Integer state : reachableFromInit)
            if (reachingFinal.contains(state)) {
                aliveStates.add(state);
            }

        if (aliveStates.size() == 0)
            return getEmptySAFA(ba);

        for (Integer state : aliveStates)
            for (SAFAMove<A, B> t : aut.getTransitionsFrom(state)) {

                if (t.to instanceof PositiveAnd || t.to instanceof PositiveOr) {
                    transitions.add(t);
                } else if (t.to instanceof PositiveId) {
                    Integer to = ((PositiveId) t.to).state;
                    if (aliveStates.contains(to))
                        transitions.add(t);
                }
            }

        Collection<Integer> finalStates = new HashSet<>();
        Collection<Integer> lookaheadFinalStates = new HashSet<>();

        for (Integer state : allFinalStates)
            if (aliveStates.contains(state))
                if (aut.finalStates.contains(state))
                    finalStates.add(state);
                else if (aut.lookaheadFinalStates.contains(state))
                    lookaheadFinalStates.add(state);

        return MkSAFA(
                transitions, aut.initialState, finalStates, lookaheadFinalStates,
                ba, false, false, false
        );
    }

    // Computes states that reachable from states
    private Set<Integer> getReachableStatesFrom(Set<Integer> states) {
        HashSet<Integer> result = new HashSet<Integer>();
        for (Integer state : states)
            visitForward(state, result);
        return result;
    }

    // Computes states that can reach states
    private Set<Integer> getReachingStates(Set<Integer> states) {
        HashSet<Integer> result = new HashSet<Integer>();
        for (Integer state : states)
            visitBackward(state, result);
        return result;
    }

    // DFS accumulates in reached
    private void visitForward(Integer state, HashSet<Integer> reached) {
        if (!reached.contains(state)) {
            reached.add(state);
            for (SAFAMove<P, S> t : this.getTransitionsFrom(state)) {
                t.to.getStates().forEach(s -> visitForward(s, reached));
            }
        }
    }

    // backward DFS accumulates in reached
    private void visitBackward(Integer state, HashSet<Integer> reached) {
        if (!reached.contains(state)) {
            reached.add(state);
            for (SAFAMove<P, S> t : this.getTransitionsTo(state)) {
                Integer predState = t.from;
                visitBackward(predState, reached);
            }
        }
    }


    class Distance extends BooleanExpressionFactory<Integer> {
        public int[] distance;

        public Distance(int size) {
            distance = new int[size];
            for (int s = 0; s < size; s++) {
                distance[s] = size + 1;
            }
        }

        public Integer MkAnd(Integer p, Integer q) {
            return (p > q) ? p : q;
        }

        public Integer MkOr(Integer p, Integer q) {
            return (p > q) ? q : p;
        }

        public Integer True() {
            return 0;
        }

        public Integer False() {
            return distance.length;
        }

        public Integer MkState(int i) {
            return distance[i];
        }

        public boolean setDistance(int state, int d) {
            if (d < distance[state]) {
                distance[state] = d;
                return true;
            } else {
                return false;
            }
        }

        public int getDistance(int state) {
            return distance[state];
        }
    }

    // The "distance" of a state s is an under-approximation of the shortest length of a word accepted from s.
    // If the distance of s is > maxStateId then no accepting configurations are reachable from s.
    private Distance computeDistances() {
        Distance distance = new Distance(maxStateId + 1);
        for (Integer s : finalStates) {
            distance.setDistance(s, 0);
        }
        boolean changed;
        do {
            changed = false;
            for (Integer s : getStates()) {
                for (SAFAInputMove<P, S> tr : getInputMovesFrom(s)) {
                    BooleanExpressionMorphism<Integer> formulaDistance = new BooleanExpressionMorphism<>((st) -> distance.getDistance(st), distance);
                    changed = distance.setDistance(s, 1 + formulaDistance.apply(tr.to)) || changed;
                }
            }
        } while (changed);
        return distance;
    }

    public SAFA<P, S> simplify(BooleanAlgebra<P, S> ba) throws TimeoutException {
        Distance distance = computeDistances();
        BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = getBooleanExpressionFactory();

        // Replace rejecting states with False
        BooleanExpressionMorphism<PositiveBooleanExpression> simplify =
                new BooleanExpressionMorphism<>((s) -> distance.getDistance(s) > maxStateId + 1 ? boolexpr.False() : boolexpr.MkState(s), boolexpr);

        Collection<SAFAMove<P, S>> transitions = new LinkedList<>();

        // Over-approximate set of states that are reachable from the initial configuration & may reach an accepting configuration
        // Collect states & simplified transitions into a new automaton.
        PositiveBooleanExpression initial = simplify.apply(initialState);
        Collection<Integer> states = new TreeSet<Integer>(); // reachable states
        Collection<Integer> worklist = new TreeSet<Integer>();
        worklist.addAll(initial.getStates());
        while (!worklist.isEmpty()) {
            int s = worklist.iterator().next();
            worklist.remove(s);
            states.add(s);
            for (SAFAMove<P, S> tr : getMovesFrom(s)) {
                PositiveBooleanExpression postState = simplify.apply(tr.to);
                if (!postState.equals(boolexpr.False())) {
                    transitions.add(new SAFAInputMove<P, S>(s, postState, tr.guard));
                    for (Integer succ : postState.getStates()) {
                        if (!states.contains(succ)) {
                            worklist.add(succ);
                        }
                    }
                }
            }
        }

        // final states are the reachable states
        Collection<Integer> finalStates = new TreeSet<>();
        for (Integer s : this.finalStates) {
            if (states.contains(s)) {
                finalStates.add(s);
            }
        }

        Collection<Integer> lookaheadFinalStates = new TreeSet<>();
        for (Integer s : this.lookaheadFinalStates) {
            if (states.contains(s)) {
                lookaheadFinalStates.add(s);
            }
        }

        return MkSAFA(
                transitions, initialState, finalStates, lookaheadFinalStates, ba,
                false, false, false
        );
    }

    // /**
    // * Return a list [<g1, t1>, ..., <gn, tn>] of <guard, transition table>
    // * pairs such that: - For each i and each state s, s transitions to ti[s]
    // on
    // * reading a letter satisfying gi - {g1, ..., gn} is the set of all
    // * satisfiable conjunctions of guards on outgoing transitions leaving the
    // * input set of states
    // *
    // * @param states
    // * The states from which to compute the outgoing transitions
    // * @param ba
    // * @param guard
    // * All transitions in the list must comply with guard
    // * @return
    // */
    // private <E extends BooleanExpression> LinkedList<Pair<P, Map<Integer,
    // E>>> getTransitionTablesFrom(
    // Collection<Integer> states, BooleanAlgebra<P, S> ba, P guard,
    // BooleanExpressionFactory<E> tgt) {
    // LinkedList<Pair<P, Map<Integer, E>>> moves = new LinkedList<>();
    //
    // BooleanExpressionMorphism<E> coerce = new BooleanExpressionMorphism<>((x)
    // -> tgt.MkState(x), tgt);
    // moves.add(new Pair<P, Map<Integer, E>>(guard, new HashMap<>()));
    // for (Integer s : states) {
    // LinkedList<Pair<P, Map<Integer, E>>> moves2 = new LinkedList<>();
    // for (SAFAInputMove<P, S> t : getInputMovesFrom(s)) {
    // for (Pair<P, Map<Integer, E>> move : moves) {
    // P newGuard = ba.MkAnd(t.guard, move.getFirst());
    // if (ba.IsSatisfiable(newGuard)) {
    // Map<Integer, E> map = new HashMap<Integer, E>(move.getSecond());
    // map.put(s, coerce.apply(t.to));
    // moves2.add(new Pair<>(newGuard, map));
    // }
    // }
    // }
    // moves = moves2;
    // }
    // return moves;
    // }

    /**
     * Checks whether the SAFA aut is empty
     *
     * @throws TimeoutException
     */
    public static <P, S, E extends BooleanExpression> boolean isEmpty(SAFA<P, S> aut, BooleanAlgebra<P, S> ba)
            throws TimeoutException {
        // TODO: the default boolean expression factory should *not* be
        // boolexpr.
        return isEmpty(aut, ba, Long.MAX_VALUE);
    }

    /**
     * Checks whether the SAFA aut is empty
     *
     * @throws TimeoutException
     */
    public static <P, S, E extends BooleanExpression> boolean isEmpty(SAFA<P, S> aut, BooleanAlgebra<P, S> ba,
                                                                      long timeout) throws TimeoutException {
        // TODO: the default boolean expression factory should *not* be
        // boolexpr.
        BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = getBooleanExpressionFactory();
        return isEquivalent(aut, getEmptySAFA(ba), ba, boolexpr, timeout).getFirst();
    }

    /**
     * Checks whether laut and raut are equivalent using bisimulation up to
     * congruence.
     *
     * @throws TimeoutException
     */
    public static <P, S, E extends BooleanExpression> Pair<Boolean, List<S>> isEquivalent(SAFA<P, S> laut,
                                                                                          SAFA<P, S> raut, BooleanAlgebra<P, S> ba, BooleanExpressionFactory<E> boolexpr) throws TimeoutException {
        return isEquivalent(laut, raut, ba, boolexpr, Long.MAX_VALUE);
    }

    /**
     * Checks whether laut and raut are equivalent using bisimulation up to
     * congruence.
     */
    public static <P, S, E extends BooleanExpression> Pair<Boolean, List<S>>
    checkEquivalenceOfTwoConfigurations(
            SAFA<P, S> aut,
            PositiveBooleanExpression c1,
            PositiveBooleanExpression c2,
            BooleanAlgebra<P, S> ba, BooleanExpressionFactory<E> boolexpr, long timeout)
            throws TimeoutException {
        Timers.setForCongruence();
        Timers.startFull();
        Timers.setTimeout(timeout);

        SAFARelation similar = new SATRelation();

        PriorityQueue<Pair<Pair<E, E>, List<S>>> worklist = new PriorityQueue<>(new RelationComparator<>());

        BooleanExpressionMorphism<E> coerce = new BooleanExpressionMorphism<>((x) -> boolexpr.MkState(x), boolexpr);
        E leftInitial = coerce.apply(c1);
        E rightInitial = coerce.apply(c2);

        similar.add(leftInitial, rightInitial);
        worklist.add(new Pair<>(new Pair<>(leftInitial, rightInitial), new LinkedList<>()));
        int i = 0;
        while (!worklist.isEmpty()) {
            Timers.assertFullTO(timeout);
            Timers.oneMoreState();

            Pair<Pair<E, E>, List<S>> next = worklist.remove();

            E left = next.getFirst().getFirst();
            E right = next.getFirst().getSecond();
            List<S> witness = next.getSecond();

            P guard = ba.True();
            boolean isSat = true;
            do {
                Timers.assertFullTO(timeout);

                Timers.startSolver();
                S model = ba.generateWitness(guard);
                Timers.stopSolver();

                P implicant = ba.True();
                Map<Integer, E> move = new HashMap<>();
                Set<Integer> states = new HashSet<>();
                states.addAll(left.getStates());
                states.addAll(right.getStates());

                for (Integer s : states) {
                    E succ = boolexpr.False();
//                    for (SAFAInputMove<P, S> tr : aut.getInputMovesFrom(s)) {
                    for (SAFAMove<P, S> tr : aut.getMovesFrom(s)) {
                        Timers.assertFullTO(timeout);

                        Timers.startSolver();
                        boolean hm = tr.isEpsilonTransition() || ba.HasModel(tr.guard, model);
                        Timers.stopSolver();

                        if (hm) {
                            succ = boolexpr.MkOr(succ, coerce.apply(tr.to));
                            Timers.startSolver();

                            if (tr.isEpsilonTransition()) {
                                // implicant = ba.MkAnd(implicant, ba.True());
                            } else {
                                implicant = ba.MkAnd(implicant, tr.guard);
                            }

                            Timers.stopSolver();
                        } else {
                            Timers.startSolver();
                            implicant = ba.MkAnd(implicant, ba.MkNot(tr.guard));
                            Timers.stopSolver();
                        }
                    }
                    move.put(s, succ);
                }

                Timers.startSubsumption();
                E leftSucc = boolexpr.substitute((lit) -> move.get(lit)).apply(left);
                E rightSucc = boolexpr.substitute((lit) -> move.get(lit)).apply(right);
                List<S> succWitness = new LinkedList<>();
                succWitness.addAll(witness);
                succWitness.add(model);

                Collection<Integer> finalStates = new ArrayList<>(aut.finalStates);
                finalStates.addAll(aut.lookaheadFinalStates);
                boolean checkIfDiff = leftSucc.hasModel(finalStates) != rightSucc.hasModel(finalStates);
                Timers.stopSubsumption();

                if (checkIfDiff) {
                    // leftSucc is accepting and rightSucc is rejecting or
                    // vice versa
                    Timers.stopFull();
                    return new Pair<>(false, succWitness);
                } else {
                    Timers.startSubsumption();
                    if (!similar.isMember(leftSucc, rightSucc)) {
                        if (!similar.add(leftSucc, rightSucc)) {
                            Timers.stopSubsumption();
                            Timers.stopFull();
                            return new Pair<>(false, succWitness);
                        }
                        worklist.add(new Pair<>(new Pair<>(leftSucc, rightSucc), succWitness));
                    } else {
                        Timers.oneMoreSub();
                    }
                    Timers.stopSubsumption();
                }
                Timers.startSolver();
                guard = ba.MkAnd(guard, ba.MkNot(implicant));

                isSat = ba.IsSatisfiable(guard);
                Timers.stopSolver();
            } while (isSat);
        }
        Timers.stopFull();

        return new Pair<>(true, null);
    }

    /**
     * Checks whether laut and raut are equivalent using bisimulation up to
     * congruence.
     */
    public static <P, S, E extends BooleanExpression> Pair<Boolean, List<S>> isEquivalent(SAFA<P, S> laut,
                                                                                          SAFA<P, S> raut, BooleanAlgebra<P, S> ba, BooleanExpressionFactory<E> boolexpr, long timeout)
            throws TimeoutException {
        Triple<SAFA<P, S>, PositiveBooleanExpression, PositiveBooleanExpression> triple = binaryOp(laut, raut, ba, BoolOp.Union);
        return checkEquivalenceOfTwoConfigurations(triple.getLeft(), triple.getMiddle(), triple.getRight(), ba, boolexpr, timeout);
    }

    static class RelationComparator<E extends BooleanExpression, A> implements Comparator<Pair<Pair<E, E>, List<A>>> {
        @Override
        public int compare(Pair<Pair<E, E>, List<A>> x, Pair<Pair<E, E>, List<A>> y) {
            Pair<E, E> xRel = x.first;
            List<A> xWitness = x.second;
            Pair<E, E> yRel = y.first;
            List<A> yWitness = y.second;

            int lsize = xRel.first.getSize() + xRel.second.getSize();
            int rsize = yRel.first.getSize() + yRel.second.getSize();
            if (lsize < rsize)
                return -1;
            if (rsize < lsize)
                return 1;
            return xWitness.size() - yWitness.size();
        }
    }

    /**
     * Checks whether laut and raut are equivalent using HopcroftKarp on the SFA
     * accepting the reverse language
     */
    public static <P, S> boolean areReverseEquivalent(SAFA<P, S> aut1, SAFA<P, S> aut2,
                                                      BooleanAlgebra<P, S> ba) throws TimeoutException {
        return areReverseEquivalent(aut1, aut2, ba, Long.MAX_VALUE);
    }

    /**
     * Checks whether laut and raut are equivalent using HopcroftKarp on the SFA
     * accepting the reverse language
     */
    public static <P, S> boolean areReverseEquivalent(SAFA<P, S> aut1, SAFA<P, S> aut2,
                                                      BooleanAlgebra<P, S> ba, long timeout) throws TimeoutException {

        long startTime = System.currentTimeMillis();

        UnionFindHopKarp<S> ds = new UnionFindHopKarp<>();

        HashMap<HashSet<Integer>, Integer> reached1 = new HashMap<HashSet<Integer>, Integer>();
        HashMap<HashSet<Integer>, Integer> reached2 = new HashMap<HashSet<Integer>, Integer>();

        LinkedList<Pair<HashSet<Integer>, HashSet<Integer>>> toVisit = new LinkedList<>();

        HashSet<Integer> in1 = new HashSet<Integer>(aut1.finalStates);
        HashSet<Integer> in2 = new HashSet<Integer>(aut2.finalStates);

        reached1.put(in1, 0);
        reached2.put(in2, 1);
        toVisit.add(new Pair<HashSet<Integer>, HashSet<Integer>>(in1, in2));

        ds.add(0, in1.contains(aut1.initialState));
        ds.add(1, in2.contains(aut2.initialState));
        ds.mergeSets(0, 1);

        while (!toVisit.isEmpty()) {
            if (System.currentTimeMillis() - startTime > timeout)
                throw new TimeoutException("Timeout in the equivalence check");

            Pair<HashSet<Integer>, HashSet<Integer>> curr = toVisit.removeFirst();
            HashSet<Integer> curr1 = curr.first;
            HashSet<Integer> curr2 = curr.second;

            ArrayList<SAFAInputMove<P, S>> movesToCurr1 = new ArrayList<>();
            ArrayList<P> predicatesToCurr1 = new ArrayList<>();
            ArrayList<SAFAInputMove<P, S>> movesToCurr2 = new ArrayList<>();
            ArrayList<P> predicatesToCurr2 = new ArrayList<>();

            for (SAFAInputMove<P, S> t : aut1.getInputMoves())
                if (t.to.hasModel(curr1)) {
                    movesToCurr1.add(t);
                    predicatesToCurr1.add(t.guard);
                }
            for (SAFAInputMove<P, S> t : aut2.getInputMoves())
                if (t.to.hasModel(curr2)) {
                    movesToCurr2.add(t);
                    predicatesToCurr2.add(t.guard);
                }

            Collection<Pair<P, ArrayList<Integer>>> minterms1 = ba.GetMinterms(predicatesToCurr1, timeout);
            Collection<Pair<P, ArrayList<Integer>>> minterms2 = ba.GetMinterms(predicatesToCurr2, timeout);

            for (Pair<P, ArrayList<Integer>> minterm1 : minterms1) {
                for (Pair<P, ArrayList<Integer>> minterm2 : minterms2) {
                    if (System.currentTimeMillis() - startTime > timeout)
                        throw new TimeoutException("Timeout in the equivalence check");

                    P conj = ba.MkAnd(minterm1.first, minterm2.first);
                    if (ba.IsSatisfiable(conj)) {
                        // Take from states
                        HashSet<Integer> from1 = new HashSet<>();
                        HashSet<Integer> from2 = new HashSet<>();
                        for (int i = 0; i < minterm1.second.size(); i++)
                            if (minterm1.second.get(i) == 1)
                                from1.add(movesToCurr1.get(i).from);

                        for (int i = 0; i < minterm2.second.size(); i++)
                            if (minterm2.second.get(i) == 1)
                                from2.add(movesToCurr2.get(i).from);

                        // If not in union find add them
                        Integer r1 = null, r2 = null;
                        if (!reached1.containsKey(from1)) {
                            r1 = ds.getNumberOfElements();
                            reached1.put(from1, r1);
                            ds.add(r1, aut1.initialState.hasModel(from1));
                        }
                        if (r1 == null)
                            r1 = reached1.get(from1);

                        if (!reached2.containsKey(from2)) {
                            r2 = ds.getNumberOfElements();
                            reached2.put(from2, r2);
                            ds.add(r2, aut2.initialState.hasModel(from2));
                        }
                        if (r2 == null)
                            r2 = reached2.get(from2);

                        // Check whether are in simulation relation
                        if (!ds.areInSameSet(r1, r2)) {
                            if (!ds.mergeSets(r1, r2))
                                return false;

                            toVisit.add(new Pair<HashSet<Integer>, HashSet<Integer>>(from1, from2));
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the SAFA accepts the input list
     List<Set<Integer>> cgNodes = new ArrayList<>();
     *
     * @param aut
     * @param ba
     * @return true if accepted false otherwise
     * @throws TimeoutException
     */
    public static <P, S> SFA<P, S> getReverseSFA(SAFA<P, S> aut, BooleanAlgebra<P, S> ba) throws TimeoutException {

        // components of new SFA
        Collection<SFAMove<P, S>> transitions = new ArrayList<SFAMove<P, S>>();
        Integer initialState = 0;
        Collection<Integer> finalStates = new ArrayList<Integer>();

        HashMap<HashSet<Integer>, Integer> reached = new HashMap<HashSet<Integer>, Integer>();
        LinkedList<HashSet<Integer>> toVisit = new LinkedList<HashSet<Integer>>();

        HashSet<Integer> init = new HashSet<>(aut.finalStates);
        reached.put(init, 0);
        toVisit.add(init);

        while (!toVisit.isEmpty()) {
            HashSet<Integer> currentState = toVisit.removeFirst();
            int currentStateID = reached.get(currentState);
            ArrayList<SAFAInputMove<P, S>> movesToCurr = new ArrayList<>();
            ArrayList<P> predicatesToCurr = new ArrayList<>();

            if (aut.initialState.hasModel(currentState))
                finalStates.add(currentStateID);

            for (SAFAInputMove<P, S> t : aut.getInputMoves())
                if (t.to.hasModel(currentState)) {
                    movesToCurr.add(t);
                    predicatesToCurr.add(t.guard);
                }

            Collection<Pair<P, ArrayList<Integer>>> minterms = ba.GetMinterms(predicatesToCurr);
            for (Pair<P, ArrayList<Integer>> minterm : minterms) {

                ArrayList<Integer> moveBits = minterm.second;
                HashSet<Integer> fromState = new HashSet<Integer>();
                for (int moveIndex = 0; moveIndex < moveBits.size(); moveIndex++)
                    if (moveBits.get(moveIndex) == 1)
                        fromState.add(movesToCurr.get(moveIndex).from);

                // Add new move if target state is not the empty set
                if (fromState.size() > 0) {
                    int fromSt = getStateId(fromState, reached, toVisit);
                    transitions.add(new SFAInputMove<P, S>(currentStateID, fromSt, minterm.first));
                }
            }
        }

        SFA<P, S> rev = SFA.MkSFA(transitions, initialState, finalStates, ba);
        rev.setIsDet(true);
        return rev;
    }

    // ------------------------------------------------------
    // Shuffle operation
    // ------------------------------------------------------

    /**
     * Returns an automaton that accepts the shuffle (interleaving) of
     * the languages of the given automata. Both automata are determinized,
     * if not already deterministic. Does not modify the input automata languages.
     *
     * Complexity: quadratic in number of states (if already deterministic).
     */
    public static <P, S> SAFA<P, S> shuffle(SAFA<P, S> aut1, SAFA<P, S> aut2, BooleanAlgebra<P, S> ba) throws TimeoutException {
        if (aut1.getLookaheadFinalStates().size() > 0 || aut1.getTransitions().stream().anyMatch(t -> t.to instanceof PositiveAnd)) {
            throw new UnsupportedOperationException("AFA with universal states not supported for shuffle operation!");
        } else if (aut2.getLookaheadFinalStates().size() > 0 || aut2.getTransitions().stream().anyMatch(t -> t.to instanceof PositiveAnd)) {
            throw new UnsupportedOperationException("AFA with universal states not supported for shuffle operation!");
        }

        aut1 = SAFA.removeEpsilonMovesFrom(aut1, ba);
        aut2 = SAFA.removeEpsilonMovesFrom(aut2, ba);

        int maxStateId = Math.max(aut1.maxStateId, aut2.maxStateId);

        Map<Integer, List<SAFAMove<P, S>>> transitions1 = getSortedTransitions(aut1);
        Map<Integer, List<SAFAMove<P, S>>> transitions2 = getSortedTransitions(aut2);

        Collection<Integer> finalStates = new HashSet<>();
        Collection<SAFAMove<P, S>> transitions = new ArrayList<>();

        LinkedList<StatePair> worklist = new LinkedList<>();
        Map<StatePair, StatePair> newStates = new HashMap<>();
        PositiveBooleanExpression initial = SAFA.getBooleanExpressionFactory().MkState(++maxStateId);

        StatePair p = new StatePair(initial, getState(aut1.initialState), getState(aut2.initialState));
        worklist.add(p);
        newStates.put(p, p);

        while (worklist.size() > 0) {
            p = worklist.removeFirst();

            if (aut1.finalStates.contains(p.s1) && aut2.finalStates.contains(p.s2)) {
                finalStates.add(getState(p.s));
            }

            List<SAFAMove<P, S>> t1 = transitions1.get(p.s1);
            for (int i = 0; i < t1.size(); i++) {

                for (Integer to : t1.get(i).to.getStates()) {
                    StatePair q = new StatePair(to, p.s2);
                    StatePair r = newStates.get(q);

                    if (r == null) {
                        q.s = SAFA.getBooleanExpressionFactory().MkState(++maxStateId);
                        worklist.add(q);
                        newStates.put(q, q);
                        r = q;
                    }

                    transitions.add(new SAFAInputMove<>(getState(p.s), r.s, t1.get(i).guard));
                }

            }

            List<SAFAMove<P, S>> t2 = transitions2.get(p.s2);
            for (int i = 0; i < t2.size(); i++) {
                StatePair q = new StatePair(p.s1, getState(t2.get(i).to));
                StatePair r = newStates.get(q);
                if (r == null) {
                    q.s = SAFA.getBooleanExpressionFactory().MkState(++maxStateId);
                    worklist.add(q);
                    newStates.put(q, q);
                    r = q;
                }

                transitions.add(new SAFAInputMove<>(getState(p.s), r.s, t2.get(i).guard));
            }
        }

        return removeDeadOrUnreachableStates(MkSAFA(transitions, initial, finalStates, new HashSet<>(), ba), ba);
    }

    private static <P, S> Map<Integer, List<SAFAMove<P, S>>> getSortedTransitions(SAFA<P, S> safa) {
        Collection<Integer> states = safa.getStates();

        Map<Integer, List<SAFAMove<P, S>>> sortedTransitions = new HashMap<>();
        for (Integer s : states) {
            List<SAFAMove<P, S>> moves = new ArrayList<>(safa.getTransitionsFrom(s));
            moves.sort(Comparator.comparingInt(m -> m.from));

            assert !sortedTransitions.containsKey(s);
            sortedTransitions.put(s, moves);
        }

        return sortedTransitions;
    }

    private static Integer getState(PositiveBooleanExpression pb) {
        assert pb instanceof PositiveId;
        return ((PositiveId) pb).state;
    }

    // ------------------------------------------------------
    // Boolean automata operations
    // ------------------------------------------------------

    /**
     * Computes the intersection with <code>aut</code> as a new SFA
     * @throws TimeoutException
     */
    public SAFA<P, S> intersectionWith(SAFA<P, S> aut, BooleanAlgebra<P, S> ba) throws TimeoutException {
        return binaryOp(this, aut, ba, BoolOp.Intersection).getLeft();
    }

    /**
     * Computes the intersection with <code>aut</code> as a new SFA
     * @throws TimeoutException
     */
    public Triple<SAFA<P, S>, PositiveBooleanExpression, PositiveBooleanExpression>
    intersectionWithGetConjucts(SAFA<P, S> aut, BooleanAlgebra<P, S> ba) throws TimeoutException {
        return binaryOp(this, aut, ba, BoolOp.Intersection);
    }

    /**
     * Computes the intersection with <code>aut</code> as a new SFA
     * @throws TimeoutException
     */
    public SAFA<P, S> unionWith(SAFA<P, S> aut, BooleanAlgebra<P, S> ba) throws TimeoutException {
        return binaryOp(this, aut, ba, BoolOp.Union).getLeft();
    }

    class DeMorgan extends BooleanExpressionFactory<PositiveBooleanExpression> {
        private BooleanExpressionFactory<PositiveBooleanExpression> boolexpr;

        public DeMorgan() {
            boolexpr = getBooleanExpressionFactory();
        }

        public PositiveBooleanExpression MkAnd(PositiveBooleanExpression p, PositiveBooleanExpression q) {
            return boolexpr.MkOr(p, q);
        }

        public PositiveBooleanExpression MkOr(PositiveBooleanExpression p, PositiveBooleanExpression q) {
            return boolexpr.MkAnd(p, q);
        }

        public PositiveBooleanExpression True() {
            return boolexpr.False();
        }

        public PositiveBooleanExpression False() {
            return boolexpr.True();
        }

        public PositiveBooleanExpression MkState(int i) {
            return boolexpr.MkState(i);
        }
    }

    /**
     * Computes the complement of the automaton as a new SAFA. The input
     * automaton need not be normal.
     * @throws TimeoutException
     */
    public SAFA<P, S> getUnaryPathSAFA(BooleanAlgebra<P, S> ba) throws TimeoutException {
        // DeMorganize all transitions

        Collection<SAFAMove<P, S>> transitions = new ArrayList<>();

        for (SAFAMove<P, S> t : this.getMoves())
            transitions.add(new SAFAInputMove<>(t.from, t.to, ba.True()));

        return MkSAFA(
                transitions, this.initialState, finalStates, lookaheadFinalStates, ba,
                false, false, false
        );
    }

    /**
     * Computes the complement of the automaton as a new SAFA. The input
     * automaton need not be normal.
     * @throws TimeoutException
     */
    public SAFA<P, S> negate(BooleanAlgebra<P, S> ba) throws TimeoutException {
        // DeMorganize all transitions

        if (lookaheadFinalStates.size() > 0) {
            System.out.println("Negating SAFA with universal states!");
        }

        SAFA<P, S> aut = isEpsilonFree() ? this : removeEpsilonMovesFrom(this, ba);
//		if (!isEpsilonFree()) {
//			System.out.println(aut.getDot("epsilonFree"));
//		}

        Collection<SAFAMove<P, S>> transitions = new ArrayList<>();

        BooleanExpressionMorphism<PositiveBooleanExpression> demorganize = new BooleanExpressionMorphism<PositiveBooleanExpression>(
                (x) -> boolexpr.MkState(x), new DeMorgan());
        boolean addAccept = false; // do we need to create an accept state?
        for (int state = 0; state <= aut.maxStateId; state++) {
            P residual = ba.True();
            if (aut.inputMovesFrom.containsKey(state)) {
                for (SAFAMove<P, S> transition : aut.inputMovesFrom.get(state)) {
                    transitions.add(new SAFAInputMove<>(state, demorganize.apply(transition.to), transition.guard));
                    residual = ba.MkAnd(ba.MkNot(transition.guard), residual);
                }
            }
            if (ba.IsSatisfiable(residual)) {
                transitions.add(new SAFAInputMove<>(state, boolexpr.MkState(aut.maxStateId + 1), residual));
                addAccept = true;
            }
        }

        // Negate the set of final states
        Set<Integer> nonFinal = new HashSet<>();
//		assert initialState.getStates().size() == 1;
//		Integer init = initialState.getStates().iterator().next();
        for (int state = 0; state <= aut.maxStateId; state++) {
//			if (state == init && !finalStates.contains(state)) {
//				continue;
//			}

            if (!aut.finalStates.contains(state) && !aut.lookaheadFinalStates.contains(state)) {
                nonFinal.add(state);
            }
        }

        if (addAccept) {
            nonFinal.add(aut.maxStateId + 1);
            transitions.add(new SAFAInputMove<>(aut.maxStateId + 1, boolexpr.MkState(aut.maxStateId + 1), ba.True()));
        }

        PositiveBooleanExpression notInitial = demorganize.apply(aut.initialState);

        return removeDeadOrUnreachableStates(MkSAFA(
                transitions, notInitial, nonFinal, new HashSet<>(),
                ba, false, false, false), ba
        );
    }

    public boolean isEpsilonFree() {
        for (Integer s : epsilonFrom.keySet()) {
            if (!epsilonFrom.get(s).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return an equivalent copy without epsilon moves
     * @throws TimeoutException
     */
    @SuppressWarnings("unchecked")
    public static <A, B> SAFA<A, B> removeEpsilonMovesFrom(SAFA<A, B> aut,
                                                           BooleanAlgebra<A, B> ba) throws TimeoutException {
        assert aut.initialState instanceof PositiveId;
        return removeEpsilonMovesFrom(aut, ((PositiveId) aut.initialState).state, ba);
    }

    /**
     * @return an equivalent copy without epsilon moves
     * @throws TimeoutException
     */
    @SuppressWarnings("unchecked")
    public static <A, B> SAFA<A, B> removeEpsilonMovesFrom(SAFA<A, B> aut,
                                                           int start,
                                                           BooleanAlgebra<A, B> ba) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

//		TODO
//		if (aut.isEpsilonFree)
//			return (SFA<A, B>) aut.clone();

        Collection<SAFAMove<A, B>> transitions = new ArrayList<>();
        Collection<Integer> finalStates = new ArrayList<>();
        Collection<Integer> lookaheadFinalStates = new ArrayList<>();

        HashMap<Collection<Integer>, Integer> reachedStates = new HashMap<>();
        LinkedList<Collection<Integer>> toVisitStates = new LinkedList<>();

        /* add start state */
        Collection<Integer> reachableFromInit = aut.findReachableExistentialStates(start, ba);
        reachedStates.put(reachableFromInit, start);
        toVisitStates.add(reachableFromInit);

        while (!toVisitStates.isEmpty()) {
            Collection<Integer> currState = toVisitStates.removeFirst();
            int currStateId = reachedStates.get(currState);

            for (SAFAMove<A, B> t1 : aut.getTransitionsFrom(currState)) {
                /* universal states can only have outgoing epsilon transitions given our construction */

                if (t1.isEpsilonTransition() && t1.to instanceof PositiveAnd) {
                    Collection<Integer> nextState = new HashSet<>();

                    PositiveAnd pa = (PositiveAnd) t1.to;
                    assert pa.left instanceof PositiveId && pa.right instanceof PositiveId;

                    SAFA<A, B> left = removeEpsilonMovesFrom(aut, ((PositiveId) pa.left).state, ba);

                    assert left.initialState.getStates().size() == 1;
                    int leftInit = left.initialState.getStates().iterator().next();
                    transitions.addAll(left.getTransitions()
                            .stream()
                            .filter(t -> !left.getTransitionsFrom(leftInit).contains(t))
                            .collect(Collectors.toSet()));
                    finalStates.addAll(left.finalStates);
                    lookaheadFinalStates.addAll(left.lookaheadFinalStates);

                    SAFA<A, B> right = removeEpsilonMovesFrom(aut, ((PositiveId) pa.right).state, ba);

                    assert right.initialState.getStates().size() == 1;
                    int rightInit = right.initialState.getStates().iterator().next();
                    transitions.addAll(right.getTransitions()
                            .stream()
                            .filter(t -> !right.getTransitionsFrom(rightInit).contains(t))
                            .collect(Collectors.toSet()));
                    finalStates.addAll(right.finalStates);
                    lookaheadFinalStates.addAll(right.lookaheadFinalStates);

                    for (SAFAMove<A, B> leftT : left.getTransitionsFrom(leftInit)) {
                        assert !leftT.isEpsilonTransition();

                        for (SAFAMove<A, B> rightT : right.getTransitionsFrom(rightInit)) {
                            Collection<A> pset = Arrays.asList(leftT.guard, rightT.guard);
                            PositiveBooleanExpression boolFunc = pb.MkAnd(leftT.to, rightT.to);

                            SAFAInputMove<A, B> tnew = new SAFAInputMove<>(currStateId, boolFunc, ba.MkAnd(pset));
                            transitions.add(tnew);
                            nextState.addAll(tnew.to.getStates());
                        }
                    }

                } else if (!t1.isEpsilonTransition()) {
                    /* can happen if working with epsilon free safa */
                    if (t1.to instanceof PositiveAnd || t1.to instanceof PositiveOr) {
                        t1.to.getStates().forEach(to -> {
                            reachedStates.put(Collections.singletonList(to), to);
                            toVisitStates.add(Collections.singletonList(to));
                        });

                        transitions.add(t1);
                    } else if (t1.to instanceof PositiveId) {
                        Integer to = ((PositiveId) t1.to).state;
                        Collection<Integer> nextState = aut.findReachableExistentialStates(to, ba);

                        int nextStateId;
                        if (!reachedStates.containsKey(nextState)) {
                            reachedStates.put(nextState, to);
                            toVisitStates.add(nextState);
                            nextStateId = to;
                        } else {
                            nextStateId = reachedStates.get(nextState);
                        }

                        SAFAMove<A, B> tnew = (SAFAMove<A, B>) t1.clone();
                        tnew.from = currStateId;
                        tnew.to = pb.MkState(nextStateId);

                        transitions.add(tnew);
                    }
                }
            }
        }

        for (Collection<Integer> stSet : reachedStates.keySet()) {
            for (Integer s : stSet) {
                if (aut.finalStates.contains(s)) {
                    finalStates.add(reachedStates.get(stSet));
                    break;
                } else if (aut.lookaheadFinalStates.contains(s)) {
                    lookaheadFinalStates.add(reachedStates.get(stSet));
                    break;
                }
            }
        }

//		return removeDeadOrUnreachableStates(MkSAFA(transitions, pb.MkState(start), finalStates, lookaheadFinalStates, ba), ba);
        return (MkSAFA(transitions, pb.MkState(start), finalStates, lookaheadFinalStates, ba));
    }

    protected Collection<Integer> findReachableExistentialStates(Integer state, BooleanAlgebra<P, S> ba) {
        HashSet<Integer> st = new HashSet<>();
        st.add(state);
        return findReachableExistentialStates(st, ba);
    }

    protected Collection<Integer> findReachableExistentialStates(Collection<Integer> fronteer,
                                                                 BooleanAlgebra<P, S> ba) {
        Collection<Integer> reached = new HashSet<>(fronteer);
        LinkedList<Integer> toVisit = new LinkedList<>(fronteer);

        while (toVisit.size() > 0) {
            for (SAFAMove<P, S> t : getMovesFrom(toVisit.removeFirst())) {
                if (t.isEpsilonTransition() && t.to instanceof PositiveId) {
                    Integer to = ((PositiveId) t.to).state;
                    if (!reached.contains(to)) {
                        reached.add(to);
                        toVisit.add(to);
                    }
                }
            }
        }

        return reached;
    }

    public enum BoolOp {
        Intersection, Union
    }

    /**
     * Computes the intersection with <code>aut1</code> and <code>aut2</code> as
     * a new SFA
     * @throws TimeoutException
     */
    public static <A, B> Triple<SAFA<A, B>, PositiveBooleanExpression, PositiveBooleanExpression>
    binaryOp(SAFA<A, B> aut1, SAFA<A, B> aut2, BooleanAlgebra<A, B> ba, BoolOp op) throws TimeoutException {

        int offset = aut1.maxStateId + 1;
        BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = getBooleanExpressionFactory();

        // Integer initialState = aut1.maxStateId + aut2.maxStateId + 2;
        PositiveBooleanExpression initialState = null;

        Collection<Integer> finalStates = new ArrayList<>(aut1.finalStates);
        for (int state : aut2.finalStates)
            finalStates.add(state + offset);

        Collection<Integer> lookaheadFinalStates = new ArrayList<>(aut1.lookaheadFinalStates);
        for (int state : aut2.lookaheadFinalStates)
            lookaheadFinalStates.add(state + offset);

        // Copy all transitions (with proper renaming for aut2)
        Collection<SAFAMove<A, B>> transitions = new ArrayList<>(aut1.getMoves());
        for (SAFAMove<A, B> t : aut2.getMoves())
            if (t.isEpsilonTransition())
                transitions.add(new SAFAEpsilon<>(t.from + offset, boolexpr.offset(offset).apply(t.to)));
            else
                transitions.add(new SAFAInputMove<>(t.from + offset, boolexpr.offset(offset).apply(t.to), t.guard));

        PositiveBooleanExpression liftedAut2Init = boolexpr.offset(offset).apply(aut2.initialState);
        switch (op) {
            case Union:
                initialState = boolexpr.MkOr(aut1.initialState, liftedAut2Init);
                break;

            case Intersection:
                // Add extra moves from new initial state
                initialState = boolexpr.MkAnd(aut1.initialState, liftedAut2Init);
                break;

            default:
                throw new NotImplementedException("Operation " + op + " not implemented");
        }

        return Triple.of(
                MkSAFA(transitions, initialState, finalStates, lookaheadFinalStates,
                        ba, false, false, false),
                aut1.initialState,
                liftedAut2Init);
    }

    /**
     * Normalizes the SAFA by having at most one transition for each symbol out
     * of each state
     *
     * @throws TimeoutException
     */
    public SAFA<P, S> normalize(BooleanAlgebra<P, S> ba) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = getBooleanExpressionFactory();

        // Copy all transitions (with proper renaming for aut2)
        Collection<SAFAMove<P, S>> transitions = new ArrayList<>();

        for (int state : states) {
            transitions.addAll(getEpsilonFrom(state));

            ArrayList<SAFAInputMove<P, S>> trFromState = new ArrayList<>(getInputMovesFrom(state));
            ArrayList<P> predicates = new ArrayList<>();
            for (SAFAInputMove<P, S> t : trFromState) {
                predicates.add(t.guard);
            }

            Collection<Pair<P, ArrayList<Integer>>> minterms = ba.GetMinterms(predicates);
            for (Pair<P, ArrayList<Integer>> minterm : minterms) {
                PositiveBooleanExpression newTo = null;

                for (int i = 0; i < minterm.second.size(); i++)
                    if (minterm.second.get(i) == 1)
                        if (newTo == null)
                            newTo = trFromState.get(i).to;
                        else
                            newTo = boolexpr.MkOr(newTo, trFromState.get(i).to);

                if (newTo != null) {
                    transitions.add(new SAFAInputMove<>(state, newTo, minterm.first));
                } else {
                    transitions.add(new SAFAInputMove<>(state, boolexpr.False(), minterm.first));
                }
            }
        }

        return MkSAFA(
                transitions, initialState, finalStates, lookaheadFinalStates,
                ba, false, false, false
        );
    }

    /**
     * Normalizes the SAFA by having at most one transition for each symbol out
     * of each state
     * @throws TimeoutException
     */
    public SAFA<P, S> complete(BooleanAlgebra<P, S> ba) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = getBooleanExpressionFactory();

        // Copy all transitions (with proper renaming for aut2)
        Collection<SAFAMove<P, S>> transitions = new ArrayList<>(getMoves());

        boolean addedSink = false;
        int sink = maxStateId + 1;
        for (int state : states) {
            ArrayList<SAFAInputMove<P, S>> trFromState = new ArrayList<>(getInputMovesFrom(state));
            P not = ba.True();
            for (SAFAInputMove<P, S> t : trFromState) {
                not = ba.MkAnd(not, ba.MkNot(t.guard));
            }

            if (ba.IsSatisfiable(not)) {
                transitions.add(new SAFAInputMove<>(state, boolexpr.MkState(sink), not));
                addedSink = true;
            }
        }
        if (addedSink)
            transitions.add(new SAFAInputMove<>(sink, boolexpr.MkState(sink), ba.True()));

        return MkSAFA(transitions, initialState, finalStates, lookaheadFinalStates, ba, false, false, false);
    }

    public String getDot(String name) {
        Set<Integer> andNodes = new HashSet<>();
        for (Integer state : getStates()) {
            for (SAFAMove<P, S> t : getMovesFrom(state)) {
                if (t.to instanceof PositiveAnd) {
                    andNodes.add(t.from);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph " + name + "{\n rankdir=LR;\n");
        for (Integer state : getStates()) {

            sb.append(state + "[label=" + state);
            if (andNodes.contains(state)) {
                sb.append(",shape=square");
            }

            if (getFinalStates().contains(state) || getLookaheadFinalStates().contains(state))
                sb.append(",peripheries=2");

            sb.append("]\n");
//			assert getInitialState().getStates().size() == 1;
            if (state.equals(getInitialState().getStates().iterator().next()))
                sb.append("XX" + state + " [color=white, label=\"\"]");
        }

        Integer initial = getInitialState().getStates().iterator().next();
        sb.append("XX" + initial + " -> " + initial + "\n");

        for (Integer state : getStates()) {
            for (SAFAMove<P, S> t : getMovesFrom(state)) {
                sb.append(t.toDotString());
            }
        }

        sb.append("}");

        return sb.toString();
    }

    // ------------------------------------------------------
    // Properties accessing methods
    // ------------------------------------------------------

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAInputMove<P, S>> getInputMoves() {
        return getInputMovesFrom(states);
    }

    /**
     * Returns the set of transitions starting at state <code>s</code>
     */
    public Collection<SAFAMove<P, S>> getTransitionsFrom(Integer state) {
        Collection<SAFAMove<P, S>> moves = new HashSet<SAFAMove<P, S>>();
        moves.addAll(getInputMovesFrom(state));
        moves.addAll(getEpsilonFrom(state));
        return moves;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAMove<P, S>> getTransitionsFrom(Collection<Integer> stateSet) {
        Collection<SAFAMove<P, S>> transitions = new LinkedList<SAFAMove<P, S>>();
        for (Integer state : stateSet)
            transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAInputMove<P, S>> getInputMovesFrom(Collection<Integer> stateSet) {
        Collection<SAFAInputMove<P, S>> transitions = new LinkedList<SAFAInputMove<P, S>>();
        for (Integer state : stateSet)
            transitions.addAll(getInputMovesFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<SAFAEpsilon<P, S>> getEpsilonFrom(Integer state) {
        Collection<SAFAEpsilon<P, S>> trset = epsilonFrom.get(state);
        if (trset == null) {
            trset = new HashSet<SAFAEpsilon<P, S>>();
            epsilonFrom.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAEpsilon<P, S>> getEpsilonFrom(Collection<Integer> stateSet) {
        Collection<SAFAEpsilon<P, S>> transitions = new LinkedList<SAFAEpsilon<P, S>>();
        for (Integer state : stateSet)
            transitions.addAll(getEpsilonFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<SAFAInputMove<P, S>> getInputMovesFrom(Integer state) {
        Collection<SAFAInputMove<P, S>> trset = inputMovesFrom.get(state);
        if (trset == null) {
            trset = new HashSet<SAFAInputMove<P, S>>();
            inputMovesFrom.put(state, trset);
            return trset;
        }
        return trset;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAMove<P, S>> getMoves() {
        return getMovesFrom(getStates());
    }

    /**
     * Set of moves from set of states
     */
    public Collection<SAFAMove<P, S>> getMovesFrom(Collection<Integer> states) {
        Collection<SAFAMove<P, S>> transitions = new LinkedList<SAFAMove<P, S>>();
        for (Integer state : states)
            transitions.addAll(getMovesFrom(state));
        return transitions;
    }

    public Collection<SAFAMove<P, S>> getMovesFrom(Integer state) {
        Collection<SAFAMove<P, S>> transitions = new LinkedList<SAFAMove<P, S>>();
        transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions starting set of states
     */
    public Collection<SAFAMove<P, S>> getTransitions() {
        Collection<SAFAMove<P, S>> transitions = new LinkedList<SAFAMove<P, S>>();
        for (Integer state : states)
            transitions.addAll(getTransitionsFrom(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<SAFAMove<P, S>> getTransitionsTo(Integer state) {
        Collection<SAFAMove<P, S>> moves = new HashSet<SAFAMove<P, S>>();
        moves.addAll(getInputMovesTo(state));
        moves.addAll(getEpsilonTo(state));
        return moves;
    }

    /**
     * Returns the set of transitions to a set of states
     */
    public Collection<SAFAMove<P, S>> getTransitionsTo(Collection<Integer> stateSet) {
        Collection<SAFAMove<P, S>> transitions = new LinkedList<SAFAMove<P, S>>();
        for (Integer state : stateSet)
            transitions.addAll(getTransitionsTo(state));
        return transitions;
    }

    /**
     * Returns the set of transitions to state <code>s</code>
     */
    public Collection<SAFAEpsilon<P, S>> getEpsilonTo(Integer state) {
        Collection<SAFAEpsilon<P, S>> trset = epsilonTo.get(state);
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
    public Collection<SAFAInputMove<P, S>> getInputMovesTo(Integer state) {
        Collection<SAFAInputMove<P, S>> trset = inputMovesTo.get(state);
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
    public Collection<SAFAInputMove<P, S>> getInputMovesTo(Collection<Integer> stateSet) {
        Collection<SAFAInputMove<P, S>> transitions = new LinkedList<SAFAInputMove<P, S>>();
        for (Integer state : stateSet)
            transitions.addAll(getInputMovesTo(state));
        return transitions;
    }

    @Override
    public Object clone() {
        SAFA<P, S> cl = new SAFA<P, S>();

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
        String s = "";
        s = "Automaton: " + getTransitionCount() + " transitions, " + stateCount() + " states" + "\n";
        s += "Transitions \n";
        for (SAFAMove<P, S> t : getMoves())
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
