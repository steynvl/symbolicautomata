package benchmark.regexconverter;

import automata.safa.SAFA;
import benchmark.SAFAProvider;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSAFAConstruction {

    UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testConcatAndUnion() throws TimeoutException {
        String regex = "abc|de";
        validateRegexConstruction(regex, 11, 10, 2, 0);
        validateRegexInputString(regex, Arrays.asList("abc", "de"), Arrays.asList("ab"));
    }

    @Test
    public void testMetaChar() throws TimeoutException {
        validateRegexConstruction("\\s", 2, 1, 1, 0);
        validateRegexInputString("\\s", Arrays.asList(" "), Arrays.asList("a"));

        validateRegexConstruction("\\S", 2, 1, 1, 0);
        validateRegexInputString("\\S", Arrays.asList("a"), Arrays.asList(" "));

        validateRegexConstruction("\\w", 2, 1, 1, 0);
        validateRegexInputString("\\w", Arrays.asList("a", "_", "1"), Arrays.asList(" "));
    }

    @Test
    public void testCharacterClass() throws TimeoutException {
        validateRegexConstruction("[\\d]", 2, 1, 1, 0);
        validateRegexInputString("[\\d]", Arrays.asList("1"), Arrays.asList("10"));

        validateRegexConstruction("[\\\\a123bc!@#$s%^&*(){}]",
                2, 1, 1, 0);
        validateRegexInputString("[\\\\a123bc!@#$s%^&*(){}]",
                Arrays.asList("1", "a", "^", "s"), Arrays.asList("10"));

        validateRegexConstruction("[a-zA-Z1-9]", 2, 1, 1, 0);
        validateRegexInputString("[a-zA-Z1-9]", Arrays.asList("8", "b", "c", "C"), Arrays.asList("10"));

        validateRegexConstruction("\\a|[b-zA-Z1-9]", 5, 4, 2, 0);
        validateRegexInputString("\\a|[b-zA-Z1-9]", Arrays.asList("a"), Arrays.asList("\\a"));
    }

    @Test
    public void testQuantifiers() throws TimeoutException {
        validateRegexConstruction("[\\d]+", 5, 5, 1, 0);
        validateRegexInputString("[\\d]+", Arrays.asList("1234567", "111131", "111123459"), Arrays.asList("a"));

        validateRegexConstruction("a*", 3, 3, 1, 0);
        validateRegexInputString("a*", Arrays.asList("", "a", "aaaa"), Arrays.asList("b"));

        validateRegexConstruction("[abc]*", 3, 3, 1, 0);
        validateRegexInputString("[abc]*", Arrays.asList("", "aaaa", "abcbcbcbcc"), Arrays.asList("d"));

        validateRegexConstruction("de?|f[abc]?", 13, 12, 4, 0);
        validateRegexInputString("de?|f[abc]?", Arrays.asList("d", "de", "fa", "f"), Arrays.asList("def"));
    }

    /* helper methods */
    private void validateRegexInputString(String regex,
                                          List<String> matching,
                                          List<String> nonMatching) throws TimeoutException {
        SAFA<CharPred, Character> safa = new SAFAProvider(regex, solver).getSAFA();

        for (String m : matching)
            assertTrue(safa.accepts(lOfS(m), solver));

        for (String nm : nonMatching)
            assertFalse(safa.accepts(lOfS(nm), solver));
    }

    private void validateRegexConstruction(String regex, int stateCnt, int transitionCnt,
                                           int finalStateCnt, int lookaheadCnt) {
        SAFA<CharPred, Character> safa = new SAFAProvider(regex, solver).getSAFA();

        assertEquals(safa.getStates().size(), stateCnt);
        assertEquals((int) safa.getTransitionCount(), transitionCnt);
        assertEquals(safa.getFinalStates().size(), finalStateCnt);
        assertEquals(safa.getLookaheadFinalStates().size(), lookaheadCnt);
    }

    private void printDot(String regex) {
        System.out.println(new SAFAProvider(regex, solver).getSAFA().getDot("safa"));
    }

    private List<Character> lOfS(String s) {
        List<Character> l = new ArrayList<>();
        char[] ca = s.toCharArray();
        for (int i = 0; i < s.length(); i++)
            l.add(ca[i]);
        return l;
    }

}
