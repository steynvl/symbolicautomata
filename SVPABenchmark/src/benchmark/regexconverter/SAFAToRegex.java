package benchmark.regexconverter;

import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.SAFAEpsilon;
import automata.safa.SAFAMove;
import automata.safa.booleanexpression.PositiveAnd;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import automata.safa.booleanexpression.PositiveId;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.*;

public class SAFAToRegex {

    private static final BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

    private static final String EPSILON = "ε";

    public static String toRegex(SAFA<CharPred, Character> safa,
                                 UnaryCharIntervalSolver solver) throws TimeoutException {
        Set<Integer> universalStates = new HashSet<>();

        safa = trimAndNormalize(safa, solver);
        System.out.println(safa.getDot("trimAndNormalized"));
        Set<Integer> lookaheadStates = new HashSet<>();

        /* process all universal states */
        for (int s : safa.getStates()) {

            Collection<SAFAMove<CharPred, Character>> moves = safa.getTransitionsFrom(s);
            SAFAMove<CharPred, Character> transition;

            /* FIXME: to be able to work for a epsilon-free SAFA, have to process all and then merge them together somehow */
            if (moves.size() == 1 && (transition = moves.iterator().next()).to instanceof PositiveAnd) {
                System.out.println("transition = " + transition);

                universalStates.add(transition.from);
                int lookaheadStart = Collections.min(transition.to.getStates());
                int mainBranch = Collections.max(transition.to.getStates());

                System.out.println("from = " + transition.from);
                System.out.println("lookaheadStart = " + lookaheadStart);

                SAFA<CharPred, Character> leftSubAut = constructSubAutomata(lookaheadStart, safa, solver);
                lookaheadStates.addAll(leftSubAut.getStates());
                System.out.println(leftSubAut.getDot("leftSubAut"));

                transition.to = pb.MkState(mainBranch);

                String regex = toRegex(leftSubAut, solver);

                String trailingMatch = String.format("(%s.%s)*%s", EPSILON, EPSILON, EPSILON);
                if (regex.endsWith(trailingMatch)) {
                    regex = regex.substring(0, regex.length() - trailingMatch.length());
                }

                transition.regex = String.format("(?=%s)", regex);
            }
        }

        safa.getStates().removeAll(lookaheadStates);

        /* process remaining states */
        int initial = safa.getInitialState().getStates().iterator().next();
        LinkedList<Integer> toProcess = new LinkedList<>();
        List<Integer> fromCandidates = new ArrayList<>();
        List<Integer> toCandidates = new ArrayList<>();
        for (int s : safa.getStates()) {
            if (s != initial && !safa.getFinalStates().contains(s))
                toProcess.add(s);
            if (!safa.getFinalStates().contains(s))
                fromCandidates.add(s);
            if (s != initial)
                toCandidates.add(s);
        }

        Set<SAFAMove<CharPred, Character>> removedTransitions = new HashSet<>();
        System.out.println("toProcess = " + toProcess);
        System.out.println("fromCandidiates = " + fromCandidates);
        System.out.println("toCandidiates = " + toCandidates);
        while (!toProcess.isEmpty()) {
            int q = toProcess.removeFirst();

            for (int q1 : fromCandidates) {
                if (q1 == q) continue;

                for (int q2 : toCandidates) {
                    if (q2 == q) continue;

                    Optional<SAFAMove<CharPred, Character>> directMove = Optional.empty();

                    RegexBuilder rb = new RegexBuilder();
                    for (SAFAMove<CharPred, Character> move : safa.getMovesFrom(q1)) {
                        if (removedTransitions.contains(move)) continue;

                        assert move.to instanceof PositiveId;
                        int to = move.to.getStates().iterator().next();
                        if (to == q2) {
                            rb.setAlphaQ1_Q2(move.regex);
                            directMove = Optional.of(move);
                        } else if (to == q) {
                            rb.setAlphaQ1_Q(move.regex);
                        }
                    }

                    for (SAFAMove<CharPred, Character> move : safa.getMovesFrom(q)) {
                        if (removedTransitions.contains(move)) continue;

                        assert move.to instanceof PositiveId;
                        int to = move.to.getStates().iterator().next();

                        if (to == q) {
                            rb.setAlphaQ_Q(move.regex);
                        } else if (to == q2) {
                            rb.setAlphaQ_Q2(move.regex);
                        }
                    }

                    Optional<String> regex = rb.buildRegex();
                    if (regex.isPresent()) {
                        SAFAMove<CharPred, Character> newMove = directMove.orElseGet(
                                () -> new SAFAEpsilon<>(q1, pb.MkState(q2))
                        );
                        newMove.regex = regex.get();

                        if (!directMove.isPresent())
                            safa.addTransition(newMove, solver, true);
                    }
                }
            }

            removedTransitions.addAll(safa.getEpsilonFrom().getOrDefault(q, new HashSet<>()));
            removedTransitions.addAll(safa.getEpsilonTo().getOrDefault(q, new HashSet<>()));
            removedTransitions.addAll(safa.getInputMovesFrom().getOrDefault(q, new HashSet<>()));
            removedTransitions.addAll(safa.getInputMovesTo().getOrDefault(q, new HashSet<>()));
            safa.getEpsilonFrom().remove(q); safa.getEpsilonTo().remove(q);
            safa.getInputMovesFrom().remove(q); safa.getInputMovesTo().remove(q);

            safa.getStates().remove(q);

            // TODO update safa.transitionsCount
        }

        assert safa.getStates().size() == 2;
        assert safa.getFinalStates().size() == 1;
        assert safa.getInitialState() instanceof PositiveId;

        System.out.println(safa.getTransitions());

        Collection<SAFAMove<CharPred, Character>> moves = safa.getTransitionsFrom(initial);
        moves.removeAll(removedTransitions);

        assert moves.size() == 1;

        return moves.iterator().next().regex;
    }

