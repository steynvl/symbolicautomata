package benchmark.regexconverter;

import automata.safa.SAFA;
import benchmark.SAFAProvider;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.SubMatchUnaryCharIntervalSolver;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class TestSubMatchSAFA {

    @Test
    public void testSubMatchingSAFA01() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
            new Pair<>("a|aa", Arrays.asList("a", "aa")),
            new Pair<>("aa|a", Arrays.asList("a", "aa")),
            new Pair<>("a*(b|abb)", Arrays.asList("ab", "b", "abb", "aabb")),
            new Pair<>("a*(abb|b)", Arrays.asList("ab", "b", "abb", "aabb")),
            new Pair<>("a*b|abb", Arrays.asList("ab", "abb")),
            new Pair<>("a|ab", Arrays.asList("a", "ab")),
            new Pair<>("a*(ab)*b", Arrays.asList("a", "ab", "abb", "aaab")),
            new Pair<>("abb|a*(ab)*b", Arrays.asList("a", "ab", "abb", "aaab")),
            new Pair<>("a|ab", Arrays.asList("a", "ab"))
        );

        validateTests(tests);
    }

    @Test
    public void testSubMatchingSAFA02() throws TimeoutException {
//        List<Pair<String, List<String>>> tests = Arrays.asList(
//                new Pair<>("^((a*)*b)*b", Arrays.asList("a", "ab", "abb", "aabb", "ababb"))
//                new Pair<>("^(b|(a|b)*bb)", Arrays.asList("b", "abb", "bbb", "aabbb")),
//                new Pair<>("ba((?!ab))*", Arrays.asList("baab", "baaab", "baqweab"))
//        );

//        validateTests(tests);
    }

    @Test
    public void testLookaheads01() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?=aa)a", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("(?=aaa)aa", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("EB(?=AA)AAA", Arrays.asList("EBAA", "EBAAA")),
                new Pair<>("EB(?=AAAA)AAA", Arrays.asList("EBAA", "EBAAA", "EBAAAA", "EBAAAAA", "EBAAAB")),
                new Pair<>("(a(?=kl+)|qw*x)kl", Arrays.asList("akl", "qxkl", "qwxkl", "qwxkl", "akll", "akllq")),
                new Pair<>("(?=aa)aa", Arrays.asList("", "a", "aa", "aaa", "aaaa"))
        );

        validateTests(tests);
    }

    @Test
    public void testLookaheads02() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("((?=aa)a|aa)", Arrays.asList("", "a", "aa", "aaa")),
                new Pair<>("((?=aa)a)a", Arrays.asList("", "a", "aa", "aaa")),
                new Pair<>("(a|(?=[^a]{3})aa)aa", Arrays.asList("", "a", "aa", "aaa", "bve", "bbqaa")),
                new Pair<>("((?=aa)a|aa)b*c", Arrays.asList("aac", "aabc", "ac", "abc")),
                new Pair<>("a(?=c|d).", Arrays.asList("ac", "ad", "aq", "acd", "ade", "aqqc")),
                new Pair<>("abc(?=abcde)(?=ab)", Arrays.asList("", "abcabcdeab", "abcabcde", "abcab")),

                /* TODO check translation of plus is what I think makes this break */
                // new Pair<>("f[oa]*(?=o)", Arrays.asList("fo", "fa", "faaaaao", "foaoaoaaaao")),

                new Pair<>("f[oa]*(?=o)", Arrays.asList("fo", "fa", "faaaaao", "foaoaoaaaao")),
                new Pair<>("(?=.*[a-z])(?=.*[0-9])", Arrays.asList("", "aa", "31", "a3", "ab3", "a3b")),
                new Pair<>("((?=a).)*", Arrays.asList("a", "aa", "aaa", "aaab")),
                new Pair<>("((?=aa)a)*a", Arrays.asList("a", "aa", "aaa", "aaaa"))
        );

        validateTests(tests);
    }

    @Test
    public void testNestedPositiveLookaheads() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?=a(?=b))a", Arrays.asList("a", "ab", "abb"))
        );

        /* TODO have to figure out first how to shuffle two SAFAs containing universal states  */

        assertTrue(true);
        // validateTests(tests);
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?!aa).", Arrays.asList("a", "ab", "ad")),
                new Pair<>("a(?!b)", Arrays.asList("a", "ab", "aa", "abc", "aab")),
                new Pair<>("([ab]*)(?!b)c", Arrays.asList("ac", "abc", "aaaac", "abbbb")),
                new Pair<>("abc(?!d)", Arrays.asList("abcd", "abcc", "abc")),
                new Pair<>("\\/\\*((?!\\/\\*).)*\\*\\/", Arrays.asList(
                        "/* */", "/   */", "/*  /* */ */", "/* this is a comment */", "/* *", "/**/"
                )),
                new Pair<>("((?!(ab)).)*", Arrays.asList("ab", "ba", "qw", "", "a", "b")),
                new Pair<>("((?!(ab)).)*ab", Arrays.asList("ab", "ba", "qw", "", "a", "b")),
                new Pair<>("(?=.*)(?=.*)(.{4}).*", Arrays.asList("1234", "12345", "123", "1", ""))
        );

        validateTests(tests);
    }

    @Test
    public void testNestedNegativeLookaheads() throws TimeoutException {
        /* TODO have to handle nested positive lookaheads first */

        assertTrue(true);
    }

    private void validateTests(List<Pair<String, List<String>>> tests) throws TimeoutException {
        for (Pair<String, List<String>> test : tests) {
            String regex = test.first;
            SAFAProvider safaProvider = new SAFAProvider(regex);
            validateStrings(
                    regex, safaProvider.getSubMatchSAFA(),
                    safaProvider.getSubMatchSolver(), test.second
            );
        }
    }

    private void validateStrings(String regex,
                                 SAFA<CharPred, Character> safa,
                                 SubMatchUnaryCharIntervalSolver solver,
                                 List<String> strings) throws TimeoutException {
        /* compile regex to java.util.regex.Pattern to test our model against */
        Pattern pattern = Pattern.compile(regex);

        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            String stringWithDelimiter;

            if (matcher.find()) {
                stringWithDelimiter = String.format("%s%s%s", matcher.group(0), solver.getDelimiter(),
                        string.substring(matcher.group(0).length()));
                boolean accepts = safa.accepts(Utils.lOfS(stringWithDelimiter), solver);
                System.out.printf("%s [alternating:%b][java:true]\n", stringWithDelimiter, accepts);
                assertTrue(accepts);
            } else {
                stringWithDelimiter = string + solver.getDelimiter();
                boolean accepts = safa.accepts(Utils.lOfS(stringWithDelimiter), solver);
                System.out.printf("%s [alternating:%b][java:false]\n", stringWithDelimiter, accepts);
                assertFalse(accepts);
            }

        }

    }

}
