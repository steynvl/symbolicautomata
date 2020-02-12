package benchmark.regexconverter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
    TestFullMatchSAFA.class,
    TestRegexTranslation.class,
    TestSAFANegation.class,
    TestShuffleOperation.class
})

public class TestSAFA { }