package theory.intervals;

import theory.characters.CharPred;
import theory.characters.StdCharPred;

public class HashEncodingUnaryCharIntervalSolver extends UnaryCharIntervalSolver {

    private CharPred redefinedTrue;

    private char delimiter;

    public HashEncodingUnaryCharIntervalSolver(char delimiter) {
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
