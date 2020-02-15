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
    public void testLookaheads01() throws TimeoutException {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(?=aa)a", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("(?=aaa)aa", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaa")),
                new Pair<>("EB(?=AA)AAA", Arrays.asList("EBAA", "EBAAA")),
                new Pair<>("EB(?=AAAA)AAA", Arrays.asList("EBAA", "EBAAA", "EBAAAA", "EBAAAAA", "EBAAAB")),
                new Pair<>("(a(?=kl+)|qw*x)kl", Arrays.asList("akl", "qxkl", "qwxkl", "qwxkl", "akll"))
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
//                new Pair<>("", Arrays.asList("", "", "", "", "")),
        );

        validateTests(tests);
    }

    @Test
    public void testLookaheads02() throws TimeoutException {
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
