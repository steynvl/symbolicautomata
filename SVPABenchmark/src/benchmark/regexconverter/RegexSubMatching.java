package benchmark.regexconverter;

import RegexParser.*;
import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.SAFAInputMove;
import automata.safa.SAFAMove;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.SubMatchUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.util.*;

public class RegexSubMatching {

    private SubMatchUnaryCharIntervalSolver solver;

    private Pair<Boolean, List<Character>> equivalency;

    private String regex1, regex2;

    private SAFA<CharPred, Character> safa1, safa2;

    public RegexSubMatching(String regex1, String regex2) {
        this.regex1 = regex1;
        this.regex2 = regex2;

        testForEquivalence();
    }

    public Pair<Boolean, List<Character>> getEquivalencyPair() {
        return equivalency;
    }

    public boolean isEquivalent() {
        return equivalency.first;
    }

    public SAFA<CharPred, Character> getSAFA01() {
        return safa1;
    }

    public SAFA<CharPred, Character> getSAFA02() {
        return safa2;
    }

    public SubMatchUnaryCharIntervalSolver getSolver() {
        return solver;
    }

    public static Pair<SAFA<CharPred, Character>, SubMatchUnaryCharIntervalSolver> constructSubMatchingSAFA(String regex) throws TimeoutException {
        RegexNode node = Utils.parseRegex(regex);

        Character delimiter = findDelimiterCandidate(node);
        SubMatchUnaryCharIntervalSolver solver = new SubMatchUnaryCharIntervalSolver(delimiter);

        RegexNode translated = RegexTranslator.translate(node);
        SAFA<CharPred, Character> safa = RegexConverter.toSAFA(translated, solver, delimiter);
        safa = SAFA.concatenate(safa, buildRemainder(delimiter, solver), solver);

        return new Pair<>(safa, solver);
    }

    private static SAFA<CharPred, Character> buildRemainder(Character delimiter,
                                                            SubMatchUnaryCharIntervalSolver solver) throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();
        Collection<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();
        transitions.add(new SAFAInputMove<>(0, boolExpr.MkState(1), new CharPred(delimiter)));

        SAFA<CharPred, Character> dem = SAFA.MkSAFA(
                transitions, boolExpr.MkState(0), Arrays.asList(1), new ArrayList<>(), solver
        );

        return SAFA.concatenate(dem, SAFA.star(SAFA.dot(solver), solver), solver);
    }

    private void testForEquivalence() {
        RegexNode node1 = Utils.parseRegex(regex1);
        RegexNode node2 = Utils.parseRegex(regex2);
        Character delimiter = findDelimiterCandidate(node1, node2);

        /* create custom solver to redefine dot symbol to not match explicit capturing brackets */
        solver = new SubMatchUnaryCharIntervalSolver(delimiter);

        RegexNode translated1 = RegexTranslator.translate(node1);
        RegexNode translated2 = RegexTranslator.translate(node2);

        try {
            SAFA<CharPred, Character> remainder = buildRemainder(delimiter, solver);

            safa1 = RegexConverter.toSAFA(translated1, solver, delimiter);
            safa1 = SAFA.concatenate(safa1, remainder, solver);

            safa2 = RegexConverter.toSAFA(translated2, solver, delimiter);
            safa2 = SAFA.concatenate(safa2, remainder, solver);

            equivalency = SAFA.isEquivalent(
                    safa1, safa2, new UnaryCharIntervalSolver(), SAFA.getBooleanExpressionFactory()
            );
        } catch (TimeoutException e) {
            /* TODO */
        }
    }

    private static Character findDelimiterCandidate(RegexNode node1) {
        return findDelimiterCandidate(node1, null);
    }

    private static Character findDelimiterCandidate(RegexNode node1,
                                                    RegexNode node2) {
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

        CharPred alphabet = node2 == null ? determineAlphabetOfRegex(node1) :
                solver.MkOr(determineAlphabetOfRegex(node1), determineAlphabetOfRegex(node2));

        CharPred negatedAlphabet = solver.MkNot(alphabet);

        if (!hasCandidate(negatedAlphabet)) {
            negatedAlphabet = solver.True();
        }

        return solver.generateWitness(negatedAlphabet);
    }

    private static boolean hasCandidate(CharPred cp) {
        int candidates = 0;

        for (ImmutablePair<Character, Character> interval : cp.intervals) {
            assert interval.getLeft() <= interval.getRight();
            candidates += interval.getRight() - interval.getLeft();
            if (candidates >= 1) return true;
        }

        return false;
    }

    private static CharPred determineAlphabetOfRegex(RegexNode node) {
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
        CharPred cp = solver.False();

        if (node instanceof UnionNode) {
            UnionNode unionNode = (UnionNode) node;
            CharPred left = determineAlphabetOfRegex(unionNode.getMyRegex1());
            CharPred right = determineAlphabetOfRegex(unionNode.getMyRegex2());

            return solver.MkOr(left, right);
        } else if (node instanceof ConcatenationNode) {
            ConcatenationNode concatNode = (ConcatenationNode) node;

            for (RegexNode n : concatNode.getList()) {
                cp = solver.MkOr(cp, determineAlphabetOfRegex(n));
            }

            return cp;
        } else if (node instanceof DotNode || node instanceof AnchorNode) {
            return cp;
        } else if (node instanceof StarNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((StarNode) node).getMyRegex1()));
        } else if (node instanceof PlusNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((PlusNode) node).getMyRegex1()));
        } else if (node instanceof OptionalNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((OptionalNode) node).getMyRegex1()));
        } else if (node instanceof PositiveLookaheadNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((PositiveLookaheadNode) node).getMyRegex1()));
        } else if (node instanceof NegativeLookaheadNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((NegativeLookaheadNode) node).getMyRegex1()));
        } else if (node instanceof AtomicGroupNode) {
            return solver.MkOr(cp, determineAlphabetOfRegex(((AtomicGroupNode) node).getMyRegex1()));
        } else if (node instanceof NormalCharNode || node instanceof EscapedCharNode || node instanceof MetaCharNode) {
            CharNode charNode = (CharNode) node;
            return solver.MkOr(cp, new CharPred(charNode.getChar()));
        } else if (node instanceof CharacterClassNode || node instanceof NotCharacterClassNode) {
            boolean isNormalCC = node instanceof CharacterClassNode;
            List<IntervalNode> intervals = isNormalCC ?
                    ((CharacterClassNode) node).getIntervals() : ((NotCharacterClassNode) node).getIntervals();

            CharPred cpIntervals = solver.False();
            for (IntervalNode intervalNode : intervals) {
                if (intervalNode.getMode().equals("single")) {
                    cpIntervals = solver.MkOr(cpIntervals, new CharPred(intervalNode.getChar1().getChar()));
                } else if (intervalNode.getMode().equals("range")) {
                    cpIntervals = solver.MkOr(cpIntervals, new CharPred(
                            intervalNode.getChar1().getChar(),
                            intervalNode.getChar2().getChar())
                    );
                } else {
                    System.err.println("Unknown interval mode, program will quit.");
                    System.exit(-1);
                }
            }

            return isNormalCC ? cpIntervals : solver.MkNot(cpIntervals);
        } else if (node instanceof RepetitionNode) {
            return determineAlphabetOfRegex(((RepetitionNode) node).getMyRegex1());
        } else if (node instanceof ModifierNode) {
            throw new UnsupportedOperationException();
        } else {
            System.err.println("Wrong instance of node, program will quit");
            System.exit(-1);
        }

        return cp;
    }

}
