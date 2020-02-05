package benchmark.regexconverter;

import automata.safa.SAFA;
import benchmark.SFT.codecs.Trie;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.Arrays;
import java.util.List;

public class TestSAFANegation {

    @Test
    public void testRemoveEpsilonTransitions01() throws TimeoutException {
        String regex = "(?=.*[a-z])(?=.*[0-9])...";
        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);

        Utils.validateRegexConstruction(noEps, 10, 14, 1, 4);
        Utils.validateRegexInputString(noEps, regex, Arrays.asList("a3d", "a", "333", "ddd", ",,,"));
    }

    @Test
    public void testRemoveEpsilonTransitions02() throws TimeoutException {
        String regex = "(?=a(?=b))ab";
        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateRegexInputString(noEps, regex, Arrays.asList("ab", "ba"));
    }

    @Test
    public void testNegation01() throws TimeoutException {
        String regex = "a(?!b(?!c))..";
        List<String> strings = Arrays.asList("abc", "abd", "acd");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);


//        SAFA<CharPred, Character> a = Utils.constructFromRegex(regex);
//        System.out.println(a.getDot("qqq"));
//        System.out.println(a);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        System.out.println(noEps.getDot("noEps"));
        System.out.println(noEps);
//        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation02() throws TimeoutException {
        String regex = "a|(?!a)aa";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation03() throws TimeoutException {
        String regex = "aa|(?!aa)a";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation04() throws TimeoutException {
        String regex = "(?!aa)..";
        List<String> strings = Arrays.asList("a", "aa", "aaa", "ab", "ba");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation05() throws TimeoutException {
        String regex = "a(?!b).";
        List<String> strings = Arrays.asList("ad", "ab", "ac");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation06() throws TimeoutException {
        String regex = "([ab]*)(?!b)c";
        List<String> strings = Arrays.asList("abc");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);

        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void testNegation07() throws TimeoutException {
        String regex = "a(?!b(?!c))..";
        List<String> strings = Arrays.asList("abc", "ada", "abe");

        Utils.validateRegexInputString(Utils.constructFromRegex(regex), regex, strings);

        System.out.println(Utils.constructFromRegex(regex).getDot("safa"));
        System.out.println(Utils.constructEpsilonFree(regex).getDot("noEps"));

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);

        System.out.println(noEps.getDot("noEps"));

        Utils.validateRegexInputString(noEps, regex, strings);
    }

    @Test
    public void test() throws TimeoutException {
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
        System.out.println(SAFA.isEquivalent(Utils.constructEpsilonFree("aa|(?!aa)a"), Utils.constructEpsilonFree("a|(?!a)aa"), solver, SAFA.getBooleanExpressionFactory()));
    }

}
