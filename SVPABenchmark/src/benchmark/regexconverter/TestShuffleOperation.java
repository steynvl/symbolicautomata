package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.Arrays;

public class TestShuffleOperation {

    private UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testShuffleOperation01() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFromRegex("ab"),
                Utils.constructFromRegex("cd"), solver);

        Utils.validateFullMatchRegexConstruction(shuffle, 9, 12, 1, 0);
        Utils.validateAFAStrings(shuffle, Arrays.asList("abcd", "acdb", "cdab", "cabd"), Arrays.asList("abc", "cdba"));
    }

    @Test
    public void testShuffleOperation02() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFromRegex("a|b"),
                Utils.constructFromRegex("c|d"), solver);

        Utils.validateAFAStrings(shuffle, Arrays.asList("ac", "ad", "bc", "bd", "ca", "db"), Arrays.asList("a", "c"));
    }

   @Test
    public void testShuffleOperation03() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFromRegex("ab?"),
                Utils.constructFromRegex("cd?"), solver);

        Utils.validateAFAStrings(shuffle, Arrays.asList("abcd", "acd", "ac", "acd", "abc", "ca", "cda", "cab"),
                Arrays.asList("abd", "bcd"));
    }

    @Test
    public void testShuffleOperation04() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFromRegex("(ab)*"),
                Utils.constructFromRegex("c?d*"), solver);

        System.out.println(shuffle.getDot("shuffle"));

        Utils.validateAFAStrings(shuffle, Arrays.asList("d", "cd", "ab", "dabab", "dddab", ""),
                Arrays.asList("abb", "cda"));
    }
    @Test
    public void testShuffleOperation05() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFromRegex("ab+"),
                Utils.constructFromRegex("c?d+"), solver);

        Utils.validateAFAStrings(shuffle, Arrays.asList("abd", "abcd", "dab", "cddab", "cdabbb"),
                Arrays.asList("ca", "dba"));
    }

}
