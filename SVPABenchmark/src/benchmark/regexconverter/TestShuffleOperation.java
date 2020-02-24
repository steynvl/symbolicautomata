package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestShuffleOperation {

    private UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testShuffleOperation01() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFullMatchFromRegex("ab"),
                Utils.constructFullMatchFromRegex("cd"), solver);

        Utils.validateFullMatchRegexConstruction(shuffle, 9, 12, 1, 0);
        validateAFAStrings(shuffle, Arrays.asList("abcd", "acdb", "cdab", "cabd"), Arrays.asList("abc", "cdba"));
    }

    @Test
    public void testShuffleOperation02() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFullMatchFromRegex("a|b"),
                Utils.constructFullMatchFromRegex("c|d"), solver);

        validateAFAStrings(shuffle, Arrays.asList("ac", "ad", "bc", "bd", "ca", "db"), Arrays.asList("a", "c"));
    }

   @Test
    public void testShuffleOperation03() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFullMatchFromRegex("ab?"),
                Utils.constructFullMatchFromRegex("cd?"), solver);

        validateAFAStrings(shuffle, Arrays.asList("abcd", "acd", "ac", "acd", "abc", "ca", "cda", "cab"),
                Arrays.asList("abd", "bcd"));
    }

    @Test
    public void testShuffleOperation04() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFullMatchFromRegex("(ab)*"),
                Utils.constructFullMatchFromRegex("c?d*"), solver);

        validateAFAStrings(shuffle, Arrays.asList("d", "cd", "ab", "dabab", "dddab", ""),
                Arrays.asList("abb", "cda"));
    }
    @Test
    public void testShuffleOperation05() throws TimeoutException {
        SAFA<CharPred, Character> shuffle = SAFA.shuffle(Utils.constructFullMatchFromRegex("ab+"),
                Utils.constructFullMatchFromRegex("c?d+"), solver);

        validateAFAStrings(shuffle, Arrays.asList("abd", "abcd", "dab", "cddab", "cdabbb"),
                Arrays.asList("ca", "dba"));
    }

    private void validateAFAStrings(SAFA<CharPred, Character> safa,
                                          List<String> matching, List<String> nonMatching) throws TimeoutException {
        for (String match : matching) {
            assertTrue(safa.accepts(Utils.lOfS(match), solver));
        }

        for (String nonMatch : nonMatching) {
            assertFalse(safa.accepts(Utils.lOfS(nonMatch), solver));
        }
    }

}
