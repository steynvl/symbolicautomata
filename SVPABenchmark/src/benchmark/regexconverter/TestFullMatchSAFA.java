package benchmark.regexconverter;

import automata.AutomataException;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;

public class TestFullMatchSAFA {

    @Test
    public void testConcatAndUnion() throws TimeoutException, AutomataException {
        String regex = "abc|de";
        TestUtils.validateFullMatchRegexInputStrings(regex, Arrays.asList(
                new LookaheadWord("abc"), new LookaheadWord("de"),
                new LookaheadWord("ab"))
        );
    }

    @Test
    public void testMetaChar() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("\\s", Arrays.asList(
                new LookaheadWord(" "), new LookaheadWord("a")
        ));

        TestUtils.validateFullMatchRegexInputStrings("\\S", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord(" ")
        ));

        TestUtils.validateFullMatchRegexInputStrings("\\w", Arrays.asList(
                new LookaheadWord("_"), new LookaheadWord("a"),
                new LookaheadWord("1"), new LookaheadWord(" ")
        ));
    }

    @Test
    public void testCharacterClass() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("[\\d]", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("10")
        ));

        TestUtils.validateFullMatchRegexInputStrings("[\\\\a123bc!@#$s%^&*(){}]", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("a"),
                new LookaheadWord("^"), new LookaheadWord("s"),
                new LookaheadWord("10")
        ));

        TestUtils.validateFullMatchRegexInputStrings("[a-zA-Z1-9]", Arrays.asList(
                new LookaheadWord("8"), new LookaheadWord("b"),
                new LookaheadWord("c"), new LookaheadWord("C"),
                new LookaheadWord("10")
        ));


        TestUtils.validateFullMatchRegexInputStrings("\\a|[a-zA-Z1-9]", Arrays.asList(
                new LookaheadWord("8"), new LookaheadWord("b"),
                new LookaheadWord("\\a"), new LookaheadWord("C")
        ));
    }

    @Test
    public void testQuantifiers() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("[\\d]+", Arrays.asList(
                new LookaheadWord("1234567"), new LookaheadWord("111131"),
                new LookaheadWord("111123459"), new LookaheadWord("a")
        ));

        TestUtils.validateFullMatchRegexInputStrings("a*", Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("a"),
                new LookaheadWord("aaaaaa"), new LookaheadWord("b")
        ));

        TestUtils.validateFullMatchRegexInputStrings("[abc]*", Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("aaaa"),
                new LookaheadWord("abcbcbcbcc"), new LookaheadWord("d")
        ));

        TestUtils.validateFullMatchRegexInputStrings("de?|f[abc]?", Arrays.asList(
                new LookaheadWord("d"), new LookaheadWord("de"),
                new LookaheadWord("fa"), new LookaheadWord("f")
        ));
    }

    @Test
    public void testRepetition() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("[\\d]{2,}", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("11")
        ));

        TestUtils.validateFullMatchRegexInputStrings("a{2,}", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa"),
                new LookaheadWord("aaa"), new LookaheadWord("aaaaaaaaa")
        ));

        TestUtils.validateFullMatchRegexInputStrings("1{2,3}", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("11"),
                new LookaheadWord("111"), new LookaheadWord("1111"),
                new LookaheadWord("11111111111111")
        ));


        TestUtils.validateFullMatchRegexInputStrings(".{1,3}", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("12"),
                new LookaheadWord("123"), new LookaheadWord("1234"),
                new LookaheadWord(""), new LookaheadWord(" ")
        ));

        TestUtils.validateFullMatchRegexInputStrings(".{4}", Arrays.asList(
                new LookaheadWord("1"), new LookaheadWord("12"),
                new LookaheadWord("123"), new LookaheadWord("1234"),
                new LookaheadWord(""), new LookaheadWord("123456")
        ));
    }

    @Test
    public void testPositiveLookaheads() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("EB(?=AA)AAA", Arrays.asList(
                new LookaheadWord("EBAA"), new LookaheadWord("EBAAA")
        ));

        TestUtils.validateFullMatchRegexInputStrings("(?=aa)aab", Arrays.asList(
                new LookaheadWord("aab"), new LookaheadWord("aa"),
                new LookaheadWord("aaba")
        ));

        TestUtils.validateFullMatchRegexInputStrings("(a(?=b)|qw*x)kl", Arrays.asList(
                new LookaheadWord("qxkl"), new LookaheadWord("qwxkl"),
                new LookaheadWord("akl")
        ));

        TestUtils.validateFullMatchRegexInputStrings("((?=aa)a|aa)", Arrays.asList(
                new LookaheadWord("aa"), new LookaheadWord("aaa"),
                new LookaheadWord("aaaa"), new LookaheadWord("a")
//                new LookaheadWord("a", "a")
//                new LookaheadWord("aa", "a")
        ));

        TestUtils.validateFullMatchRegexInputStrings("((?=aa)a)a", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa")
        ));

        TestUtils.validateFullMatchRegexInputStrings("(?=aa)aa", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa")
        ));

        TestUtils.validateFullMatchRegexInputStrings("(a|(?=[^a]{3})aa)aa", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa"),
                new LookaheadWord("aaa")
        ));

        TestUtils.validateFullMatchRegexInputStrings("((?=aa)a|aa)b*c", Arrays.asList(
                new LookaheadWord("aac"), new LookaheadWord("aabc"),
                new LookaheadWord("aabbbc"), new LookaheadWord("ac"),
                new LookaheadWord("abc")
        ));

        TestUtils.validateFullMatchRegexInputStrings("a(?=d).", Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa"),
                new LookaheadWord("adb")
        ));

        TestUtils.validateFullMatchRegexInputStrings("a(?=c|d).", Arrays.asList(
                new LookaheadWord("ac"), new LookaheadWord("ad"),
                new LookaheadWord("ae"), new LookaheadWord("adac")
        ));

        StringBuilder sb = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("a");
            s1.append("aaaaa");
            s2.append("aaaaaaaaaaa");
        }
        TestUtils.validateFullMatchRegexInputStrings("(?=a)(" + sb.toString() + ")+", Arrays.asList(
                new LookaheadWord(s1.toString()), new LookaheadWord(s2.toString()),
                new LookaheadWord("a"), new LookaheadWord("")
        ));

        List<Pair<String, List<String>>> bulk = Arrays.asList(
                new Pair<>("abc(?=xyz)", Arrays.asList("abcxyz")),
                new Pair<>("abc(?=xyz)...", Arrays.asList("abcxyz", "abcxxz")),
                new Pair<>("abc(?=abcde)(?=ab)", Arrays.asList("abcabcdefg")),
                new Pair<>("ab(?=c)\\wd\\w\\w", Arrays.asList("abcdef", "abccef")),
                new Pair<>("((?=([^a]{2})d)\\\\w{3})\\\\w\\\\w", Arrays.asList("abcdef", "abccef")),
                new Pair<>("f[oa]+(?=o)", Arrays.asList("faao", "faaao")),
                new Pair<>("(?=.*[a-z])(?=.*[0-9])...", Arrays.asList("a3d", "333", "a3", "aqq")),
                new Pair<>("((?=a).)*", Arrays.asList("", "a", "aa", "aaa", "b")),
                new Pair<>("(?=.*a)(?=.*b)(?=.*c).*", Arrays.asList("abc", "anvbaixcz", "cba", "ab", "ac", "cbqq")),
                new Pair<>("((?=aa)a)*a", Arrays.asList("a", "aa", "aaa", "aaaa", "aaaab")),
                new Pair<>("(?=.*)(?=.*)(.{4}).*", Arrays.asList("1234", "12345", "123"))
        );

        for (Pair<String, List<String>> test : bulk) {
            String regex = test.first;
            for (String word : test.second) {
                TestUtils.validateFullMatchRegexInputStrings(regex, Arrays.asList(new LookaheadWord(word)));
            }
        }
    }

    @Test
    public void testNestedPositiveLookaheads() throws TimeoutException, AutomataException {
        TestUtils.validateFullMatchRegexInputStrings("(?=a(?=b))...", Arrays.asList(new LookaheadWord("abc")));
        TestUtils.validateFullMatchRegexInputStrings("(?=a(?=bc))...", Arrays.asList(new LookaheadWord("abc")));
        TestUtils.validateFullMatchRegexInputStrings("(?=a(?=b)b)...", Arrays.asList(new LookaheadWord("abc")));
        TestUtils.validateFullMatchRegexInputStrings("(?=a(?=b)c)...", Arrays.asList(new LookaheadWord("abc")));
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException, AutomataException {
        List<Pair<String, List<String>>> bulk = Arrays.asList(
                new Pair<>("(?!aa)..", Arrays.asList("ab", "ba", "bb", "aa")),
                new Pair<>("a(?!b).", Arrays.asList("ad", "ac", "ab")),
                new Pair<>("([ab]*)(?!b)c", Arrays.asList("abc")),
                new Pair<>("abc(?!d).", Arrays.asList("abce", "abcd")),
                new Pair<>("\\/\\*((?!\\/\\*).)*\\*\\/", Arrays.asList("/* */", "/   */", "/*  /* */ */",
                        "/* this is a comment */", "/* *", "/**/")),
                new Pair<>("((?!(ab)).)*", Arrays.asList("ab", "ba", "qw", "", "a", "b")),
                new Pair<>("((?!(ab)).)ab*", Arrays.asList("ab", "ba", "qw", "", "a", "b")),
                new Pair<>("(a|(?!ab))*b", Arrays.asList("a", "b", "ab", "bb", "bbb")),
                new Pair<>("(?=.*)(?=.*)(.{4}).*", Arrays.asList("1234", "12345", "123")),
                new Pair<>("a(?!b(?!c))..", Arrays.asList("ada", "abe"))
        );

        for (Pair<String, List<String>> test : bulk) {
            String regex = test.first;
            for (String word : test.second) {
                TestUtils.validateFullMatchRegexInputStrings(regex, Arrays.asList(new LookaheadWord(word)));
            }
        }
    }

    @Test
    public void testFullMatchAnchors() throws TimeoutException {
        String regex = "(?=(ab)*b)(?=(a|b)*bba*)a";
//        SAFA<CharPred, Character> safa = TestUtils.constructFullMatchFromRegex(regex);
//        System.out.println(safa.getDot("safa"));

//        regex = "((?=a*b$)a)*b$";
//        safa = TestUtils.constructFullMatchFromRegex(regex);
//        System.out.println(safa.getDot("safa"));

        regex = "((?=a*b$)ab*)*b$";
//        safa = TestUtils.constructFullMatchFromRegex(regex);
//        System.out.println(safa.getDot("safa"));
    }

}
