package benchmark.regexconverter;


import java.util.*;

import automata.safa.*;
import automata.safa.booleanexpression.PositiveAnd;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import automata.safa.booleanexpression.PositiveId;
import org.sat4j.specs.TimeoutException;

import RegexParser.AnchorNode;
import RegexParser.CharNode;
import RegexParser.CharacterClassNode;
import RegexParser.ConcatenationNode;
import RegexParser.DotNode;
import RegexParser.EscapedCharNode;
import RegexParser.FormulaNode;
import RegexParser.IntervalNode;
import RegexParser.MetaCharNode;
import RegexParser.ModifierNode;
import RegexParser.NormalCharNode;
import RegexParser.NotCharacterClassNode;
import RegexParser.OptionalNode;
import RegexParser.PlusNode;
import RegexParser.RegexNode;
import RegexParser.RepetitionNode;
import RegexParser.StarNode;
import RegexParser.UnionNode;
import RegexParser.PositiveLookaheadNode;
import RegexParser.NegativeLookaheadNode;
import RegexParser.AtomicGroupNode;
import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class RegexConverter {

    private static final BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

    public static String toRegex(SAFA<CharPred, Character> safa,
                                 UnaryCharIntervalSolver solver) throws TimeoutException {
        Set<Integer> universalStates = new HashSet<>();

        safa = trimAndNormalize(safa, solver);

        /* process all universal states */
        for (int s : safa.getStates()) {
            Collection<SAFAMove<CharPred, Character>> moves = safa.getTransitionsFrom(s);
            SAFAMove<CharPred, Character> transition;
            if (moves.size() == 1 && (transition = moves.iterator().next()).to instanceof PositiveAnd) {
                universalStates.add(transition.from);
                Iterator<Integer> it = transition.to.getStates().iterator();
                int lookaheadStart = it.next();
                int mainBranch = it.next();

                /* TODO might need to remove all states in leftsubaut from safa.states */
                SAFA<CharPred, Character> leftSubAut = constructSubAutomata(lookaheadStart, safa);

                safa.getEpsilonFrom().get(transition.from).clear();

                SAFAEpsilon<CharPred, Character> newMove = new SAFAEpsilon<>(transition.from, pb.MkState(mainBranch));
                newMove.regex = String.format("(?=%s)", toRegex(leftSubAut, solver));

                safa.getEpsilonFrom().get(transition.from).add(newMove);

                return newMove.regex;
            }
        }

        /* process remaining states */
        int initial = safa.getInitialState().getStates().iterator().next();
        LinkedList<Integer> toProcess = new LinkedList<>();
        List<Integer> fromCandidiates = new ArrayList<>();
        List<Integer> toCandidiates = new ArrayList<>();
        for (int s : safa.getStates()) {
            if (s != initial && !safa.getFinalStates().contains(s))
                toProcess.add(s);
            if (!safa.getFinalStates().contains(s))
                fromCandidiates.add(s);
            if (s != initial)
                toCandidiates.add(s);
        }

        while (!toProcess.isEmpty()) {
            int q = toProcess.removeFirst();
            int q1 = fromCandidiates.iterator().next();
            int q2 = toCandidiates.iterator().next();

            for (SAFAMove<CharPred, Character> move : safa.getEpsilonFrom(q1)) {
                int to;
                if (move.to instanceof PositiveId && (to = move.to.getStates().iterator().next()) == q2) {
                    
                }
            }

        }

        /* TODO do some assertions before return delta(q0, f) */

        return null;
    }

    private static SAFA<CharPred, Character> trimAndNormalize(SAFA<CharPred, Character> safa,
                                                              UnaryCharIntervalSolver solver) throws TimeoutException {
        assert safa.getInitialState() instanceof PositiveId;

        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

        int initial = ((PositiveId) safa.getInitialState()).state;
        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        Collection<Integer> finalStates = new HashSet<>();

        for (int state : safa.getStates()) {

            for (SAFAMove<CharPred, Character> move : safa.getTransitionsFrom(state)) {
                assert move.to instanceof PositiveId || move.to instanceof PositiveAnd;

                SAFAMove<CharPred, Character> tnew = (SAFAMove<CharPred, Character>) move.clone();

                tnew.regex = tnew.isEpsilonTransition() ? "ε" : tnew.guard.toString();
                transitions.add(tnew);
            }

        }

        /* we assume requirement our initial state will have no incoming transitions  */
        assert safa.getTransitionsTo(initial).size() == 0;

        /* enforce one final state with no outgoing transitions */
        if (safa.getFinalStates().size() == 1) {
            int finalState = safa.getFinalStates().iterator().next();
            if (safa.getTransitionsFrom(finalState).size() > 0) {
                transitions.add(
                        new SAFAEpsilon<>(finalState, pb.MkState(safa.getMaxStateId() + 1))
                );
                finalStates.add(safa.getMaxStateId() + 1);
            } else {
                finalStates.add(finalState);
            }
        } else {
            int newFinal = safa.getMaxStateId() + 1;
            for (int fs : safa.getFinalStates()) {
                transitions.add(
                        new SAFAEpsilon<>(fs, pb.MkState(newFinal))
                );
            }
            finalStates.add(newFinal);
        }

        return SAFA.MkSAFA(
                transitions, safa.getInitialState(), finalStates, safa.getLookaheadFinalStates(), solver
        );
    }

    private static SAFA<CharPred, Character> constructSubAutomata(int start,
                                                                  SAFA<CharPred, Character> safa) throws TimeoutException {


        return null;
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver) throws TimeoutException {
        return toSAFA(phi, unarySolver, null);
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver,
                                                   Character delimiter)
            throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        SAFA<CharPred, Character> outputSAFA = null;

        if (phi instanceof UnionNode) {
            // get left SAFA and right SAFA, union them

            UnionNode cphi = (UnionNode) phi;

            SAFA<CharPred, Character> left = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            SAFA<CharPred, Character> right = toSAFA(cphi.getMyRegex2(), unarySolver, delimiter);

            outputSAFA = SAFA.union(left, right, unarySolver);

            return outputSAFA;

        } else if (phi instanceof ConcatenationNode) {
            // get the first SAFA in concatenation list, then for every following
            // SAFA, iteratively concatenate them
            ConcatenationNode cphi = (ConcatenationNode) phi;
            List<RegexNode> concateList = cphi.getList();
            Iterator<RegexNode> it = concateList.iterator();
            //initialize SAFA to empty SAFA
            SAFA<CharPred, Character> iterateSAFA = SAFA.getEmptySAFA(unarySolver);
            if (it.hasNext()) {
                RegexNode regexNode = it.next();
                iterateSAFA = toSAFA(regexNode, unarySolver, delimiter);
                while (it.hasNext()) {
                    regexNode = it.next();
                    SAFA<CharPred, Character> followingSAFA = toSAFA(regexNode, unarySolver, delimiter);
                    iterateSAFA = SAFA.concatenate(iterateSAFA, followingSAFA, unarySolver);
                }
            }
            return iterateSAFA;
        } else if (phi instanceof DotNode) {
            return SAFA.dot(unarySolver);

        } else if (phi instanceof AnchorNode) {
            AnchorNode cphi = (AnchorNode) phi;
            outputSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            if (!cphi.hasStartAnchor()) {
                // put startAnchor SAFA to the front of the following SAFA
                outputSAFA = SAFA.concatenate(SAFA.getFullSAFA(unarySolver), outputSAFA, unarySolver);
            }
            if (!cphi.hasEndAnchor()) {
                // for end anchor, create a SAFA that has state 0 that goes to state 1 with every input and add self-loop for state 1
                outputSAFA = SAFA.concatenate(outputSAFA, SAFA.getFullSAFA(unarySolver), unarySolver);
            }

        } else if (phi instanceof StarNode) {
            // use SAFA.star() method
            StarNode cphi = (StarNode) phi;
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            outputSAFA = SAFA.star(tempSAFA, unarySolver);

        } else if (phi instanceof PlusNode) {
            // expr+ = expr concatenate with expr*
            PlusNode cphi = (PlusNode) phi;
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            outputSAFA = SAFA.concatenate(tempSAFA, SAFA.star(tempSAFA, unarySolver), unarySolver);

        } else if (phi instanceof OptionalNode) {
            OptionalNode cphi = (OptionalNode) phi;
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);

            // build an SAFA that only accepts the empty string
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            PositiveBooleanExpression initial = SAFA.getBooleanExpressionFactory().MkState(0);
            outputSAFA = SAFA.union(
                    tempSAFA,
                    SAFA.MkSAFA(transitions, initial, Arrays.asList(0), new HashSet<>(), unarySolver), unarySolver
            );

        } else if (phi instanceof PositiveLookaheadNode) {
            PositiveLookaheadNode cphi = (PositiveLookaheadNode) phi;
            SAFA<CharPred, Character> lookAhead = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            lookAhead = SAFA.concatenate(lookAhead, SAFA.star(SAFA.dot(unarySolver), unarySolver), unarySolver);

            if (delimiter != null) {
                lookAhead = shuffleInDelimiter(lookAhead, delimiter, unarySolver);
            }

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);

            return outputSAFA;

        } else if (phi instanceof NegativeLookaheadNode) {
            NegativeLookaheadNode cphi = (NegativeLookaheadNode) phi;
            SAFA<CharPred, Character> lookAhead = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);

            lookAhead = SAFA.concatenate(lookAhead, SAFA.star(SAFA.dot(unarySolver), unarySolver), unarySolver);
            lookAhead = lookAhead.negate(unarySolver);

            if (delimiter != null) {
                lookAhead = shuffleInDelimiter(lookAhead, delimiter, unarySolver);
            }

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);

            return outputSAFA;

        } else if (phi instanceof AtomicGroupNode) {
            AtomicGroupNode cphi = (AtomicGroupNode) phi;

            System.out.println(RegexTranslator.printNode(
                    RegexTranslator.translate(cphi.getMyRegex1())
            ) + " [atomic]");

            outputSAFA = toSAFA(RegexTranslator.translate(cphi.getMyRegex1()), unarySolver, delimiter);
        } else if (phi instanceof NormalCharNode) {
            // make a SAFA that has a transition which accepts this char
            NormalCharNode cphi = (NormalCharNode) phi;
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
            transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred(cphi.getChar())));

            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
        } else if (phi instanceof EscapedCharNode) {
            // make a SAFA that has a transition which accepts the char after the
            // backslash
            EscapedCharNode cphi = (EscapedCharNode) phi;
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            transitions.add(new SAFAInputMove<>(
                    0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred(cphi.getChar()))
            );

            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
        } else if (phi instanceof MetaCharNode) {
            MetaCharNode cphi = (MetaCharNode) phi;
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            char meta = cphi.getChar();
            if (meta == 't') {
                // CharPred \t
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred('\t', '\t')));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'n') {
                // CharPred \n)
                transitions.add(new SAFAInputMove<>(0, boolExpr.MkState(1), new CharPred('\n', '\n')));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'r') {
                // CharPred \r
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred('\r', '\r')));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'f') {
                // CharPred \f
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred('\f', '\f')));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'b') {
                // don't know how to do word boundary
                throw new UnsupportedOperationException();
            } else if (meta == 'B') {
                // don't know how to do word boundary
                throw new UnsupportedOperationException();
            } else if (meta == 'd') {
                // use existing NUM
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), StdCharPred.NUM));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'D') {
                // MkNot(NUM)
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), unarySolver.MkNot(StdCharPred.NUM)));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 's') {
                // use existing SPACES
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), StdCharPred.SPACES));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'S') {
                // MkNot(SPACES)
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), unarySolver.MkNot(StdCharPred.SPACES)));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'v') {
                // predicate= new CharPred('\v', '\v');
                // this meta can be seen in the regexlib but it seems java
                // does not support this
                throw new UnsupportedOperationException();
            } else if (meta == 'w') {
                // use existing WORD
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), StdCharPred.WORD));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            } else if (meta == 'W') {
                // MkNot(WORD)
                transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), unarySolver.MkNot(StdCharPred.WORD)));
                return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
            }

        } else if (phi instanceof CharacterClassNode) {
            // MkOr each interval then MkSAFA from the final CharPred
            CharacterClassNode cphi = (CharacterClassNode) phi;
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            List<IntervalNode> intervalList = cphi.getIntervals();
            Iterator<IntervalNode> it = intervalList.iterator();
            CharPred predicate = unarySolver.False();
            if (it.hasNext()) {
                predicate = getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = getCharPred(it.next(), unarySolver);
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), predicate));
            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);

        } else if (phi instanceof NotCharacterClassNode) {
            // MkOr each interval then MkNot the final result of the CharPred,
            // then MkSAFA from that final CharPred
            NotCharacterClassNode cphi = (NotCharacterClassNode) phi;
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            List<IntervalNode> intervalList = cphi.getIntervals();
            Iterator<IntervalNode> it = intervalList.iterator();
            CharPred predicate = unarySolver.False();
            if (it.hasNext()) {
                predicate = getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = getCharPred(it.next(), unarySolver);
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            predicate = unarySolver.MkNot(predicate);
            transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), predicate));
            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);

        } else if (phi instanceof RepetitionNode) {
            RepetitionNode cphi = (RepetitionNode) phi;
            //special case when there is zero repetition which means the Regex is not existing
            if (cphi.getMin() == 0) {
                return SAFA.getEmptySAFA(unarySolver);
            }
            //now the repetition will be at least once
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter);
            //make sure there is no empty SAFA when using SAFA.concatenate()
            outputSAFA = tempSAFA;
            // i starts from 1 because we already have one repetition above
            for (int i = 1; i < cphi.getMin(); i++) { //now we looped min times
                outputSAFA = SAFA.concatenate(outputSAFA, tempSAFA, unarySolver);
            }

            if (cphi.getMode().equals("min")) {
                //already looped min times
                return outputSAFA;

            } else if (cphi.getMode().equals("minToInfinite")) {
                // concatenate with a star, e.g. R{3,} = RRR(R)*
                return SAFA.concatenate(outputSAFA, SAFA.star(tempSAFA, unarySolver), unarySolver);

            } else { // minToMax
                SAFA<CharPred, Character> unions = outputSAFA;
                for (int i = cphi.getMin(); i < cphi.getMax(); i++) {
                    unions = SAFA.concatenate(unions, tempSAFA, unarySolver);
                    outputSAFA = SAFA.union(outputSAFA, unions, unarySolver);
                }
                return outputSAFA;
            }

        } else if (phi instanceof ModifierNode) {
            throw new UnsupportedOperationException();
        } else {
            System.err.println("Wrong instance of phi, program will quit");
            System.exit(-1);
        }

        return outputSAFA;
    }

    private static SAFA<CharPred, Character> shuffleInDelimiter(SAFA<CharPred, Character> safa,
                                                                Character delimiter,
                                                                UnaryCharIntervalSolver solver) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        transitions.add(new SAFAInputMove<>(
                0, boolExpr.MkState(1), new CharPred(delimiter))
        );

        SAFA<CharPred, Character> delimiterSAFA = SAFA.MkSAFA(
                transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), solver
        );

        return SAFA.shuffle(safa, delimiterSAFA, solver);
    }

    public static SFA<CharPred, Character> toSFA(FormulaNode phi, UnaryCharIntervalSolver unarySolver)
            throws TimeoutException {
        SFA<CharPred, Character> outputSFA = null;

        if (phi instanceof PositiveLookaheadNode) {
            System.out.println("Positive lookaheads are not supported in the SFA model!");
            System.exit(3);
        } else if (phi instanceof NegativeLookaheadNode) {
            System.out.println("Negative lookaheads are not supported in the SFA model!");
            System.exit(3);
        } else if (phi instanceof AtomicGroupNode) {
            System.out.println("Atomic groups are not supported in the SFA model!");
            System.exit(3);
        } else if (phi instanceof UnionNode) {
            // get left SFA and right SFA, union them

            UnionNode cphi = (UnionNode) phi;
            SFA<CharPred, Character> left = toSFA(cphi.getMyRegex1(), unarySolver);
            SFA<CharPred, Character> right = toSFA(cphi.getMyRegex2(), unarySolver);
            outputSFA = SFA.union(left, right, unarySolver);

            return outputSFA;

        } else if (phi instanceof ConcatenationNode) {
            // get the first SFA in concatenation list, then for every following
            // SFA, iteratively concatenate them

            ConcatenationNode cphi = (ConcatenationNode) phi;
            List<RegexNode> concateList = cphi.getList();
            Iterator<RegexNode> it = concateList.iterator();
            //initialize SFA to empty SFA
            SFA<CharPred, Character> iterateSFA = SFA.getEmptySFA(unarySolver);
            if (it.hasNext()) {
                iterateSFA = toSFA(it.next(), unarySolver);
                while (it.hasNext()) {
                    SFA<CharPred, Character> followingSFA = toSFA(it.next(), unarySolver);
                    iterateSFA = SFA.concatenate(iterateSFA, followingSFA, unarySolver);
                }
            }
            return iterateSFA;

        } else if (phi instanceof DotNode) {
            // make a SFA that has a transition which accepts TRUE
            Collection<SFAMove<CharPred, Character>> transitionsA = new LinkedList<SFAMove<CharPred, Character>>();
            transitionsA.add(new SFAInputMove<CharPred, Character>(0, 1, unarySolver.True()));
            return SFA.MkSFA(transitionsA, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof AnchorNode) {
            AnchorNode cphi = (AnchorNode) phi;
            outputSFA = toSFA(cphi.getMyRegex1(), unarySolver);
            if (!cphi.hasStartAnchor()) {
                // put startAnchor SFA to the front of the following SFA
                outputSFA = SFA.concatenate(SFA.getFullSFA(unarySolver), outputSFA, unarySolver);
            }
            if (!cphi.hasEndAnchor()) {
                // for end anchor, create a SFA that has state 0 that goes to state 1 with every input and add self-loop for state 1
                outputSFA = SFA.concatenate(outputSFA, SFA.getFullSFA(unarySolver), unarySolver);
            }

        } else if (phi instanceof StarNode) {
            // use existing SFA.star() method
            StarNode cphi = (StarNode) phi;
            SFA<CharPred, Character> tempSFA = toSFA(cphi.getMyRegex1(), unarySolver);
            outputSFA = SFA.star(tempSFA, unarySolver);

        } else if (phi instanceof PlusNode) {
            // expr+ = expr concatenate with expr*
            PlusNode cphi = (PlusNode) phi;
            SFA<CharPred, Character> tempSFA = toSFA(cphi.getMyRegex1(), unarySolver);
            outputSFA = SFA.concatenate(tempSFA, SFA.star(tempSFA, unarySolver), unarySolver);

        } else if (phi instanceof OptionalNode) {
            OptionalNode cphi = (OptionalNode) phi;
            SFA<CharPred, Character> tempSFA = toSFA(cphi.getMyRegex1(), unarySolver);

            // build an SFA that only accepts the empty string
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            outputSFA = SFA.union(tempSFA, SFA.MkSFA(transitions, 0, Arrays.asList(0), unarySolver), unarySolver);

        } else if (phi instanceof NormalCharNode) {
            // make a SFA that has a transition which accepts this char
            NormalCharNode cphi = (NormalCharNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred(cphi.getChar())));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof EscapedCharNode) {
            // make a SFA that has a transition which accepts the char after the
            // backslash
            EscapedCharNode cphi = (EscapedCharNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred(cphi.getChar())));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof MetaCharNode) {
            MetaCharNode cphi = (MetaCharNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            char meta = cphi.getChar();
            if (meta == 't') {
                // CharPred \t
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('\t', '\t')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'n') {
                // CharPred \n
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('\n', '\n')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'r') {
                // CharPred \r
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('\r', '\r')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'f') {
                // CharPred \f
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('\f', '\f')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'b') {
                // don't know how to do word boundary
                throw new UnsupportedOperationException();
            } else if (meta == 'B') {
                // don't know how to do word boundary
                throw new UnsupportedOperationException();
            } else if (meta == 'd') {
                // use existing NUM
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, StdCharPred.NUM));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'D') {
                // MkNot(NUM)
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, unarySolver.MkNot(StdCharPred.NUM)));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 's') {
                // use existing SPACES
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, StdCharPred.SPACES));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'S') {
                // MkNot(SPACES)
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, unarySolver.MkNot(StdCharPred.SPACES)));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

            } else if (meta == 'v') {
                // predicate= new CharPred('\v', '\v');
                // this meta can be seen in the regexlib but it seems java
                // does not support this
                throw new UnsupportedOperationException();
            } else if (meta == 'w') {
                // use existing WORD
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, StdCharPred.WORD));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'W') {
                // MkNot(WORD)
                transitions.add(new SFAInputMove<CharPred, Character>(0, 1, unarySolver.MkNot(StdCharPred.WORD)));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            }

        } else if (phi instanceof CharacterClassNode) {
            // MkOr each interval then MkSFA from the final CharPred
            CharacterClassNode cphi = (CharacterClassNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            List<IntervalNode> intervalList = cphi.getIntervals();
            Iterator<IntervalNode> it = intervalList.iterator();
            CharPred predicate = unarySolver.False();
            if (it.hasNext()) {
                predicate = getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = getCharPred(it.next(), unarySolver);
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            transitions.add(new SFAInputMove<CharPred, Character>(0, 1, predicate));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof NotCharacterClassNode) {
            // MkOr each interval then MkNot the final result of the CharPred,
            // then MkSFA from that final CharPred
            NotCharacterClassNode cphi = (NotCharacterClassNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            List<IntervalNode> intervalList = cphi.getIntervals();
            Iterator<IntervalNode> it = intervalList.iterator();
            CharPred predicate = unarySolver.False();
            if (it.hasNext()) {
                predicate = getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = getCharPred(it.next(), unarySolver);
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            predicate = unarySolver.MkNot(predicate);
            transitions.add(new SFAInputMove<CharPred, Character>(0, 1, predicate));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof RepetitionNode) {
            RepetitionNode cphi = (RepetitionNode) phi;
            //special case when there is zero repetition which means the Regex is not existing
            if (cphi.getMin() == 0) {
                return SFA.getEmptySFA(unarySolver);
            }
            //now the repetition will be at least once
            SFA<CharPred, Character> tempSFA = toSFA(cphi.getMyRegex1(), unarySolver);
            //make sure there is no empty SFA when using SFA.concatenate()
            outputSFA = tempSFA;
            // i starts from 1 because we already have one repetition above
            for (int i = 1; i < cphi.getMin(); i++) { //now we looped min times
                outputSFA = SFA.concatenate(outputSFA, tempSFA, unarySolver);
            }

            if (cphi.getMode().equals("min")) {
                //already looped min times
                return outputSFA;

            } else if (cphi.getMode().equals("minToInfinite")) {
                // concatenate with a star, e.g. R{3,} = RRR(R)*
                return SFA.concatenate(outputSFA, SFA.star(tempSFA, unarySolver), unarySolver);

            } else { // minToMax
                SFA<CharPred, Character> unions = outputSFA;
                for (int i = cphi.getMin(); i < cphi.getMax(); i++) {
                    unions = SFA.concatenate(unions, tempSFA, unarySolver);
                    outputSFA = SFA.union(outputSFA, unions, unarySolver);
                }
                return outputSFA;
            }

        } else if (phi instanceof ModifierNode) {
            throw new UnsupportedOperationException();
        } else {
            System.err.println("Wrong instance of phi, program will quit");
            System.exit(-1);
        }

        return outputSFA;

    }

    private static CharPred getCharPred(IntervalNode node, UnaryCharIntervalSolver unarySolver) {
        CharPred predicate = null;
        if (node.getMode().equals("single")) {
            CharNode single = node.getChar1();
            if (single instanceof NormalCharNode) {
                predicate = new CharPred(single.getChar());
            } else if (single instanceof MetaCharNode) {
                char meta = single.getChar();
                if (meta == 't') {
                    predicate = new CharPred('\t', '\t');
                } else if (meta == 'n') {
                    predicate = new CharPred('\n', '\n');
                } else if (meta == 'r') {
                    predicate = new CharPred('\r', '\r');
                } else if (meta == 'f') {
                    predicate = new CharPred('\f', '\f');
                } else if (meta == 'b') {
                    // don't know how to do word boundary
                    throw new UnsupportedOperationException();
                } else if (meta == 'B') {
                    // don't know how to do word boundary
                    throw new UnsupportedOperationException();
                } else if (meta == 'd') {
                    predicate = StdCharPred.NUM;
                } else if (meta == 'D') {
                    predicate = unarySolver.MkNot(StdCharPred.NUM);
                } else if (meta == 's') {
                    predicate = StdCharPred.SPACES;
                } else if (meta == 'S') {
                    predicate = unarySolver.MkNot(StdCharPred.SPACES);
                } else if (meta == 'v') {
                    // predicate= new CharPred('\v', '\v');
                    // this meta can be seen in the regexlib but it seems java
                    // does not support this
                    throw new UnsupportedOperationException();
                } else if (meta == 'w') {
                    predicate = StdCharPred.WORD;
                } else if (meta == 'W') {
                    // not sure how to take complement, there should be more
                    predicate = unarySolver.MkNot(StdCharPred.WORD);
                }

            } else { // EscapedCharNode
                // getChar() method returns the char after the backslash
                // TODO: not sure if backslash is needed
                predicate = new CharPred(single.getChar());
            }
        } else {
            predicate = new CharPred(node.getChar1().getChar(), node.getChar2().getChar());
        }

        return predicate;
    }

}
