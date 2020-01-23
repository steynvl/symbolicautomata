package benchmark.regexconverter;

import org.junit.Test;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;

public class TestSAFAConstruction {

    @Test
    public void testConcatAndUnion() throws TimeoutException {
        String regex = "abc|de";
        Utils.validateRegexConstruction(regex, 11, 10, 2, 0);
        Utils.validateRegexInputString(regex, Arrays.asList("abc", "de"), Arrays.asList("ab"));
    }

    @Test
    public void testMetaChar() throws TimeoutException {
        Utils.validateRegexConstruction("\\s", 2, 1, 1, 0);
        Utils.validateRegexInputString("\\s", Arrays.asList(" "), Arrays.asList("a"));

        Utils.validateRegexConstruction("\\S", 2, 1, 1, 0);
        Utils.validateRegexInputString("\\S", Arrays.asList("a"), Arrays.asList(" "));

        Utils.validateRegexConstruction("\\w", 2, 1, 1, 0);
        Utils.validateRegexInputString("\\w", Arrays.asList("a", "_", "1"), Arrays.asList(" "));
    }

    @Test
    public void testCharacterClass() throws TimeoutException {
        Utils.validateRegexConstruction("[\\d]", 2, 1, 1, 0);
        Utils.validateRegexInputString("[\\d]", Arrays.asList("1"), Arrays.asList("10"));

        Utils.validateRegexConstruction("[\\\\a123bc!@#$s%^&*(){}]",
                2, 1, 1, 0);
        Utils.validateRegexInputString("[\\\\a123bc!@#$s%^&*(){}]",
                Arrays.asList("1", "a", "^", "s"), Arrays.asList("10"));

        Utils.validateRegexConstruction("[a-zA-Z1-9]", 2, 1, 1, 0);
        Utils.validateRegexInputString("[a-zA-Z1-9]", Arrays.asList("8", "b", "c", "C"), Arrays.asList("10"));

        Utils.validateRegexConstruction("\\a|[b-zA-Z1-9]", 5, 4, 2, 0);
        Utils.validateRegexInputString("\\a|[b-zA-Z1-9]", Arrays.asList(), Arrays.asList("\\a"));
    }

    @Test
    public void testQuantifiers() throws TimeoutException {
        Utils.validateRegexConstruction("[\\d]+", 5, 5, 1, 0);
        Utils.validateRegexInputString("[\\d]+", Arrays.asList("1234567", "111131", "111123459"), Arrays.asList("a"));

        Utils.validateRegexConstruction("a*", 3, 3, 1, 0);
        Utils.validateRegexInputString("a*", Arrays.asList("", "a", "aaaa"), Arrays.asList("b"));

        Utils.validateRegexConstruction("[abc]*", 3, 3, 1, 0);
        Utils.validateRegexInputString("[abc]*", Arrays.asList("", "aaaa", "abcbcbcbcc"), Arrays.asList("d"));

        Utils.validateRegexConstruction("de?|f[abc]?", 13, 12, 4, 0);
        Utils.validateRegexInputString("de?|f[abc]?", Arrays.asList("d", "de", "fa", "f"), Arrays.asList("def"));
    }

    @Test
    public void testPositiveLookaheads() throws TimeoutException {
        Utils.validateRegexConstruction("EB(?=AA)AAA", 19, 18, 1, 1);
        Utils.validateRegexInputString("EB(?=AA)AAA", Arrays.asList("EBAAA"), Arrays.asList("EBAA"));

        Utils.validateRegexInputString("(?=aa)aab", Arrays.asList("aab"), Arrays.asList("aa", "aaba"));
        Utils.validateRegexInputString("(a(?=b)|qw*x)kl", Arrays.asList("qxkl", "qwxkl"), Arrays.asList("akl"));

        Utils.validateRegexInputString("((?=aa)a|aa)", Arrays.asList("aa"), Arrays.asList("aaa", "aaaa"));
        Utils.validateRegexInputString("((?=aa)a)a", Arrays.asList("aa"), Arrays.asList("aaa", "aaaa"));
        Utils.validateRegexInputString("(?=aa)aa", Arrays.asList("aa"), Arrays.asList("a", "aaa"));
        Utils.validateRegexInputString("(a|(?=[^a]{3})aa)aa", Arrays.asList("aaa"), Arrays.asList("aaaa"));
        Utils.validateRegexInputString("((?=aa)a|aa)b*c",
                Arrays.asList("aac", "aabc", "aabbbc"), Arrays.asList("ac", "abc"));

        Utils.validateRegexInputString("a(?=d).", Arrays.asList("ad"), Arrays.asList("a", "aa", "adb"));
        Utils.validateRegexInputString("a(?=c|d).", Arrays.asList("ac", "ad"), Arrays.asList("ae", "adac"));

        StringBuilder sb = new StringBuilder();
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("a");
            s1.append("aaaaa");
            s2.append("aaaaaaaaaaa");
        }
        Utils.validateRegexInputString("(?=a)(" + sb.toString() + ")+",
                Arrays.asList(s1.toString(), s2.toString()), Arrays.asList("a"));

        Utils.validateRegexInputString("abc(?=xyz)", Arrays.asList(), Arrays.asList("abcxyz"));
        Utils.validateRegexInputString("abc(?=xyz)...", Arrays.asList("abcxyz"), Arrays.asList("abcxxz"));

        Utils.validateRegexInputString("abc(?=abcde)(?=ab)", Arrays.asList(), Arrays.asList("abcabcdefg"));
        Utils.validateRegexInputString("ab(?=c)\\wd\\w\\w", Arrays.asList("abcdef"), Arrays.asList());
        Utils.validateRegexInputString("((?=([^a]{2})d)\\w{3})\\w\\w", Arrays.asList(),
                Arrays.asList("abcdef", "abccef"));

        Utils.validateRegexInputString("f[oa]+(?=o)", Arrays.asList(), Arrays.asList("faaao"));
        Utils.validateRegexInputString("(?=.*[a-z])(?=.*[0-9])...",
                Arrays.asList("a3d"), Arrays.asList("333", "a3", "aqq"));
        Utils.validateRegexInputString("((?=a).)*", Arrays.asList("", "a", "aa", "aaa"), Arrays.asList("b"));
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException {
        Utils.validateRegexInputString("(?!aa)..", Arrays.asList("ab", "ba", "bb"), Arrays.asList("aa"));
    }
}
