package benchmark.regexconverter;


import java.util.Optional;

/**
 *  Used to build the regex for a given transition during
 *  the state elimination algorithm
 *
 */
public class RegexBuilder {

    public String alphaQ1_Q2;
    public String alphaQ1_Q;
    public String alphaQ_Q;
    public String alphaQ_Q2;

    public Optional<String> buildRegex() {
        String alphaQ_Q = this.alphaQ_Q == null ? "" : String.format("(%s)*", this.alphaQ_Q);
        if (alphaQ1_Q2 != null && alphaQ1_Q != null && alphaQ_Q2 != null) {
            return Optional.of(String.format("(%s|%s%s%s)", alphaQ1_Q2, alphaQ1_Q, alphaQ_Q, alphaQ_Q2));
        } else if (alphaQ1_Q != null && alphaQ_Q2 != null) {
            return Optional.of(String.format("%s%s%s", alphaQ1_Q, alphaQ_Q, alphaQ_Q2));
        } else if (alphaQ1_Q2 != null) {
            return Optional.of(alphaQ1_Q2);
        } else {
            return Optional.empty();
        }
    }

    public void setAlphaQ1_Q2(String regex) {
//        assert alphaQ1_Q2 == null;
        alphaQ1_Q2 = regex;
    }

    public void setAlphaQ1_Q(String regex) {
//        assert alphaQ1_Q == null;
        alphaQ1_Q = regex;
    }

    public void setAlphaQ_Q(String regex) {
//        assert alphaQ_Q == null;
        alphaQ_Q = regex;
    }

    public void setAlphaQ_Q2(String regex) {
//        assert alphaQ_Q2 == null;
        alphaQ_Q2 = regex;
    }

}
