package benchmark.regexconverter;

import RegexParser.*;
import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.SAFAInputMove;
import automata.safa.SAFAMove;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.*;

public class SAFAConstruction {

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver) throws TimeoutException {
        return toSAFA(phi, unarySolver, null, new StringBuilder());
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver,
                                                   Character delimiter) throws TimeoutException {
        return toSAFA(phi, unarySolver, delimiter, new StringBuilder());
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver,
                                                   Character delimiter,
                                                   StringBuilder sb)
            throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        SAFA<CharPred, Character> outputSAFA = null;

        if (phi instanceof UnionNode) {
            // get left SAFA and right SAFA, union them

            UnionNode cphi = (UnionNode) phi;

            sb.append("(");
            SAFA<CharPred, Character> left = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
            sb.append("|");
            SAFA<CharPred, Character> right = toSAFA(cphi.getMyRegex2(), unarySolver, delimiter, sb);
            sb.append(")");

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

                iterateSAFA = toSAFA(regexNode, unarySolver, delimiter, sb);
                while (it.hasNext()) {
                    regexNode = it.next();
                    SAFA<CharPred, Character> followingSAFA = toSAFA(regexNode, unarySolver, delimiter, sb);

                    iterateSAFA = SAFA.concatenate(iterateSAFA, followingSAFA, unarySolver);
                }
            }
            return iterateSAFA;
        } else if (phi instanceof DotNode) {
            sb.append(".");
            return SAFA.dot(unarySolver);

        } else if (phi instanceof AnchorNode) {
            AnchorNode cphi = (AnchorNode) phi;
            if (cphi.hasStartAnchor()) {
                sb.append("^");
//                outputSAFA = SAFA.getFullSAFA(unarySolver);
            } else if (cphi.hasEndAnchor()) {
                sb.append("$");
                outputSAFA = SAFA.endAnchor(unarySolver);
            }
        } else if (phi instanceof StarNode) {
            // use SAFA.star() method
            StarNode cphi = (StarNode) phi;
            sb.append("(");
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
            sb.append(")*");
            outputSAFA = SAFA.star(tempSAFA, unarySolver);

        } else if (phi instanceof PlusNode) {
            // expr+ = expr concatenate with expr*
            PlusNode cphi = (PlusNode) phi;
            sb.append("(");
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
            sb.append(")+");
            outputSAFA = SAFA.concatenate(tempSAFA, SAFA.star(tempSAFA, unarySolver), unarySolver);

        } else if (phi instanceof OptionalNode) {
            OptionalNode cphi = (OptionalNode) phi;
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);

            // build an SAFA that only accepts the empty string
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            PositiveBooleanExpression initial = SAFA.getBooleanExpressionFactory().MkState(0);
            outputSAFA = SAFA.union(
                    tempSAFA,
                    SAFA.MkSAFA(transitions, initial, Arrays.asList(0), new HashSet<>(), unarySolver), unarySolver
            );
            sb.append("?");

        } else if (phi instanceof PositiveLookaheadNode) {
            PositiveLookaheadNode cphi = (PositiveLookaheadNode) phi;
            sb.append("(?=");
            SAFA<CharPred, Character> lookAhead = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
            sb.append(")");
            lookAhead = SAFA.concatenate(lookAhead, SAFA.star(SAFA.dot(unarySolver), unarySolver), unarySolver);

            if (delimiter != null) {
                lookAhead = shuffleInDelimiter(lookAhead, delimiter, unarySolver);
            }

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);

            return outputSAFA;

        } else if (phi instanceof NegativeLookaheadNode) {
            NegativeLookaheadNode cphi = (NegativeLookaheadNode) phi;
            sb.append("(?!");
            SAFA<CharPred, Character> lookAhead = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
            sb.append(")");

            lookAhead = SAFA.concatenate(lookAhead, SAFA.star(SAFA.dot(unarySolver), unarySolver), unarySolver);
            lookAhead = lookAhead.negate(unarySolver);

            if (delimiter != null) {
                lookAhead = shuffleInDelimiter(lookAhead, delimiter, unarySolver);
            }

            outputSAFA = SAFA.positiveLookAhead(lookAhead, unarySolver);

            return outputSAFA;

        } else if (phi instanceof AtomicGroupNode) {
            AtomicGroupNode cphi = (AtomicGroupNode) phi;

            RegexNode translated = RegexTranslator.translate(cphi.getMyRegex1());
            outputSAFA = toSAFA(translated, unarySolver, delimiter, sb);
        } else if (phi instanceof NormalCharNode) {
            // make a SAFA that has a transition which accepts this char
            NormalCharNode cphi = (NormalCharNode) phi;
            sb.append(cphi.getChar());
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
            transitions.add(new SAFAInputMove<>(0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred(cphi.getChar())));

            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
        } else if (phi instanceof EscapedCharNode) {
            // make a SAFA that has a transition which accepts the char after the
            // backslash
            EscapedCharNode cphi = (EscapedCharNode) phi;
            StringBuilder s = new StringBuilder();
            cphi.toRaw(s);
            sb.append(s);
            Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<SAFAMove<CharPred, Character>>();
            transitions.add(new SAFAInputMove<>(
                    0, SAFA.getBooleanExpressionFactory().MkState(1), new CharPred(cphi.getChar()))
            );

            return SAFA.MkSAFA(transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), unarySolver);
        } else if (phi instanceof MetaCharNode) {
            MetaCharNode cphi = (MetaCharNode) phi;
            StringBuilder s = new StringBuilder();
            cphi.toRaw(s);
            sb.append(s);
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
            sb.append("[");
            if (it.hasNext()) {
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                sb.append(predicate.toRaw());
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
                    sb.append(temp.toRaw());
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            sb.append("]");
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
            sb.append("[^");
            if (it.hasNext()) {
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                sb.append(predicate.toRaw());
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
                    sb.append(temp.toRaw());
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            sb.append("]");
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
            SAFA<CharPred, Character> tempSAFA = toSAFA(cphi.getMyRegex1(), unarySolver, delimiter, sb);
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

    private static SAFA<CharPred, Character> manualShuffle(SAFA<CharPred, Character> safa,
                                                           Character delimiter,
                                                           UnaryCharIntervalSolver solver) {
        int maxStateID = safa.getMaxStateId();



        return null;
    }

}
