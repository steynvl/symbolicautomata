package benchmark.regexconverter;

import RegexParser.*;
import automata.safa.SAFA;
import benchmark.SAFAProvider;
import org.junit.Test;
import theory.characters.CharPred;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestRegexTranslation {

    @Test
    public void testFollow01() throws RegexTranslationException {
        String regex = "(a|aa)bd[0-9].*(m|k)\\d";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);

        assertEquals("(((((b)(d))([0-9 ]))((Dot)*))(((m) | (k))))(Meta:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex1())));
        assertEquals("(((((b)(d))([0-9 ]))((Dot)*))(((m) | (k))))(Meta:d)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex2())));
    }

    @Test
    public void testFollow02() throws RegexTranslationException {
        String regex = "((a|b)*b|b)c.d*";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        StarNode node4 = (StarNode) node3.getList().get(0);
        UnionNode node5 = (UnionNode) node4.getMyRegex1();

        assertEquals("((((((a) | (b)))(b))(c))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex1())));
        assertEquals("((((((a) | (b)))(b))(c))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex2())));
        assertEquals("((((((a) | (b)))(b))(c))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4.getMyRegex1())));
    }

    @Test
    public void testFollow03() throws RegexTranslationException {
        String regex = "((a|b)*b|b)c(a|b(a|a)bb((a|b)a)).d*";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);

        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        StarNode node4 = (StarNode) node3.getList().get(0);

        assertEquals("(((c)(((a) | (b)(((a) | (a)))(b)(b)((((a) | (b)))(a)))))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node2.getMyRegex2())));
        assertEquals("(((c)(((a) | (b)(((a) | (a)))(b)(b)((((a) | (b)))(a)))))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node3.getList().get(1))));
        assertEquals("(((((((a) | (b)))(b))(c))(((a) | (b)(((a) | (a)))(b)(b)((((a) | (b)))(a)))))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4)));
        assertEquals("(((((((a) | (b)))(b))(c))(((a) | (b)(((a) | (a)))(b)(b)((((a) | (b)))(a)))))(Dot))((d)*)",
                RegexTranslator.printNode(RegexTranslator.follow(node4.getMyRegex1())));
    }

    @Test
    public void testFollow04() throws RegexTranslationException {
        String regex = "(((a(ab|c)k.\\d(aa|bb)|k))|c)end";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        UnionNode node2 = (UnionNode) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();
        ConcatenationNode node4 = (ConcatenationNode) node3.getList().get(0);
        UnionNode node5 = (UnionNode) node4.getList().get(0);
        ConcatenationNode node6 = (ConcatenationNode) node5.getMyRegex1();
        UnionNode node7 = (UnionNode) node6.getList().get(1);
        ConcatenationNode node8 = (ConcatenationNode) node7.getMyRegex1();

        assertEquals("((((((k)(Dot))(Meta:d))(((a)(a) | (b)(b))))(e))(n))(d)",
                RegexTranslator.printNode(RegexTranslator.follow(node7.getMyRegex2())));
        assertEquals("((((((k)(Dot))(Meta:d))(((a)(a) | (b)(b))))(e))(n))(d)",
                RegexTranslator.printNode(RegexTranslator.follow(node8.getList().get(0))));
        assertEquals("((((((k)(Dot))(Meta:d))(((a)(a) | (b)(b))))(e))(n))(d)",
                RegexTranslator.printNode(RegexTranslator.follow(node8.getList().get(1))));
    }

    @Test
    public void testFollow05() throws RegexTranslationException {
        String regex = "(?>a*(b*a*)(a*b*))";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        AtomicGroupNode node2 = (AtomicGroupNode ) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();

        assertEquals("((a)(((b)*)((a)*)))(((a)*)((b)*))",
                RegexTranslator.printNode(RegexTranslator.follow(node3.getList().get(0))));
    }

    @Test
    public void testFollow07() throws RegexTranslationException {
        String regex = "(?>a*(b*c*)(d*e*))";
        RegexNode root = TestUtils.parseRegex(regex);
        RegexTranslator.setParents(root);

        ConcatenationNode node1 = (ConcatenationNode) root;
        AtomicGroupNode node2 = (AtomicGroupNode ) node1.getList().get(0);
        ConcatenationNode node3 = (ConcatenationNode) node2.getMyRegex1();

        ConcatenationNode node4 = (ConcatenationNode) node3.getList().get(2);
        StarNode node5 = (StarNode) node4.getList().get(1);

        assertEquals("e",
                RegexTranslator.printNode(RegexTranslator.follow(node5.getMyRegex1())));
        assertEquals("e",
                RegexTranslator.printNode(RegexTranslator.follow(node5)));
    }

    @Test
    public void testRegexTranslation01() throws Exception {
        String regex = "a|aa";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        StringBuilder sb = new StringBuilder();
        translated.toRaw(sb);
        assertEquals("((a)|((?!((a))))((a)(a)))", sb.toString());

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("a"),
                new LookaheadWord("aa"), new LookaheadWord("a", "a")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testRegexTranslation02() throws Exception {
        String regex = "aa|a";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        StringBuilder sb = new StringBuilder();
        translated.toRaw(sb);
        assertEquals("((a)(a)|((?!((a)(a))))((a)))", sb.toString());

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("a"),
                new LookaheadWord("aa"), new LookaheadWord("a", "a")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testRegexTranslation03() throws Exception {
        String regex = "a*(ab)*b";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord("b"), new LookaheadWord("ab"),
                new LookaheadWord("aab"), new LookaheadWord("abb"),
                new LookaheadWord("aab", "b"), new LookaheadWord("aa", "bb")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testRegexTranslation04() throws Exception {
        String regex = "a*(b|abb)";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("b"),
                new LookaheadWord("ab"), new LookaheadWord("aab"),
                new LookaheadWord("aa", "bb"), new LookaheadWord("a", "bb"),
                new LookaheadWord("ab", "b")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testRegexTranslation06() throws Exception {
        String regex = "a*(abb|b)";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("b"),
                new LookaheadWord("ab"), new LookaheadWord("aab"),
                new LookaheadWord("aa", "bb"), new LookaheadWord("a", "bb"),
                new LookaheadWord("ab", "b")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testRegexTranslation07() throws Exception {
        String regex = "abb|a*(ab)*b";

        RegexNode translated = RegexTranslator.translate(TestUtils.parseRegex(regex));

        SAFAProvider safaProvider = SAFAProvider.fromRegexNode(translated);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("b"),
                new LookaheadWord("ab"), new LookaheadWord("aab"),
                new LookaheadWord("aa", "bb"), new LookaheadWord("a", "bb"),
                new LookaheadWord("ab", "b")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testAtomicOperator01() throws Exception {
        String regex = "a*(?>bc|b)c";

        RegexNode rewla = RegexTranslator.removeAtomicOperators(regex);

        StringBuilder sb = new StringBuilder();
        rewla.toRaw(sb);
        assertEquals("((a)*)(((b)(c)|((?!((b)(c))))((b))))(c)", sb.toString());

        SAFAProvider safaProvider = new SAFAProvider(rewla);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(new LookaheadWord("abc"));

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testAtomicOperator02() throws Exception {
        String regex = "(?>a*)";

        RegexNode rewla = RegexTranslator.removeAtomicOperators(regex);

        SAFAProvider safaProvider = new SAFAProvider(rewla);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord("a"), new LookaheadWord("aa")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testAtomicOperator03() throws Exception {
        String regex = "(?>\\s*(\\S\\S*\\s*))(\\s\\s*\\S\\S*)";

        RegexNode rewla = RegexTranslator.removeAtomicOperators(regex);

        SAFAProvider safaProvider = new SAFAProvider(rewla);
        SAFA<CharPred, Character> safa = safaProvider.getSAFA();

        List<LookaheadWord> strings = Arrays.asList(
                new LookaheadWord(""), new LookaheadWord("  aaa   aaa")
        );

        TestUtils.validateSubMatch(regex, safa, safaProvider.getSolver(), strings);
    }

    @Test
    public void testAtomicOperator16() throws Exception {
        List<Pair<String, List<String>>> tests = Arrays.asList(
                new Pair<>("(a(b))_1(?>33|3)37", Arrays.asList("ab_1337", "ab_13337")),
                new Pair<>("\\s*(?>\\/\\/.*\\n)*", Arrays.asList("   //\n", "     //")),
                new Pair<>("<\\?(em|i)((?> +)[^>]*)?>", Arrays.asList("<?em>", "<?em   >", "<?em   >>")),
                new Pair<>("(?>33|3)(?:3)(?>33|3)3", Arrays.asList("33333", "333333", "3333", "3333333")),
                new Pair<>("<td>(?>(.+)<\\/td>)", Arrays.asList("<td>a</td>", "<td></td>", "<td></td></td>")),
                new Pair<>("(?>a?)a", Arrays.asList("", "a", "aa")),
                new Pair<>("(?>a*a)", Arrays.asList("", "a", "aa", "aaa", "aaaa", "baaa")),
                new Pair<>("(?>(\\.|[^\"])*)", Arrays.asList("a", "b", "aababab", "\"", "aababa\"")),
                new Pair<>("(?>a*(b*a*)(a*b*))", Arrays.asList("abab")),
                new Pair<>("[a-z0-9]+(?>_[a-z0-9]+)?", Arrays.asList("", "azaaz", "a10ask_a")),
                new Pair<>("(\\.\\d\\d(?>[1-9]?))\\d+", Arrays.asList(".625", ".625000")),
                new Pair<>("(?>(\"|[^\"])*)", Arrays.asList("aa\"", "", "a:", "\"\"\"", "2:")),
                new Pair<>("(?>\\W\\w*):", Arrays.asList("abc:", "", "a:", "2:", "1")),
                new Pair<>("a(?>bc|b)c", Arrays.asList("abc")),
                new Pair<>("(?>n|n1)@gmail\\.com", Arrays.asList("n@gmail.com", "n1@gmail.com"))
        );

        for (Pair<String, List<String>> test : tests) {
            String aREwLA = test.first;
            RegexNode REwLA = RegexTranslator.removeAtomicOperators(aREwLA);

            SAFAProvider safaProvider = new SAFAProvider(REwLA);
            SAFA<CharPred, Character> safa = safaProvider.getSAFA();

            List<LookaheadWord> strings = test.second.stream().map(LookaheadWord::new).collect(Collectors.toList());

            TestUtils.validateSubMatch(aREwLA, safa, safaProvider.getSolver(), strings);
        }
    }

    @Test
    public void testNestedAtomicOperator() {
        List<String> atomicRegexes = Arrays.asList(
                "(?>[a-zA-Z_]\\w*(?>[?!])?)(:)(?!:)",
                "(?>33|3(?>(3)))",
                "(?>(?>c))"
        );

        for (String regex : atomicRegexes) {
            try {
                RegexTranslator.removeAtomicOperators(regex);
            } catch (RegexTranslationException e) {
                String msg = "Nested atomic groups not supported.";
                assertEquals(msg, e.getMessage());
                continue;
            }

            fail();
        }
    }

}
