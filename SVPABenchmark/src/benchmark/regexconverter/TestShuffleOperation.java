package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class TestShuffleOperation {

    private UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testShuffleOperation01() throws TimeoutException {
        SAFA<CharPred, Character> safa1 = Utils.constructFromRegex("ab");
        SAFA<CharPred, Character> safa2 = Utils.constructFromRegex("cd");

        System.out.println(safa1.getDot("safa1"));
        System.out.println(safa2.getDot("safa2"));

        SAFA<CharPred, Character> shuffle = SAFA.shuffle(safa1, safa2, solver);

        System.out.println(shuffle.getDot("shuffle"));
    }

}
