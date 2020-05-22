package benchmark.regexconverter;


import RegexParser.*;
import automata.safa.*;
import automata.sfa.SFA;
import org.sat4j.specs.TimeoutException;

import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class RegexConverter {

    public static String toRegex(SAFA<CharPred, Character> safa,
                                 UnaryCharIntervalSolver solver) throws TimeoutException {
        return SAFAToRegex.toRegex(safa, solver);
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver) throws TimeoutException {
        return SAFAConstruction.toSAFA(phi, unarySolver, null, new StringBuilder());
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver,
                                                   Character delimiter) throws TimeoutException {
        return SAFAConstruction.toSAFA(phi, unarySolver, delimiter, new StringBuilder());
    }

    public static SAFA<CharPred, Character> toSAFA(FormulaNode phi,
                                                   UnaryCharIntervalSolver unarySolver,
                                                   Character delimiter,
                                                   StringBuilder sb)
            throws TimeoutException {
        return SAFAConstruction.toSAFA(phi, unarySolver, delimiter, sb);
    }

    public static SFA<CharPred, Character> toSFA(FormulaNode phi, UnaryCharIntervalSolver unarySolver)
            throws TimeoutException {
        return SFAConstruction.toSFA(phi, unarySolver);
    }

    public static CharPred getCharPred(IntervalNode node, UnaryCharIntervalSolver unarySolver) {
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
