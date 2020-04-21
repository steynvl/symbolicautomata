package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;

import java.util.Arrays;
import java.util.List;

public class TestSAFANegation {

    @Test
    public void testRemoveEpsilonTransitions01() throws TimeoutException {
        String regex = "(?=.*[a-z])(?=.*[0-9])...";
        System.out.println(Utils.constructFullMatchFromRegex(regex).getDot("safa"));
//        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);

//        Utils.validateFullMatchRegexConstruction(noEps, 10, 14, 1, 4);
//        Utils.validateFullMatchRegexInputStrings(noEps, regex, Arrays.asList("a3d", "a", "333", "ddd", ",,,"));
    }

    @Test
    public void testRemoveEpsilonTransitions02() throws TimeoutException {
        String regex = "(?=a(?=b))ab";
        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, Arrays.asList("ab", "ba"));
    }

    @Test
    public void testNegation01() throws TimeoutException {
        String regex = "a(?!b(?!c))..";
        List<String> strings = Arrays.asList("abc", "abd", "acd");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

    @Test
    public void testNegation02() throws TimeoutException {
        String regex = "a|(?!a)aa";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

    @Test
    public void testNegation03() throws TimeoutException {
        String regex = "aa|(?!aa)a";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

    @Test
    public void testNegation04() throws TimeoutException {
        String regex = "(?!aa)..";
        List<String> strings = Arrays.asList("a", "aa", "aaa", "ab", "ba");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

    @Test
    public void testNegation05() throws TimeoutException {
        String regex = "a(?!b).";
        List<String> strings = Arrays.asList("ad", "ab", "ac");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);
        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

    @Test
    public void testNegation06() throws TimeoutException {
        String regex = "([ab]*)(?!b)c";
        List<String> strings = Arrays.asList("abc");

        Utils.validateFullMatchRegexInputStrings(Utils.constructFullMatchFromRegex(regex), regex, strings);

        SAFA<CharPred, Character> noEps = Utils.constructEpsilonFree(regex);

        Utils.validateFullMatchRegexInputStrings(noEps, regex, strings);
    }

}
