package benchmark.regexconverter;

import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.HashEncodingUnaryCharIntervalSolver;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.util.Arrays;

public class TestFullMatchSAFA {

    @Test
    public void testConcatAndUnion() throws TimeoutException {
        String regex = "abc|de";
        Utils.validateFullMatchRegexConstruction(regex, 12, 12, 1, 0);
        Utils.validateFullMatchRegexInputStrings(regex, Arrays.asList("abc", "de"), Arrays.asList("ab"));
    }

    @Test
    public void testMetaChar() throws TimeoutException {
        Utils.validateFullMatchRegexConstruction("\\s", 2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("\\s", Arrays.asList(" "), Arrays.asList("a"));

        Utils.validateFullMatchRegexConstruction("\\S", 2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("\\S", Arrays.asList("a"), Arrays.asList(" "));

        Utils.validateFullMatchRegexConstruction("\\w", 2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("\\w", Arrays.asList("a", "_", "1"), Arrays.asList(" "));
    }

    @Test
    public void testCharacterClass() throws TimeoutException {
        Utils.validateFullMatchRegexConstruction("[\\d]", 2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("[\\d]", Arrays.asList("1"), Arrays.asList("10"));

        Utils.validateFullMatchRegexConstruction("[\\\\a123bc!@#$s%^&*(){}]",
                2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("[\\\\a123bc!@#$s%^&*(){}]",
                Arrays.asList("1", "a", "^", "s"), Arrays.asList("10"));

        Utils.validateFullMatchRegexConstruction("[a-zA-Z1-9]", 2, 1, 1, 0);
        Utils.validateFullMatchRegexInputStrings("[a-zA-Z1-9]", Arrays.asList("8", "b", "c", "C"), Arrays.asList("10"));

        Utils.validateFullMatchRegexConstruction("\\a|[b-zA-Z1-9]", 6, 6, 1, 0);
        Utils.validateFullMatchRegexInputStrings("\\a|[b-zA-Z1-9]", Arrays.asList(), Arrays.asList("\\a"));
    }

    @Test
    public void testQuantifiers() throws TimeoutException {
        Utils.validateFullMatchRegexConstruction("[\\d]+", 5, 5, 1, 0);
        Utils.validateFullMatchRegexInputStrings("[\\d]+", Arrays.asList("1234567", "111131", "111123459"), Arrays.asList("a"));

        Utils.validateFullMatchRegexConstruction("a*", 3, 3, 1, 0);
        Utils.validateFullMatchRegexInputStrings("a*", Arrays.asList("", "a", "aaaa"), Arrays.asList("b"));

        Utils.validateFullMatchRegexConstruction("[abc]*", 3, 3, 1, 0);
        Utils.validateFullMatchRegexInputStrings("[abc]*", Arrays.asList("", "aaaa", "abcbcbcbcc"), Arrays.asList("d"));

        Utils.validateFullMatchRegexConstruction("de?|f[abc]?", 16, 18, 1, 0);
        Utils.validateFullMatchRegexInputStrings("de?|f[abc]?", Arrays.asList("d", "de", "fa", "f"), Arrays.asList("def"));
    }

    @Test
    public void testRepetition() throws TimeoutException {
        Utils.validateFullMatchRegexInputStrings("[\\d]{2}", Arrays.asList("1", "11"));
        Utils.validateFullMatchRegexInputStrings("a{2,}", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaaaaaa"));
        Utils.validateFullMatchRegexInputStrings("1{2,3}", Arrays.asList("1", "11", "111", "1111", "1111111"));
        Utils.validateFullMatchRegexInputStrings(".{1,3}", Arrays.asList("1", "12", "123", "1234", ""));
        Utils.validateFullMatchRegexInputStrings(".{4}", Arrays.asList("1", "12", "123", "1234", "12345", ""));
    }

    @Test
    public void testPositiveLookaheads() throws TimeoutException {
        Utils.validateFullMatchRegexConstruction("EB(?=AA)AAA", 19, 18, 1, 1);
        Utils.validateFullMatchRegexInputStrings("EB(?=AA)AAA", Arrays.asList("EBAAA"), Arrays.asList("EBAA"));

        Utils.validateFullMatchRegexInputStrings("(?=aa)aab", Arrays.asList("aab"), Arrays.asList("aa", "aaba"));
        Utils.validateFullMatchRegexInputStrings("(a(?=b)|qw*x)kl", Arrays.asList("qxkl", "qwxkl"), Arrays.asList("akl"));

        Utils.validateFullMatchRegexInputStrings("((?=aa)a|aa)", Arrays.asList("aa"), Arrays.asList("aaa", "aaaa"));
        Utils.validateFullMatchRegexInputStrings("((?=aa)a)a", Arrays.asList("aa"), Arrays.asList("aaa", "aaaa"));
        Utils.validateFullMatchRegexInputStrings("(?=aa)aa", Arrays.asList("aa"), Arrays.asList("a", "aaa"));
        Utils.validateFullMatchRegexInputStrings("(a|(?=[^a]{3})aa)aa", Arrays.asList("aaa"), Arrays.asList("aaaa"));
        Utils.validateFullMatchRegexInputStrings("((?=aa)a|aa)b*c",
                Arrays.asList("aac", "aabc", "aabbbc"), Arrays.asList("ac", "abc"));

        Utils.validateFullMatchRegexInputStrings("a(?=d).", Arrays.asList("ad"), Arrays.asList("a", "aa", "adb"));
        Utils.validateFullMatchRegexInputStrings("a(?=c|d).", Arrays.asList("ac", "ad"), Arrays.asList("ae", "adac"));

        StringBuilder sb = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("a");
            s1.append("aaaaa");
            s2.append("aaaaaaaaaaa");
        }
        Utils.validateFullMatchRegexInputStrings("(?=a)(" + sb.toString() + ")+",
                Arrays.asList(s1.toString(), s2.toString()), Arrays.asList("a"));

        Utils.validateFullMatchRegexInputStrings("abc(?=xyz)", Arrays.asList(), Arrays.asList("abcxyz"));
        Utils.validateFullMatchRegexInputStrings("abc(?=xyz)...", Arrays.asList("abcxyz"), Arrays.asList("abcxxz"));

        Utils.validateFullMatchRegexInputStrings("abc(?=abcde)(?=ab)", Arrays.asList(), Arrays.asList("abcabcdefg"));
        Utils.validateFullMatchRegexInputStrings("ab(?=c)\\wd\\w\\w", Arrays.asList("abcdef"), Arrays.asList());
        Utils.validateFullMatchRegexInputStrings("((?=([^a]{2})d)\\w{3})\\w\\w", Arrays.asList(),
                Arrays.asList("abcdef", "abccef"));

        Utils.validateFullMatchRegexInputStrings("f[oa]+(?=o)", Arrays.asList(), Arrays.asList("faaao"));

        Utils.validateFullMatchRegexInputStrings("(?=.*[a-z])(?=.*[0-9])...",
                Arrays.asList("a3d"), Arrays.asList("333", "a3", "aqq"));

        Utils.validateFullMatchRegexInputStrings("((?=a).)*", Arrays.asList("", "a", "aa", "aaa"), Arrays.asList("b"));

        Utils.validateFullMatchRegexInputStrings("(?=.*a)(?=.*b)(?=.*c).*",
                Arrays.asList("abc", "anvbajxcz", "cba"), Arrays.asList("ab", "ac", "cbqq"));

        Utils.validateFullMatchRegexInputStrings("((?=aa)a)*a", Arrays.asList("a", "aa", "aaa", "aaaa", "aaab"));
        Utils.validateFullMatchRegexInputStrings("(?=.*)(?=.*)(.{4}).*", Arrays.asList("1234", "12345", "123"));
    }

    @Test
    public void testNestedPositiveLookaheads() throws TimeoutException {
        Utils.validateFullMatchRegexInputStrings("(?=a(?=b))...", Arrays.asList("abc"), Arrays.asList());
        Utils.validateFullMatchRegexInputStrings("(?=a(?=bc))...", Arrays.asList("abc"), Arrays.asList());
        Utils.validateFullMatchRegexInputStrings("(?=a(?=b)b)...", Arrays.asList("abc"), Arrays.asList());
        Utils.validateFullMatchRegexInputStrings("(?=a(?=b)c)...", Arrays.asList(), Arrays.asList("abc"));
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException {
        Utils.validateFullMatchRegexInputStrings("(?!aa)..", Arrays.asList("ab", "ba", "bb"), Arrays.asList("aa"));
        Utils.validateFullMatchRegexInputStrings("a(?!b).", Arrays.asList("ad", "ac"), Arrays.asList("ab"));
        Utils.validateFullMatchRegexInputStrings("([ab]*)(?!b)c", Arrays.asList("abc"), Arrays.asList());
        Utils.validateFullMatchRegexInputStrings("abc(?!d).", Arrays.asList("abce"), Arrays.asList("abcd"));

        Utils.validateFullMatchRegexInputStrings("\\/\\*((?!\\/\\*).)*\\*\\/", Arrays.asList(
                "/* */", "/   */", "/*  /* */ */", "/* this is a comment */", "/* *", "/**/"
        ));

        Utils.validateFullMatchRegexInputStrings("((?!(ab)).)*", Arrays.asList("ab", "ba", "qw", "", "a", "b"));
        Utils.validateFullMatchRegexInputStrings("((?!(ab)).)ab*", Arrays.asList("ab", "ba", "qw", "", "a", "b"));
        Utils.validateFullMatchRegexInputStrings("(a|(?!ab))*b", Arrays.asList("a", "b", "ab", "bb", "bbb"));
        Utils.validateFullMatchRegexInputStrings("(?=.*)(?=.*)(.{4}).*", Arrays.asList("1234", "12345", "123"));
    }

    @Test
    public void testNestedNegativeLookaheads() throws TimeoutException {
        Utils.validateFullMatchRegexInputStrings("a(?!b(?!c))..",
                Arrays.asList("abc"), Arrays.asList("ada", "abe"));
    }

    @Test
    public void testFullMatchAnchors() throws TimeoutException {
        String regex = "(?=(ab)*b)(?=(a|b)*bba*)a";
        SAFA<CharPred, Character> safa = Utils.constructFullMatchFromRegex(regex);
//        System.out.println(safa.getDot("safa"));

//        regex = "((?=a*b$)a)*b$";
//        safa = Utils.constructFullMatchFromRegex(regex);
//        System.out.println(safa.getDot("safa"));

        regex = "((?=a*b$)ab*)*b$";
        safa = Utils.constructFullMatchFromRegex(regex);
        System.out.println(safa.getDot("safa"));
    }

    @Test
    public void testSubMatchAnchors() throws TimeoutException {
        String regex = "(?=(ab)*b)(?=(a|b)*bba*)a";
        Pair<SAFA<CharPred, Character>, HashEncodingUnaryCharIntervalSolver> p = RegexSubMatching.constructSubMatchingSAFA(regex);

//        System.out.println(p.first.getDot("safa"));

//        regex = "((?=a*b$)a)*b$";
//        p = RegexSubMatching.constructSubMatchingSAFA(regex);
//        System.out.println(p.first.getDot("safa"));
        regex = "((?=a*b$)ab*)*b$";
        p = RegexSubMatching.constructSubMatchingSAFA(regex);
        System.out.println(p.first.getDot("safa"));
    }

    @Test
    public void testBrinkIdea() throws TimeoutException {
        SAFA<CharPred, Character> safa = Utils.constructFullMatchFromRegex("(a|b)*c");
        System.out.println(safa.getDot("safa"));
//        SAFA<CharPred, Character> del = Utils.constructFullMatchFromRegex("#?");
//        SAFA<CharPred, Character> shuffle = SAFA.shuffle(safa, del, new UnaryCharIntervalSolver());
//        System.out.println(shuffle.getDot("shuffle"));
//        System.out.printf("Shuffle states = %d\n", shuffle.getStates().size());
//        System.out.printf("Shuffle transitions = %d\n", shuffle.getTransitions().size());

        SAFA<CharPred, Character> manual = Utils.constructFullMatchFromRegex("#?(a#?|b#?)*c#?");
        SAFA<CharPred, Character> manualNoEps = SAFA.removeEpsilonMovesFrom(manual, new UnaryCharIntervalSolver());
        System.out.println(manualNoEps.getDot("manualNoEps"));
        System.out.printf("Shuffle states = %d\n", manualNoEps.getStates().size());
        System.out.printf("Shuffle transitions = %d\n", manualNoEps.getTransitions().size());
//        System.out.println(SAFA.isEquivalent(shuffle, manualNoEps, new UnaryCharIntervalSolver(), SAFA.getBooleanExpressionFactory()));
    }

}
