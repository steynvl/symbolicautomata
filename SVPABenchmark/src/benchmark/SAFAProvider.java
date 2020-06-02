package benchmark;

import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;
import automata.safa.SAFA;
import benchmark.regexconverter.RegexConverter;
import benchmark.regexconverter.RegexSubMatching;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.HashEncodingUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.util.List;

public class SAFAProvider {

    private SAFA<CharPred, Character> fullMatchSAFA = null;
    private UnaryCharIntervalSolver fullMatchSolver = new UnaryCharIntervalSolver();

    private SAFA<CharPred, Character> subMatchSAFA = null;
    private HashEncodingUnaryCharIntervalSolver subMatchSolver;

    private String regex;

    public SAFAProvider(String regex) {
        this.regex = regex;
    }

    public SAFA<CharPred, Character> getFullMatchSAFA() {
        if (fullMatchSAFA == null) {
            List<RegexNode> nodes = RegexParserProvider.parse(new String[]{ regex });
            try {
                fullMatchSAFA = RegexConverter.toSAFA(nodes.get(0), fullMatchSolver);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return fullMatchSAFA;
    }

    public UnaryCharIntervalSolver getFullMatchSolver() {
        return fullMatchSolver;
    }

    public SAFA<CharPred, Character> getSubMatchSAFA() {
        if (subMatchSAFA == null) {
            try {
                Pair<SAFA<CharPred, Character>, HashEncodingUnaryCharIntervalSolver> p = RegexSubMatching.constructSubMatchingSAFA(regex);
                subMatchSAFA = p.first;
                subMatchSolver = p.second;
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return subMatchSAFA;
    }

    public HashEncodingUnaryCharIntervalSolver getSubMatchSolver() {
        if (subMatchSAFA == null) {
            getSubMatchSAFA();
        }

        return subMatchSolver;
    }

}
