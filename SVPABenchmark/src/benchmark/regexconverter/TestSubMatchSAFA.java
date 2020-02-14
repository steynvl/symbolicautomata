package benchmark.regexconverter;

import automata.safa.SAFA;
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

    private Pair<SAFA<CharPred, Character>, SubMatchUnaryCharIntervalSolver> pair;

    @Test
    public void testSubMatchSAFA01() throws TimeoutException {
//        pair = RegexSubMatching.constructSubMatchingSAFA("a|aa");
        pair = RegexSubMatching.constructSubMatchingSAFA("(?=aaa)aa");

        SAFA<CharPred, Character> safa = pair.first;
        SubMatchUnaryCharIntervalSolver solver = pair.second;

        System.out.println(safa.getDot("safa"));
        System.out.println("delimiter = " + solver.getDelimiter());
        System.out.println(solver.HasModel(solver.True(), 'a'));
        System.out.println(solver.HasModel(solver.True(), solver.getDelimiter()));

        validateStrings("(?=aaa)aa", safa, solver, Arrays.asList("aa", "aaa"));

    }

    private void validateStrings(String regex,
                                 SAFA<CharPred, Character> safa,
                                 SubMatchUnaryCharIntervalSolver solver,
                                 List<String> strings) throws TimeoutException {
        Pattern pattern = Pattern.compile(regex);

        for (String string : strings) {
            Matcher matcher = pattern.matcher(string);
            String stringWithDelimiter;

            if (matcher.find()) {
                stringWithDelimiter = String.format("%s%s%s", matcher.group(0), solver.getDelimiter(),
                        string.substring(matcher.group(0).length()));
                assertTrue(safa.accepts(Utils.lOfS(stringWithDelimiter), solver));
            } else {
                stringWithDelimiter = string + solver.getDelimiter();
                assertFalse(safa.accepts(Utils.lOfS(stringWithDelimiter), solver));
            }

        }

    }

}