    private static SAFA<CharPred, Character> trimAndNormalize(SAFA<CharPred, Character> safa,
                                                              UnaryCharIntervalSolver solver) throws TimeoutException {
        assert safa.getInitialState() instanceof PositiveId;
        PositiveBooleanExpression safaInit = safa.getInitialState();

        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

        int initial = ((PositiveId) safa.getInitialState()).state;
        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        Collection<Integer> finalStates = new HashSet<>();

        for (int state : safa.getStates()) {

            for (SAFAMove<CharPred, Character> move : safa.getTransitionsFrom(state)) {
                assert move.to instanceof PositiveId || move.to instanceof PositiveAnd;

                SAFAMove<CharPred, Character> tnew = (SAFAMove<CharPred, Character>) move.clone();

                if (move.regex != null) {
                    tnew.regex = move.regex;
                } else if (move.isEpsilonTransition()) {
                    tnew.regex = EPSILON;
                } else {
                    tnew.regex = tnew.guard.getSingleChar()
                            .map(character -> character + "")
                            .orElseGet(() -> tnew.guard.equals(solver.True()) ? "." : tnew.guard.toString());
                }

                transitions.add(tnew);
            }

        }

        /* initial state may not have any incoming transitions  */
        if (safa.getTransitionsTo(initial).size() > 0) {
            int newInit = safa.getMaxStateId() + 1;
            transitions.add(new SAFAEpsilon<>(newInit, safa.getInitialState(), EPSILON));
            safa.setMaxStateId(safa.getMaxStateId() + 1);
            safaInit = pb.MkState(newInit);
        }

        /* enforce one final state with no outgoing transitions */
        if (safa.getFinalStates().size() == 1) {
            int finalState = safa.getFinalStates().iterator().next();
            if (safa.getTransitionsFrom(finalState).size() > 0) {
                transitions.add(
                        new SAFAEpsilon<>(finalState, pb.MkState(safa.getMaxStateId() + 1), EPSILON)
                );
                finalStates.add(safa.getMaxStateId() + 1);
            } else {
                finalStates.add(finalState);
            }
        } else {
            int newFinal = safa.getMaxStateId() + 1;
            for (int fs : safa.getFinalStates()) {
                transitions.add(
                        new SAFAEpsilon<>(fs, pb.MkState(newFinal), EPSILON)
                );
            }
            finalStates.add(newFinal);
        }

        return SAFA.MkSAFA(
                transitions, safaInit, finalStates, safa.getLookaheadFinalStates(), solver
        );
    }

    private static SAFA<CharPred, Character> constructSubAutomata(int start,
                                                                  SAFA<CharPred, Character> safa,
                                                                  UnaryCharIntervalSolver solver) throws TimeoutException {
        /* FIXME: figure how to handle (.. ?= ..)* */
        /* FIXME: figure how to handle (.. ?= (?= ...) ..)* */

        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        Collection<Integer> finalStates = new HashSet<>();

        Set<Integer> visited = new HashSet<>();
        visited.add(start);

        Queue<Integer> toProcess = new LinkedList<>();
        toProcess.add(start);

        while (!toProcess.isEmpty()) {
            int curr = toProcess.remove();

            for (SAFAMove<CharPred, Character> move : safa.getTransitionsFrom(curr)) {
                transitions.add(move);

                for (int to : move.to.getStates()) {
                    if (!visited.contains(to)) {
                        toProcess.add(to);
                        visited.add(to);
                    }
                }
            }

            if (safa.getLookaheadFinalStates().contains(curr)) {
                finalStates.add(curr);
            }
        }

        return SAFA.MkSAFA(transitions, pb.MkState(start), finalStates, new HashSet<>(), solver);
    }

}
