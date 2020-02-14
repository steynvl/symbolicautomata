package theory.intervals;

import theory.characters.CharPred;
import theory.characters.StdCharPred;

public class SubMatchUnaryCharIntervalSolver extends UnaryCharIntervalSolver {

    private CharPred redefinedTrue;

    private Character delimiter;

    public SubMatchUnaryCharIntervalSolver(Character delimiter) {
        super();
        this.delimiter = delimiter;
        redefinedTrue = MkAnd(StdCharPred.TRUE, MkNot(new CharPred(delimiter)));
    }

    public Character getDelimiter() {
        return delimiter;
    }

    @Override
    public CharPred True() {
        return redefinedTrue;
    }

}
