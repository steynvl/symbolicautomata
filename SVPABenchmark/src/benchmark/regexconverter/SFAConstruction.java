package benchmark.regexconverter;

import RegexParser.*;
import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.*;

public class SFAConstruction {

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
            transitionsA.add(new SFAInputMove<>(0, 1, unarySolver.True()));
            return SFA.MkSFA(transitionsA, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof AnchorNode) {
            AnchorNode cphi = (AnchorNode) phi;
//            outputSFA = toSFA(cphi.getMyRegex1(), unarySolver);
//            if (!cphi.hasStartAnchor()) {
            // put startAnchor SFA to the front of the following SFA
//                outputSFA = SFA.concatenate(SFA.getFullSFA(unarySolver), outputSFA, unarySolver);
//            }
//            if (!cphi.hasEndAnchor()) {
            // for end anchor, create a SFA that has state 0 that goes to state 1 with every input and add self-loop for state 1
//                outputSFA = SFA.concatenate(outputSFA, SFA.getFullSFA(unarySolver), unarySolver);
//            }

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
            transitions.add(new SFAInputMove<>(0, 1, new CharPred(cphi.getChar())));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof EscapedCharNode) {
            // make a SFA that has a transition which accepts the char after the
            // backslash
            EscapedCharNode cphi = (EscapedCharNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            transitions.add(new SFAInputMove<>(0, 1, new CharPred(cphi.getChar())));
            return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);

        } else if (phi instanceof MetaCharNode) {
            MetaCharNode cphi = (MetaCharNode) phi;
            Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
            char meta = cphi.getChar();
            if (meta == 't') {
                // CharPred \t
                transitions.add(new SFAInputMove<>(0, 1, new CharPred('\t', '\t')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'n') {
                // CharPred \n
                transitions.add(new SFAInputMove<>(0, 1, new CharPred('\n', '\n')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'r') {
                // CharPred \r
                transitions.add(new SFAInputMove<>(0, 1, new CharPred('\r', '\r')));
                return SFA.MkSFA(transitions, 0, Arrays.asList(1), unarySolver);
            } else if (meta == 'f') {
                // CharPred \f
                transitions.add(new SFAInputMove<>(0, 1, new CharPred('\f', '\f')));
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
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
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
                predicate = RegexConverter.getCharPred(it.next(), unarySolver);
                while (it.hasNext()) {
                    CharPred temp = RegexConverter.getCharPred(it.next(), unarySolver);
                    predicate = unarySolver.MkOr(predicate, temp);
                }
            }
            predicate = unarySolver.MkNot(predicate);
            transitions.add(new SFAInputMove<>(0, 1, predicate));
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

}
