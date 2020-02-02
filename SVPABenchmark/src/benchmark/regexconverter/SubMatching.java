package benchmark.regexconverter;

import RegexParser.*;
import automata.safa.SAFA;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.SubMatchUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;
import utilities.Quadruple;

import java.util.ArrayList;
import java.util.List;


public class SubMatching {

    /* */
    private SubMatchUnaryCharIntervalSolver solver;

    /* */
    private Pair<Boolean, List<Character>> equivalency;

    /* */
    private String regex1, regex2;

    public SubMatching(String regex1, String regex2) {
        this.regex1 = regex1;
        this.regex2 = regex2;

        testForEquivalence();
    }

    public boolean isEquivalent() {
        return equivalency.first;
    }

    /* TODO public api methods */

    private void testForEquivalence() {
        RegexNode node1 = Utils.parseRegex(regex1);
        RegexNode node2 = Utils.parseRegex(regex2);
        Quadruple<Character, Character, Character, Character> cb = findCapturingCandidates(node1, node2);

        /* create custom solver to redefine dot symbol to not match explicit capturing brackets */
        solver = new SubMatchUnaryCharIntervalSolver(cb);

        RegexNode translated1 = RegexTranslator.translate(node1);
        RegexNode translated2 = RegexTranslator.translate(node2);

        try {
            /* construct safa that matches capturing brackets, add as argument to toSAFA method */

            SAFA<CharPred, Character> safa1 = RegexConverter.toSAFA(translated1, solver);
            SAFA<CharPred, Character> safa2 = RegexConverter.toSAFA(translated2, solver);

            equivalency = SAFA.isEquivalent(safa1, safa2, solver, SAFA.getBooleanExpressionFactory());
        } catch (TimeoutException e) {
            /* TODO */
        }
    }

    private Quadruple<Character, Character, Character, Character> findCapturingCandidates(RegexNode node1,
                                                                                          RegexNode node2) {
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

        CharPred alphabet = solver.MkOr(determineAlphabetOfRegex(node1), determineAlphabetOfRegex(node2));
        CharPred negatedAlphabet = solver.MkNot(alphabet);

        if (!hasFourCandidates(negatedAlphabet)) {
            negatedAlphabet = solver.True();
        }

        List<Character> cb = new ArrayList<>();
        while (cb.size() != 4) {
            char c = solver.generateWitness(negatedAlphabet);
            if (!cb.contains(c)) {
                cb.add(c);
            }
        }

        return new Quadruple<>(cb.get(0), cb.get(1), cb.get(2), cb.get(3));
    }

    private boolean hasFourCandidates(CharPred cp) {
        int candidates = 0;

        for (ImmutablePair<Character, Character> interval : cp.intervals) {
            assert interval.getLeft() <= interval.getRight();
            candidates += interval.getRight() - interval.getLeft();
            if (candidates >= 4) return true;
        }

        return false;
    }

    private CharPred determineAlphabetOfRegex(RegexNode node) {
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
