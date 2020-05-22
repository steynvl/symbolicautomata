package benchmark.regexconverter;

import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;
import automata.safa.SAFA;
import benchmark.SAFAProvider;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class Utils {

    private static final UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    public static SAFA<CharPred, Character> constructFullMatchFromRegex(String regex) {
        return new SAFAProvider(regex).getFullMatchSAFA();
    }

    public static SAFA<CharPred, Character> constructFullMatchFromNode(RegexNode node) throws TimeoutException {
        return RegexConverter.toSAFA(node, new UnaryCharIntervalSolver());
    }

    public static SAFA<CharPred, Character> constructEpsilonFree(String regex) throws TimeoutException {
        return SAFA.removeEpsilonMovesFrom(constructFullMatchFromRegex(regex), solver);
    }

    public static void validateFullMatchRegexInputStrings(SAFA<CharPred, Character> safa,
                                                          String regex,
                                                          List<String> strings) throws TimeoutException {
        Pattern pattern = Pattern.compile(regex);

        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);

            /* only want java full matches */
            boolean found = matcher.matches();

            System.out.printf("%s [alternating:%b][java:%b]\n", string, safa.accepts(lOfS(string), solver), found);
            assertEquals(found, safa.accepts(lOfS(string), solver));
        }
    }

    public static void validateFullMatchRegexInputStrings(String regex,
                                                          List<String> strings) throws TimeoutException {
        validateFullMatchRegexInputStrings(constructFullMatchFromRegex(regex), regex, strings);
    }

    public static void validateFullMatchRegexInputStrings(String regex,
                                                          List<String> matching,
                                                          List<String> nonMatching) throws TimeoutException {
        List<String> strings = new LinkedList<>(matching);
        strings.addAll(nonMatching);
        validateFullMatchRegexInputStrings(constructFullMatchFromRegex(regex), regex, strings);
    }

    public static void validateFullMatchRegexConstruction(SAFA<CharPred, Character> safa,
                                                          int stateCnt, int transitionCnt,
                                                          int finalStateCnt, int lookaheadCnt) {
        assertEquals(safa.getStates().size(), stateCnt);
        assertEquals((int) safa.getTransitionCount(), transitionCnt);
        assertEquals(safa.getFinalStates().size(), finalStateCnt);
        assertEquals(safa.getLookaheadFinalStates().size(), lookaheadCnt);
    }

    public static void validateFullMatchRegexConstruction(String regex, int stateCnt, int transitionCnt,
                                                          int finalStateCnt, int lookaheadCnt) {
        validateFullMatchRegexConstruction(constructFullMatchFromRegex(regex), stateCnt, transitionCnt, finalStateCnt, lookaheadCnt);
    }

    public static RegexNode parseRegex(String regex) {
        String[] str = {regex};

        List<RegexNode> nodes = RegexParserProvider.parse(str);
        assert nodes.size() > 0;

        return nodes.get(0);
    }

    public static String translateRegex(String regex) throws TimeoutException {
        StringBuilder sb = new StringBuilder();
        RegexConverter.toSAFA(parseRegex(regex), new UnaryCharIntervalSolver(), null, sb);
        return sb.toString();
    }

    public static String regexToString(RegexNode node) {
        StringBuilder sb = new StringBuilder();
        node.toString(sb);
        return sb.toString();
    }

    public static void printDot(String regex) {
        System.out.println(new SAFAProvider(regex).getFullMatchSAFA().getDot("safa"));
    }

    public static void printTransitions(SAFA<CharPred, Character> safa) {
        safa.getTransitions().forEach(
                t -> System.out.printf("%s [%s]\n", t, t.isEpsilonTransition() ? "ε" : t.guard)
        );
    }

    public static List<Character> lOfS(String s) {
        List<Character> l = new ArrayList<>();
        char[] ca = s.toCharArray();
        for (int i = 0; i < s.length(); i++)
            l.add(ca[i]);
        return l;
    }

}
