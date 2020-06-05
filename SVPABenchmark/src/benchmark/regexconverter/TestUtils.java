package benchmark.regexconverter;

import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;
import automata.AutomataException;
import automata.safa.SAFA;
import benchmark.SAFAProvider;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.HashStringEncodingUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class TestUtils {

    private static final UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();

    protected static void validateFullMatchRegexInputStrings(String regex,
                                                             List<LookaheadWord> strings)
            throws TimeoutException, AutomataException {
        Pattern pattern = Pattern.compile(regex);

        SAFAProvider safaProvider = new SAFAProvider(regex);
        SAFA<CharPred, Character> safa = safaProvider.constructSAFAFromREwLA();

        for (LookaheadWord lw : strings) {
            Matcher matcher = pattern.matcher(lw.getWord());

            /* only want java full matches */
            boolean found = matcher.matches();

            /* explicitly add #-marker to the end of the input string */
            String encoded = lw.getWord() + safaProvider.getSolver().getDelimiter();
            System.out.println("ENCODED = " + encoded);

            boolean accepts = safa.accepts(lOfS(encoded), solver);

            System.out.printf("%s [alternating:%b][java:%b]\n", lw.getWord(), accepts, found);
            assertEquals(found, accepts);
        }
    }

    public static void validateSubMatch(String regex,
                                        SAFA<CharPred, Character> safa,
                                        HashStringEncodingUnaryCharIntervalSolver solver,
                                        List<LookaheadWord> strings) throws TimeoutException {
        /* compile regex to java.util.regex.Pattern to test our model against */
        Pattern pattern = Pattern.compile(regex);

        for (LookaheadWord lw : strings) {
            Matcher matcher = pattern.matcher(lw.getWord());

            String withDelimiter = lw.withDelimiter(solver.getDelimiter());
            boolean accepts = safa.accepts(TestUtils.lOfS(withDelimiter), solver);
            boolean javaAccepts = matcher.find() && matcher.start() == 0;

            if (javaAccepts) {
                if (lw.getPrefix().equals(matcher.group(0))) {
                    System.out.printf("%s [alternating:%b][java:%b]\n", withDelimiter, accepts, javaAccepts);
                    assertTrue(accepts);
                }
            } else {
                System.out.println(lw.getWord() + " <<<");
                assertFalse(accepts);
                System.out.printf("%s [alternating:%b][java:%b]\n", withDelimiter, accepts, javaAccepts);
            }
        }

    }

    public static RegexNode parseRegex(String regex) {
        String[] str = { regex };

        List<RegexNode> nodes = RegexParserProvider.parse(str);
        assert nodes.size() > 0;

        return nodes.get(0);
    }

    public static List<Character> lOfS(String s) {
        List<Character> l = new ArrayList<>();
        char[] ca = s.toCharArray();
        for (int i = 0; i < s.length(); i++)
            l.add(ca[i]);
        return l;
    }

}
