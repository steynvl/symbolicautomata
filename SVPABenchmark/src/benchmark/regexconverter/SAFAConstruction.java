package benchmark.regexconverter;

import RegexParser.*;
import automata.AutomataException;
import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.SAFAInputMove;
import automata.safa.SAFAMove;
import automata.safa.booleanexpression.PositiveAnd;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.HashStringEncodingUnaryCharIntervalSolver;

import java.util.*;

public class SAFAConstruction {

    private static final BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   HashStringEncodingUnaryCharIntervalSolver solver)
            throws TimeoutException, AutomataException {
        return SAFA.concatenate(_toSAFA(phi, solver), constructMainTail(solver), solver);
    }

    private static SAFA<CharPred, Character> constructMainTail(HashStringEncodingUnaryCharIntervalSolver solver)
            throws TimeoutException {
        PositiveBooleanExpression to = pb.MkState(1);

        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        transitions.add(new SAFAInputMove<>(0, to, new CharPred(solver.getDelimiter())));
        transitions.add(new SAFAInputMove<>(1, to, solver.True()));

        return SAFA.MkSAFA(transitions, pb.MkState(0), Arrays.asList(1), new ArrayList<>(), solver);
    }

    private static SAFA<CharPred, Character> _toSAFA(FormulaNode phi,
                                                     HashStringEncodingUnaryCharIntervalSolver unarySolver)
            throws TimeoutException, AutomataException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        SAFA<CharPred, Character> outputSAFA = null;

        if (phi instanceof UnionNode) {
            // get left SAFA and right SAFA, union them

            UnionNode cphi = (UnionNode) phi;

            SAFA<CharPred, Character> left = _toSAFA(cphi.getMyRegex1(), unarySolver);
            SAFA<CharPred, Character> right = _toSAFA(cphi.getMyRegex2(), unarySolver);

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

                iterateSAFA = _toSAFA(regexNode, unarySolver);
                while (it.hasNext()) {
                    regexNode = it.next();
                    SAFA<CharPred, Character> followingSAFA = _toSAFA(regexNode, unarySolver);

                    iterateSAFA = SAFA.concatenate(iterateSAFA, followingSAFA, unarySolver);
                }
            }
            return iterateSAFA;
        } else if (phi instanceof DotNode) {
            return SAFA.dot(unarySolver);

        } else if (phi instanceof AnchorNode) {
            AnchorNode cphi = (AnchorNode) phi;
            if (cphi.hasStartAnchor()) {
                System.out.println("Ignoring start anchor in regex!");
            } else if (cphi.hasEndAnchor()) {
                outputSAFA = SAFA.endAnchor(unarySolver);
            }
        } else if (phi instanceof StarNode) {
            // use SAFA.star() method
            StarNode cphi = (StarNode) phi;
            SAFA<CharPred, Character> tempSAFA = _toSAFA(cphi.getMyRegex1(), unarySolver);
            outputSAFA = SAFA.star(tempSAFA, unarySolver);

        } else if (phi instanceof PlusNode) {
            // expr+ = expr concatenate with expr*
            PlusNode cphi = (PlusNode) phi;
            SAFA<CharPred, Character> tempSAFA = _toSAFA(cphi.getMyRegex1(), unarySolver);
            outputSAFA = SAFA.concatenate(tempSAFA, SAFA.star(tempSAFA, unarySolver), unarySolver);

        } else if (phi instanceof OptionalNode) {
            OptionalNode cphi = (OptionalNode) phi;
            SAFA<CharPred, Character> tempSAFA = _toSAFA(cphi.getMyRegex1(), unarySolver);

            // build an SAFA that only accepts the empty string
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            PositiveBooleanExpression initial = SAFA.getBooleanExpressionFactory().MkState(0);
            outputSAFA = SAFA.union(
                    tempSAFA,
                    SAFA.MkSAFA(transitions, initial, Arrays.asList(0), new HashSet<>(), unarySolver), unarySolver
            );

        } else if (phi instanceof PositiveLookaheadNode) {
            PositiveLookaheadNode cphi = (PositiveLookaheadNode) phi;

            SAFA<CharPred, Character> lookAhead = _toSAFA(cphi.getMyRegex1(), unarySolver);

            /* add #-labeled self loops to each "or" state */
            addSelfLoops(lookAhead, unarySolver);

            for (Integer fs : lookAhead.getFinalStates()) {
                PositiveBooleanExpression to = pb.MkState(fs);
                CharPred cp = unarySolver.MkOr(unarySolver.True(), new CharPred(unarySolver.getDelimiter()));
                lookAhead.addTransition(new SAFAInputMove<>(fs, to, cp), unarySolver, true);
            }

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);
            return outputSAFA;

        } else if (phi instanceof NegativeLookaheadNode) {
            NegativeLookaheadNode cphi = (NegativeLookaheadNode) phi;

            SAFA<CharPred, Character> lookAhead = _toSAFA(cphi.getMyRegex1(), unarySolver);

            for (Integer fs : lookAhead.getFinalStates()) {
                PositiveBooleanExpression to = pb.MkState(fs);
                lookAhead.addTransition(new SAFAInputMove<>(fs, to, unarySolver.True()), unarySolver, true);
            }

            lookAhead = lookAhead.negate(unarySolver);

            /* add #-labeled self loops to each "or" state */
            addSelfLoops(lookAhead, unarySolver);

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);
            return outputSAFA;

        } else if (phi instanceof AtomicGroupNode) {
            StringBuilder sb = new StringBuilder();
            sb.append("AFA construction does not support atomic groups. ");
            sb.append("Use RegexTranslator.removeAtomicOperators to convert aREwLA -> REwLA first.");
            throw new AutomataException(sb.toString());
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
            StringBuilder s = new StringBuilder();
            cphi.toRaw(s);
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            transitions.add(new SAFAInputMove<>(
                    0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred(cphi.getChar()))
            );

            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
        } else if (phi instanceof MetaCharNode) {
            MetaCharNode cphi = (MetaCharNode) phi;
            StringBuilder s = new StringBuilder();
            cphi.toRaw(s);
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
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
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
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
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
            SAFA<CharPred, Character> tempSAFA = _toSAFA(cphi.getMyRegex1(), unarySolver);
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
            if (phi == null) {
                return null;
            }
            System.err.println("Wrong instance of phi, program will quit");
            System.exit(-1);
        }

        return outputSAFA;
    }

    private static void addSelfLoops(SAFA<CharPred, Character> aut,
                                     HashStringEncodingUnaryCharIntervalSolver solver) throws TimeoutException {
        for (SAFAMove<CharPred, Character> sm : aut.getTransitions()) {
            if (!(sm.to instanceof PositiveAnd)) {
                CharPred cp = new CharPred(solver.getDelimiter());
                SAFAInputMove<CharPred, Character> move = new SAFAInputMove<>(sm.from, pb.MkState(sm.from), cp);
                aut.addTransition(move, solver, true);
            }
        }
    }

}
