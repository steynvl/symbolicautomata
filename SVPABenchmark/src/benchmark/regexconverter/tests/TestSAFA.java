package benchmark.regexconverter.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
    TestFullMatchSAFA.class,
    TestRegexTranslation.class,
    TestSAFANegation.class,
    TestSubMatchSAFA.class,
    TestSubMatchEquivalence.class,
    TestSAFAToRegex.class
})

public class TestSAFA { }