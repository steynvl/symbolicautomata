package benchmark;

import RegexParser.*;
import automata.AutomataException;
import automata.safa.SAFA;
import benchmark.regexconverter.SAFAConstruction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.HashStringEncodingUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.List;

public class SAFAProvider {

    private SAFA<CharPred, Character> safa = null;

    private HashStringEncodingUnaryCharIntervalSolver solver;

    private RegexNode root;

    public SAFAProvider(String regex) {
        List<RegexNode> nodes = RegexParserProvider.parse(new String[]{ regex });
        assert nodes.size() == 1;
        root = nodes.get(0);
    }

    public SAFAProvider(RegexNode root) {
        this.root = root;
    }

    public static SAFAProvider fromRegexNode(RegexNode root) {
        return new SAFAProvider(root);
    }

    public SAFA<CharPred, Character> constructSAFAFromREwLA() throws TimeoutException, AutomataException {
        if (safa != null) {
            assert solver != null;
            return safa;
        }

        char delimiter = findDelimiterCandidate(root);
        delimiter = '#';

        /* create custom solver to redefine dot symbol to not match our explicit separation symbol */
        solver = new HashStringEncodingUnaryCharIntervalSolver(delimiter);

        safa = SAFAConstruction.toSAFA(root, solver);

        return safa;
    }

    public SAFA<CharPred, Character> getSAFA() throws TimeoutException, AutomataException {
        if (safa == null) {
            return constructSAFAFromREwLA();
        }

        return safa;
    }

    public HashStringEncodingUnaryCharIntervalSolver getSolver() throws TimeoutException, AutomataException {
        if (solver == null) {
            constructSAFAFromREwLA();
            return solver;
        }

        return solver;
    }

    private Character findDelimiterCandidate(RegexNode node) {
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

        CharPred alphabet = determineAlphabetOfRegex(node);

        CharPred negatedAlphabet = solver.MkNot(alphabet);

        if (!hasCandidate(negatedAlphabet)) {
            negatedAlphabet = solver.True();
        }

        return solver.generateWitness(negatedAlphabet);
    }

    private boolean hasCandidate(CharPred cp) {
        int candidates = 0;

        for (ImmutablePair<Character, Character> interval : cp.intervals) {
            assert interval.getLeft() <= interval.getRight();
            candidates += interval.getRight() - interval.getLeft();
            if (candidates >= 1) return true;
        }

        return false;
    }

    private CharPred determineAlphabetOfRegex(RegexNode node) {
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
        } else if (node instanceof CharNode) {
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
