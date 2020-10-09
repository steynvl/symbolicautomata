package automata.eps_safa;

import utilities.Pair;

import java.util.Optional;

public class MoveType {
    private Pair<Integer, Optional<Integer>> toStates;
    private boolean existential;

    private MoveType(boolean existential, Pair<Integer, Optional<Integer>> toStates) {
        this.existential =  existential;
        this.toStates = toStates;
    }

    public boolean isExistentialMove() {
        return existential;
    }

    public boolean isUniversalMove() {
        return !existential;
    }

    public Pair<Integer, Optional<Integer>> toStates() {
        return toStates;
    }

    public static MoveType makeExistential(int to) {
        return new MoveType(true, new Pair<>(to, Optional.empty()));
    }

    public static MoveType makeUniversal(int to1, int to2) {
        return new MoveType(false, new Pair<>(to1, Optional.of(to2)));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MoveType)) {
            return false;
        }

        MoveType o = (MoveType) other;
        return toStates.equals(o.toStates) && isExistentialMove() == o.isExistentialMove();
    }

}
