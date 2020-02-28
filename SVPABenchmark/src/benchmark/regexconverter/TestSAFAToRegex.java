package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.sql.Time;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSAFAToRegex {

    private static final UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testConcat() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("ab", "[a]ε[b]"),
                new Pair<>("abc", "[a]ε[b]ε[c]"),
                new Pair<>("ab(cd)e(f)", "[a]ε[b]ε[c]ε[d]ε[e]ε[f]"),
                new Pair<>("\\s", "[\\t-\\r ]"),

                /* TODO add so that when converting back to a regex we can map character classes
                 * TODO back to meta characters */
                new Pair<>("\\w", "[0-9A-Z_a-z]"),
                new Pair<>("\\W", "[\\u0000-/:-@\\[-^`{-\\uffff]")

        );

        validateWords(pairs);
    }

    @Test
    public void testUnion() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("ab|cd", "(ε[a]ε[b]ε|ε[c]ε[d]ε)"),
                new Pair<>("ab|(c|d)", "(ε[a]ε[b]ε|ε(ε[c]ε|ε[d]ε)ε)"),
                new Pair<>("a|(a|(b|cd))", "(ε[a]ε|ε(ε[a]ε|ε(ε[b]ε|ε[c]ε[d]ε)ε)ε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testCharacterClass() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("[abc]", "[a-c]"),
                new Pair<>("[\\d]", "[0-9]"),
                new Pair<>("a|[b-zA-Z1-9]", "(ε[a]ε|ε[1-9A-Zb-z]ε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testQuantifiers() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("a*", "ε(ε[a]ε)*ε"),
                new Pair<>("ab*", "[a]ε(ε[b]ε)*ε"),
                new Pair<>("[abc]*", "ε(ε[a-c]ε)*ε"),

                new Pair<>("de?|f[abc]?", "(ε[d]ε(ε[e]ε|εε)ε|ε[f]ε(ε[a-c]ε|εε)ε)")
        );

        validateWords(pairs);
    }


    @Test
    public void testRepeat() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("[a]{2}", "[a]ε[a]"),
                new Pair<>("a{2,}", "[a]ε[a]ε(ε[a]ε)*ε"),
                new Pair<>("a{2,4}", "(ε(ε[a]ε[a]ε|ε[a]ε[a]ε[a]ε)ε|ε[a]ε[a]ε[a]ε[a]ε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testPositiveLookaheads() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("(?=a)a", "(?=[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]"),
                new Pair<>("(?=a)(b|c)", "(?=[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε(ε[b]ε|ε[c]ε)"),
                new Pair<>("((?=aa)a|aa)", "(ε(?=[a]ε[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]ε|ε[a]ε[a]ε)"),
                new Pair<>("((?=aa)a|aa)b*c", "(ε(?=[a]ε[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]ε|ε[a]ε[a]ε)ε(ε[b]ε)*ε[c]")
        );

        validateWords(pairs);
    }

    @Test
    public void testRepeatedPositiveLookaheads() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("((?=aa)a)*a", "ε(ε(?=[a]ε[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]ε)*ε[a]"),
                new Pair<>("((a(?=a)a)*)*", "ε(ε(ε[a]ε(?=[a]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]ε)*ε)*ε"),
                new Pair<>("((a(?=bc)a)*)...", "ε(ε[a]ε(?=[b]ε[c]ε(ε[\\u0000-\\uffff]ε)*ε)ε[a]ε)*ε[\\u0000-\\uffff]ε[\\u0000-\\uffff]ε[\\u0000-\\uffff]")
        );

        validateWords(pairs);

//        SAFA<CharPred, Character> safa = Utils.constructFullMatchFromRegex("(?=a(?=bc))...");
        SAFA<CharPred, Character> safa = Utils.constructFullMatchFromRegex("((a(?=bc)a)*)...");
        System.out.println(safa.getDot("safa"));
        System.out.println("regex = " + RegexConverter.toRegex(safa, solver));
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException {


        assertTrue(true);
    }

    private void validateWords(List<Pair<String, String>> tests) throws TimeoutException {
        for (Pair<String, String> test : tests) {
            SAFA<CharPred, Character> safa = Utils.constructFullMatchFromRegex(test.first);
            String regex = RegexConverter.toRegex(safa, solver);
            System.out.println("regex = " + regex);
            assertEquals(test.second, regex);
        }
    }

}
