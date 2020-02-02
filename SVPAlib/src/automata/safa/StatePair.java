package automata.safa;

import automata.safa.booleanexpression.PositiveBooleanExpression;

import java.util.Objects;

public class StatePair {

    protected PositiveBooleanExpression s;
    protected int s1;
    protected int s2;

    protected StatePair(PositiveBooleanExpression s, int s1, int s2) {
        this.s = s;
        this.s1 = s1;
        this.s2 = s2;
    }

    protected StatePair(int s1, int s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StatePair)) {
            return false;
        }

        StatePair sp = (StatePair) other;
        return s1 == sp.s1 && s2 == sp.s2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(s1, s2);
    }

}
