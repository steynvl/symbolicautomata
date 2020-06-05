package benchmark.regexconverter;

public class LookaheadWord {

    private String prefix;
    private String suffix;

    private String word;

    public LookaheadWord(String prefix) {
        this.prefix = prefix;
        suffix = "";
        word = prefix;
    }

    public LookaheadWord(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        word = prefix + suffix;
    }

    public String getWord() {
        return word;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String withDelimiter(char delimiter) {
        return String.format("%s%c%s", prefix, delimiter, suffix);
    }

}
