package benchmark;

import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;
import automata.safa.SAFA;
import benchmark.regexconverter.RegexConverter;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.List;

public class SAFAProvider {

    public SAFAProvider(String regex) {

    }

    public SAFA<CharPred, Character> getFullMatchSAFA() {
        return null;
    }

    public SAFA<CharPred, Character> getSubMatchSAFA() {
        return null;
    }

    public SAFAProvider(String regex, UnaryCharIntervalSolver solver){
        String[] str = {regex};
        List<RegexNode> nodes = RegexParserProvider.parse(str);

        try {
            this.mySAFA = RegexConverter.toSAFA(nodes.get(0), solver);
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public SAFA<CharPred, Character> getSAFA(){
        return mySAFA;
    }

    private SAFA<CharPred, Character> mySAFA;
}
