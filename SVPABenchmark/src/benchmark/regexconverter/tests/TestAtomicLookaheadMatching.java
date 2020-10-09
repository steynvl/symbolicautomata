package benchmark.regexconverter.tests;

import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;
import benchmark.regexconverter.AtomicLookaheadMatching;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestAtomicLookaheadMatching {

    @Test
    public void testUnion() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("a|aa", Arrays.asList("aa")),
                new Pair<>("aaa|aa", Arrays.asList("aaa")),
                new Pair<>("(a|aa)a(a|aa)", Arrays.asList("aaa", "aaaa")),
                new Pair<>("aa|a", Arrays.asList("aa", "a")),
                new Pair<>("a|ab", Arrays.asList("a", "ab"))
        );

        for (Pair<String, List<String>> test : tests) {
            for (String word : test.second) {
                compareAgainstJava(test.first, word);
            }
        }
    }

    @Test
    public void testStar() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("a*(b|abb)", Arrays.asList("ab", "b", "abb", "aabb")),
                new Pair<>("a*(abb|b)", Arrays.asList("ab", "b", "abb", "aabb")),
                new Pair<>("a*b|abb", Arrays.asList("ab", "abb")),
                new Pair<>("a*(ab)*b", Arrays.asList("a", "ab", "abb", "aaab")),
                new Pair<>("abb|a*(ab)*b", Arrays.asList("a", "ab", "abb", "aaab"))
        );

        for (Pair<String, List<String>> test : tests) {
            for (String word : test.second) {
                compareAgainstJava(test.first, word);
            }
        }
    }

    @Test
    public void testAtomicOperator() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?>a|aa)", Arrays.asList("a", "aa")),
                new Pair<>("(?>aa|a)", Arrays.asList("a", "aa")),
                new Pair<>("(?>a*(ab)*b)", Arrays.asList("b", "ab", "aab", "abb")),
                new Pair<>("(?>a*(b|abb))", Arrays.asList("", "b", "ab", "aab", "abb")),
                new Pair<>("(?>a*(abb|b))", Arrays.asList("", "b", "ab", "aab", "abb")),
                new Pair<>("(?>abb|a*(ab)*b)", Arrays.asList("", "b", "ab", "aab", "abb")),
                new Pair<>("a*(?>bc|b)c", Arrays.asList("abc")),
                new Pair<>("(?>a*)", Arrays.asList("a", "aa")),
                new Pair<>("(?>n|n1)@", Arrays.asList("n@", "n1@")),
                new Pair<>("a(?>bc|b)c", Arrays.asList("abc")),
                new Pair<>("(?>a*(b*a*)(a*b*))", Arrays.asList("abab")),
                new Pair<>("(?>(a|b)*)*b", Arrays.asList("", "ab", "aab", "b"))
        );

        for (Pair<String, List<String>> test : tests) {
            for (String word : test.second) {
                compareAgainstJava(test.first, word);
            }
        }
    }

    @Test
    public void testLookAheads() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?=aa)a", Arrays.asList("a", "aa", "aaa")),
                new Pair<>("(?=aaa)aa", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("((?=aa)a|aa)", Arrays.asList("", "a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("((?=aa)a)a", Arrays.asList("", "a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("((?=aa)a|aa)b*c", Arrays.asList("aac", "aabc", "ac", "abc")),
                new Pair<>("((?=a)a)*", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa"))
        );

        for (Pair<String, List<String>> test : tests) {
            for (String word : test.second) {
                compareAgainstJava(test.first, word);
            }
        }
    }

    public void compareAgainstJava(String regex, String word) throws TimeoutException {
        List<RegexNode> nodes = RegexParserProvider.parse(new String[]{ regex });

        List<Pair<String, String>> prefixes = AtomicLookaheadMatching.match(nodes.get(0), word);
        List<String> first = prefixes.stream().map(p -> p.first).collect(Collectors.toList());

        Matcher javaMatcher = Pattern.compile(regex).matcher(word);

        /* compare our match to Java full match  */
        if (javaMatcher.matches()) {
            assertTrue(first.contains(word));
        } else {
            assertFalse(first.contains(word));
        }

        /* compare preferred prefix  */
        javaMatcher.reset();
        if (javaMatcher.find() && javaMatcher.start() == 0) {
            assertEquals(javaMatcher.group(), prefixes.get(0).first);
        } else {
            javaMatcher.reset();
            if (!javaMatcher.find()) {
                assertEquals(0, prefixes.size());
            }
        }
    }

}
