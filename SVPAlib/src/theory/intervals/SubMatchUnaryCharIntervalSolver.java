package theory.intervals;

import theory.characters.CharPred;
import theory.characters.StdCharPred;
import utilities.Quadruple;

public class SubMatchUnaryCharIntervalSolver extends UnaryCharIntervalSolver {

    private CharPred redefinedTrue;

    public SubMatchUnaryCharIntervalSolver(Quadruple<Character, Character, Character, Character> capturingBrackets) {
        super();

        CharPred brackets = MkOr(
                MkOr(new CharPred(capturingBrackets.first), new CharPred(capturingBrackets.second)),
                MkOr(new CharPred(capturingBrackets.third), new CharPred(capturingBrackets.fourth))
        );
        redefinedTrue = MkAnd(StdCharPred.TRUE, MkNot(brackets));
    }

    @Override
    public CharPred True() {
        return redefinedTrue;
    }

}
