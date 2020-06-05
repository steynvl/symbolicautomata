package benchmark.regexconverter;

public class RegexTranslationException extends Exception {

    private static final long serialVersionUID = 1L;

    public RegexTranslationException() {
        super();
    }

    public RegexTranslationException(String message) {
        super(message);
    }

    public RegexTranslationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegexTranslationException(Throwable cause) {
        super(cause);
    }
}

