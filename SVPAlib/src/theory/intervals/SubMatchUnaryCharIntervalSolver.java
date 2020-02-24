package theory.intervals;

import theory.characters.CharPred;
import theory.characters.StdCharPred;
import utilities.Quadruple;

public class SubMatchUnaryCharIntervalSolver extends UnaryCharIntervalSolver {

    private CharPred redefinedTrue;

    public SubMatchUnaryCharIntervalSolver(Character delimiter) {
        super();
        redefinedTrue = MkAnd(StdCharPred.TRUE, MkNot(new CharPred(delimiter)));
    }

    @Override
    public CharPred True() {
        return redefinedTrue;
    }

}
