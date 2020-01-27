package benchmark.regexconverter;

import RegexParser.RegexNode;
import utilities.Pair;
import utilities.Quadruple;

import java.util.ArrayList;
import java.util.List;

public class SubMatchEquivalence {

    public static Pair<Boolean, List<String>> testEquivalence(String regex1, String regex2) {
        Quadruple<Character, Character, Character, Character> capturingBrackets;
        RegexNode nodes1 = Utils.parseRegex(regex1);
        RegexNode nodes2 = Utils.parseRegex(regex2);
//        capturingBrackets =

        RegexNode translated1 = RegexTranslator.translate(nodes1);
        RegexNode translated2 = RegexTranslator.translate(nodes2);

        return new Pair<>(true, new ArrayList<>());
    }

    private static Quadruple<Character, Character, Character, Character> findCapturingCandidates() {


        return null;
    }

}
