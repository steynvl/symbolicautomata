package benchmark.regexconverter;

import automata.AutomataException;
import automata.safa.*;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestSAFANegation {

    @Test
    public void testManualNegation01() throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

        List<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();

        /* (a|b)c */
        transitions.add(new SAFAInputMove<>(0, pb.MkState(1), new CharPred('a')));
        transitions.add(new SAFAInputMove<>(0, pb.MkState(2), new CharPred('b')));
        transitions.add(new SAFAInputMove<>(1, pb.MkState(3), new CharPred('c')));
        transitions.add(new SAFAInputMove<>(2, pb.MkState(3), new CharPred('c')));

        Set<Integer> finalStates = new HashSet<>(Arrays.asList(3));

        SAFA<CharPred, Character> safa = SAFA.MkSAFA(transitions, pb.MkState(0), finalStates, new HashSet<>(), solver);
        List<String> matching = Arrays.asList("ac", "bc");
        List<String> nonMatching = Arrays.asList("ad", "bd", "cc", "ba");

        runWords(safa, solver, matching, nonMatching);

        SAFA<CharPred, Character> negated = safa.negate(solver);
        runWords(negated, solver, nonMatching, matching);
    }

    @Test
    public void testManualNegation02() throws TimeoutException {
        BooleanExpressionFactory<PositiveBooleanExpression> pb = SAFA.getBooleanExpressionFactory();
        UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

        List<SAFAMove<CharPred, Character>> transitions = new LinkedList<>();

        /* (a|b)c */
        transitions.add(new SAFAInputMove<>(0, pb.MkState(1), new CharPred('a')));
        transitions.add(new SAFAInputMove<>(0, pb.MkState(2), new CharPred('b')));
        transitions.add(new SAFAEpsilon<>(1, pb.MkState(3)));
        transitions.add(new SAFAEpsilon<>(2, pb.MkState(4)));
        transitions.add(new SAFAInputMove<>(3, pb.MkState(5), new CharPred('c')));
        transitions.add(new SAFAInputMove<>(4, pb.MkState(5), new CharPred('c')));

        Set<Integer> finalStates = new HashSet<>(Arrays.asList(5));

        SAFA<CharPred, Character> safa = SAFA.MkSAFA(transitions, pb.MkState(0), finalStates, new HashSet<>(), solver);
        List<String> matching = Arrays.asList("ac", "bc");
        List<String> nonMatching = Arrays.asList("ad", "bd", "cc", "ba");

        runWords(safa, solver, matching, nonMatching);

        SAFA<CharPred, Character> negated = safa.negate(solver);
        runWords(negated, solver, nonMatching, matching);
    }

    private static void runWords(SAFA<CharPred, Character> safa,
                                 UnaryCharIntervalSolver solver,
                                 List<String> matching,
                                 List<String> nonMatching) throws TimeoutException {
        for (String m : matching) {
            assertTrue(safa.accepts(TestUtils.lOfS(m), solver));
        }

        for (String nm : nonMatching) {
            assertFalse(safa.accepts(TestUtils.lOfS(nm), solver));
        }
    }

    @Test
    public void testNegation01() throws TimeoutException, AutomataException {
        String regex = "((?!aa)a)*a";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation02() throws TimeoutException, AutomataException {
        String regex = "a(?!b(?!c))..";
        List<String> strings = Arrays.asList("abc", "abd", "acd");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation03() throws TimeoutException, AutomataException {
        String regex = "a|(?!a)aa";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation04() throws TimeoutException, AutomataException {
        String regex = "aa|(?!aa)a";
        List<String> strings = Arrays.asList("a", "aa", "aaa");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation05() throws TimeoutException, AutomataException {
        String regex = "(?!aa)..";
        List<String> strings = Arrays.asList("a", "aa", "aaa", "ab", "ba");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation06() throws TimeoutException, AutomataException {
        String regex = "a(?!b).";
        List<String> strings = Arrays.asList("ad", "ab", "ac");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation07() throws TimeoutException, AutomataException {
        String regex = "([ab]*)(?!b)c";
        List<String> strings = Arrays.asList("abc");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation08() throws TimeoutException, AutomataException {
        String regex = "((?!a).)*";
        List<String> strings = Arrays.asList("bcd");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

    @Test
    public void testNegation09() throws TimeoutException, AutomataException {
        /* TODO */
        // new Pair<>("(?>(a|b)*)*b", Arrays.asList("ab", "aab", "b")),
        // new Pair<>("(?>((a|b)*)*)b", Arrays.asList("ab", "aab", "b")),
        // ((((((a|((?!(a)((a|b))))(b)))*)((?!(a|b)))))*)b
        String regex = "((((((a|((?!(a)((a|b))))(b)))*)((?!(a|b)))))*)b";

        List<String> strings = Arrays.asList("ab");

        List<LookaheadWord> lw = strings.stream().map(LookaheadWord::new).collect(Collectors.toList());
//        TestUtils.validateFullMatchRegexInputStrings(regex, lw);
    }

}
