package benchmark.regexconverter;

import RegexParser.*;
import automata.safa.SAFA;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestRegexTranslation {

    @Test
    public void testFollow01() {
        String regex = "(a|aa)bd[0-9].*(m|k)\\d";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);

        assertEquals("(((((Char:b)(Char:d))([Char:0-Char:9 ]))((Dot)*))(((Char:m) | (Char:k))))(Meta:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex1())));
        assertEquals("(((((Char:b)(Char:d))([Char:0-Char:9 ]))((Dot)*))(((Char:m) | (Char:k))))(Meta:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex2())));
    }

    @Test
    public void testFollow02() {
        String regex = "((a|b)*b|b)c.d*";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        StarNode node4 = (StarNode) node3.getList().get(0);
        UnionNode node5 = (UnionNode) node4.getMyRegex1();

        assertEquals("((((((Char:a) | (Char:b)))(Char:b))(Char:c))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex1())));
        assertEquals("((((((Char:a) | (Char:b)))(Char:b))(Char:c))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex2())));
        assertEquals("((((((Char:a) | (Char:b)))(Char:b))(Char:c))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4.getMyRegex1())));
    }

    @Test
    public void testFollow03() {
        String regex = "((a|b)*b|b)c(a|b(a|a)bb((a|b)a)).d*";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);

        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        StarNode node4 = (StarNode) node3.getList().get(0);

        assertEquals("(((Char:c)(((Char:a) | (Char:b)(((Char:a) | (Char:a)))(Char:b)(Char:b)((((Char:a) | (Char:b)))(Char:a)))))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex2())));
        assertEquals("(((Char:c)(((Char:a) | (Char:b)(((Char:a) | (Char:a)))(Char:b)(Char:b)((((Char:a) | (Char:b)))(Char:a)))))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node3.getList().get(1))));
        assertEquals("(((((((Char:a) | (Char:b)))(Char:b))(Char:c))(((Char:a) | (Char:b)(((Char:a) | (Char:a)))(Char:b)(Char:b)((((Char:a) | (Char:b)))(Char:a)))))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4)));
        assertEquals("(((((((Char:a) | (Char:b)))(Char:b))(Char:c))(((Char:a) | (Char:b)(((Char:a) | (Char:a)))(Char:b)(Char:b)((((Char:a) | (Char:b)))(Char:a)))))(Dot))((Char:d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4.getMyRegex1())));
    }

    @Test
    public void testFollow04() {
        String regex = "(((a(ab|c)k.\\d(aa|bb)|k))|c)end";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        ConcatenationNode node4 = (ConcatenationNode) node3.getList().get(0);
        UnionNode node5 = (UnionNode) node4.getList().get(0);
        ConcatenationNode node6 = (ConcatenationNode) node5.getMyRegex1();
        UnionNode node7 = (UnionNode) node6.getList().get(1);
        ConcatenationNode node8 = (ConcatenationNode) node7.getMyRegex1();

        assertEquals("((((((Char:k)(Dot))(Meta:d))(((Char:a)(Char:a) | (Char:b)(Char:b))))(Char:e))(Char:n))(Char:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node7.getMyRegex2())));
        assertEquals("((((((Char:k)(Dot))(Meta:d))(((Char:a)(Char:a) | (Char:b)(Char:b))))(Char:e))(Char:n))(Char:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node8.getList().get(0))));
        assertEquals("((((((Char:k)(Dot))(Meta:d))(((Char:a)(Char:a) | (Char:b)(Char:b))))(Char:e))(Char:n))(Char:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node8.getList().get(1))));
    }

    @Test
    public void testFollow05() {
        String regex = "(?>a*(b*a*)(a*b*))";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        AtomicGroupNode node2 = (AtomicGroupNode ) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();

        assertEquals("((Char:a)(((Char:b)*)((Char:a)*)))(((Char:a)*)((Char:b)*))" +
                        "",
                RegexTranslator.printNode(RegexTranslator.follow(node3.getList().get(0))));
    }

    @Test
    public void testFollow07() {
        String regex = "(?>a*(b*c*)(d*e*))";
        RegexNode root = Utils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        AtomicGroupNode node2 = (AtomicGroupNode ) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();

        ConcatenationNode node4 = (ConcatenationNode) node3.getList().get(2);
        StarNode node5 = (StarNode) node4.getList().get(1);

        assertEquals("Char:e",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex1())));
        assertEquals("Char:e",
                RegexTranslator.printNode(RegexTranslator.follow(node5)));
    }

    @Test
    public void testRegexTranslation01() throws TimeoutException {
        String regex = "a|aa";

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));
        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("", "a", "aa");
        Utils.validateFullMatchRegexInputStrings(safa, "a|(?!a)aa", strings);
    }

    @Test
    public void testRegexTranslation02() throws TimeoutException {
        String regex = "aa|a";

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));
        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("", "a", "aa");
        Utils.validateFullMatchRegexInputStrings(safa, "aa|(?!aa)a", strings);
    }

    @Test
    public void testRegexTranslation03() throws TimeoutException {
        String regex = "a*(ab)*b";
        String java = String.format("(?>%s)", regex);

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));

        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("b", "ab", "aab", "abb");
        Utils.validateFullMatchRegexInputStrings(safa, java, strings);
    }

    @Test
    public void testRegexTranslation04() throws TimeoutException {
        String regex = "a*(b|abb)";
        String java = String.format("(?>%s)", regex);

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));

        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("", "b", "ab", "aab", "abb");
        Utils.validateFullMatchRegexInputStrings(safa, java, strings);
    }

    @Test
    public void testRegexTranslation06() throws TimeoutException {
        String regex = "a*(abb|b)";
        String java = String.format("(?>%s)", regex);

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));
        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("", "b", "ab", "aab", "abb");
        Utils.validateFullMatchRegexInputStrings(safa, java, strings);
    }

    @Test
    public void testRegexTranslation07() throws TimeoutException {
        String regex = "abb|a*(ab)*b";
        String java = String.format("(?>%s)", regex);

        RegexNode translated = RegexTranslator.translate(Utils.parseRegex(regex));
        SAFA<CharPred, Character> safa = Utils.constructFromNode(translated);

        List<String> strings = Arrays.asList("", "b", "ab", "aab", "abb");
        Utils.validateFullMatchRegexInputStrings(safa, java, strings);
    }

    @Test
    public void testAtomicOperator01() throws TimeoutException {
        String regex = "a(?>bc|b)c";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("abc");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator02() throws TimeoutException {
        String regex = "(?>a*)";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("a", "aa");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator03() throws TimeoutException {
        String regex = "(?>\\s*(\\S\\S*\\s*))(\\s\\s*\\S\\S*)";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("", "  aaa  aaa");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator04() throws TimeoutException {
        String regex = "(?>\\s*(\\S\\S*\\s*))(\\s\\s*\\S\\S*)";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("", "  aaa  aaa");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator05() throws TimeoutException {
        String regex = "(?>n|n1)@gmail\\.com";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("n@gmail.com", "n1@gmail.com");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator06() throws TimeoutException {
        String regex = "a(?>bc|b)c";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("abc");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator07() throws TimeoutException {
        String regex = "(?>\\W\\w*):";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("abc:", "", "a:", "2:", "1");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }
    @Test
    public void testAtomicOperator08() throws TimeoutException {
        String regex = "(?>(\"|[^\"])*)";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("aa\"", "", "a:", "\"\"\"", "2:");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }
    @Test
    public void testAtomicOperator09() throws TimeoutException {
        String regex = "(\\.\\d\\d(?>[1-9]?))\\d+";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList(".625", ".625000");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }
    @Test
    public void testAtomicOperator10() throws TimeoutException {
        /* TODO fix, the optional operator seems to be breaking it! */
//        String regex = "[a-z0-9]+(?>_[a-z0-9]+)?";
//        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);
//
//        List<String> strings = Arrays.asList("", "azaaz", "a10ask_a");
//        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }
    @Test
    public void testAtomicOperator11() throws TimeoutException {
        String regex = "(?>a*(b*a*)(a*b*))";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("abab");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator12() throws TimeoutException {
        String regex = "(?>((a|b)*)*)b";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("", "ab", "aab", "b");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator13() throws TimeoutException {
        String regex = "(?>(a|b)*)*b";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("", "ab", "aab", "b");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

    @Test
    public void testAtomicOperator14() throws TimeoutException {
        String regex = "(?>(\\.|[^\"])*)";
        SAFA<CharPred, Character> safa = Utils.constructFromRegex(regex);

        List<String> strings = Arrays.asList("a", "b", "aababab", "\"", "aababa\"");
        Utils.validateFullMatchRegexInputStrings(safa, regex, strings);
    }

}
