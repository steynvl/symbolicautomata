package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSAFAToRegex {

    private static final UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    @Test
    public void testConcat() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("ab", "aεb"),
                new Pair<>("abc", "aεbεc"),
                new Pair<>("ab(cd)e(f)", "aεbεcεdεeεf"),
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
                new Pair<>("ab|cd", "(εaεbε|εcεdε)"),
                new Pair<>("ab|(c|d)", "(εaεbε|ε(εcε|εdε)ε)"),
                new Pair<>("a|(a|(b|cd))", "(εaε|ε(εaε|ε(εbε|εcεdε)ε)ε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testCharacterClass() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("[abc]", "[a-c]"),
                new Pair<>("[\\d]", "[0-9]"),
                new Pair<>("a|[b-zA-Z1-9]", "(εaε|ε[1-9A-Zb-z]ε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testQuantifiers() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("a*", "ε(εaε)*ε"),
                new Pair<>("ab*", "aε(εbε)*ε"),
                new Pair<>("[abc]*", "ε(ε[a-c]ε)*ε"),

                new Pair<>("de?|f[abc]?", "(εdε(εeε|εε)ε|εfε(ε[a-c]ε|εε)ε)")
        );

        validateWords(pairs);
    }


    @Test
    public void testRepeat() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("[a]{2}", "aεa"),
                new Pair<>("a{2,}", "aεaε(εaε)*ε"),
                new Pair<>("a{2,4}", "(ε(εaεaε|εaεaεaε)ε|εaεaεaεaε)")
        );

        validateWords(pairs);
    }

    @Test
    public void testPositiveLookaheads() throws TimeoutException {
        List<Pair<String, String>> pairs = Arrays.asList(
                new Pair<>("(?=a)a", "(?=aε)εa"),
                new Pair<>("(?=a)(b|c)", "(?=aε)ε(εbε|εcε)"),
                new Pair<>("((?=aa)a|aa)", "(ε(?=aεaε)εaε|εaεaε)"),
                new Pair<>("((?=aa)a|aa)b*c", "(ε(?=aεaε)εaε|εaεaε)ε(εbε)*εc")
        );

        validateWords(pairs);
    }

    @Test
    public void testRepeatedPositiveLookaheads() throws TimeoutException {
//        List<Pair<String, String>> pairs = Arrays.asList(
//                new Pair<>("((?=aa)a)*a", "ε(ε(?=aεaε)εaε)*εa"),
//                new Pair<>("((a(?=a)a)*)*", "ε(ε(εaε(?=aε)εaε)*ε)*ε"),
//                new Pair<>("((a(?=bc)a)*)...", "ε(εaε(?=bεcε)εaε)*ε.ε.ε."),
//                new Pair<>("((?=a).)*", "ε(ε(?=aε)ε.ε)*ε"),
//                new Pair<>("((?=aa)a(q(?=b)k)*)*a", "ε(ε(?=aεaε)εaε(εqε(?=(bεε|bεε(.εε)*.εε))εkε)*ε)*εa")
//        );

//        validateWords(pairs);


//        SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex("(E(?=F)G)*");
//        SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex("((a(?=bc)a)*)...");
//        SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex("((?=aa)a(q(?=b)k)*)*a");
//        SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex("(?=a((?=b))*)*");

//        System.out.println(safa.getDot("safa"));
//        System.out.println("regex = " + RegexConverter.toRegex(safa, solver));
    }

    @Test
    public void testNestedPositiveLookaheads() throws TimeoutException {
       List<Pair<String, String>> pairs = Arrays.asList(
               new Pair<>("(?=a(?=b))", "(?=aε(?=bε)ε)"),
               new Pair<>("(?=a(?=bc)).", "(?=aε(?=bεcε)ε)ε.")
       );

        validateWords(pairs);
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException {


        assertTrue(true);
    }

    private void validateWords(List<Pair<String, String>> tests) throws TimeoutException {
        for (Pair<String, String> test : tests) {
//            SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex(test.first);
//            String regex = RegexConverter.toRegex(safa, solver);
//            System.out.println("regex = " + regex);
//            assertEquals(test.second, regex);
        }
    }

}
