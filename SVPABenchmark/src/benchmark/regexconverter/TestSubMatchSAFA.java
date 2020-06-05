package benchmark.regexconverter;

import automata.AutomataException;
import benchmark.SAFAProvider;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSubMatchSAFA {

    @Test
    public void testSubMatchingSAFA01() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("a|aa", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa")
                )),
                new Pair<>("aa|a", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa")
                )),
                new Pair<>("a*(b|abb)", Arrays.asList(
                        new LookaheadWord("ab"), new LookaheadWord("b"),
                        new LookaheadWord("abb"), new LookaheadWord("aabb")
                )),
                new Pair<>("a*(abb|b)", Arrays.asList(
                        new LookaheadWord("ab"), new LookaheadWord("b"),
                        new LookaheadWord("abb"), new LookaheadWord("aabb")
                )),
                new Pair<>("a*b|abb", Arrays.asList(
                        new LookaheadWord("ab"), new LookaheadWord("abb")
                )),
                new Pair<>("a|ab", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab")
                )),
                new Pair<>("a*(ab)*b", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("abb"), new LookaheadWord("aaab")
                )),
                new Pair<>("abb|a*(ab)*b", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("abb"), new LookaheadWord("aaab")
                )),
                new Pair<>("a|ab", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab")
                ))
        );

        validateTests(tests);
    }

    @Test
    public void testSubMatchingSAFA02() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("((a*)*b)*b", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("abb"), new LookaheadWord("aabb"),
                        new LookaheadWord("ababb"))
                ),
                new Pair<>("(b|(a|b)*bb)", Arrays.asList(
                        new LookaheadWord("b"), new LookaheadWord("abb"),
                        new LookaheadWord("bbb"), new LookaheadWord("aabbb"))
                ),
                new Pair<>("ba((?!ab))*", Arrays.asList(
                        new LookaheadWord("baab"), new LookaheadWord("baaab"),
                        new LookaheadWord("baqweab"))
                )
        );

        validateTests(tests);
    }

    @Test
    public void testLookaheads01() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("(?=aa)a", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa"),
                        new LookaheadWord("aaa"), new LookaheadWord("aaaa"),
                        new LookaheadWord("aaaaa"))
                ),
                new Pair<>("(?=aaa)aa", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa"),
                        new LookaheadWord("aaa"), new LookaheadWord("aaaa"),
                        new LookaheadWord("aaaaa"))
                ),
                new Pair<>("EB(?=AA)AAA", Arrays.asList(
                        new LookaheadWord("EBAA"),
                        new LookaheadWord("EBAAA"))
                ),
                new Pair<>("EB(?=AAAA)AAA", Arrays.asList(
                        new LookaheadWord("EBAA"), new LookaheadWord("EBAAA"),
                        new LookaheadWord("EBAAAA"), new LookaheadWord("EBAAAAA"),
                        new LookaheadWord("EBAAAB"))
                ),
                new Pair<>("(a(?=kl+)|qw*x)kl", Arrays.asList(
                        new LookaheadWord("akl"), new LookaheadWord("qxkl"),
                        new LookaheadWord("qwxkl"), new LookaheadWord("qwxkl"),
                        new LookaheadWord("akll"), new LookaheadWord("akllq"))
                ),
                new Pair<>("(?=aa)aa", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("a"),
                        new LookaheadWord("aa"), new LookaheadWord("aaa"),
                        new LookaheadWord("aaaa"))
                )
        );

        validateTests(tests);
    }

    @Test
    public void testLookaheads02() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("((?=aa)a|aa)", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("a"),
                        new LookaheadWord("aa"), new LookaheadWord("aaa"))
                ),
                new Pair<>("((?=aa)a)a", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("a"),
                        new LookaheadWord("aa"), new LookaheadWord("aaa"))
                ),
                new Pair<>("(a|(?=[^a]{3})aa)aa", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("a"),
                        new LookaheadWord("aa"), new LookaheadWord("aaa"),
                        new LookaheadWord("bve"), new LookaheadWord("bbqaa"))
                ),
                new Pair<>("((?=aa)a|aa)b*c", Arrays.asList(
                        new LookaheadWord("aac"), new LookaheadWord("aabc"),
                        new LookaheadWord("ac"), new LookaheadWord("abc"))
                ),
                new Pair<>("a(?=c|d).", Arrays.asList(
                        new LookaheadWord("ac"), new LookaheadWord("ad"),
                        new LookaheadWord("aq"), new LookaheadWord("acd"),
                        new LookaheadWord("ade"), new LookaheadWord("aqqc"))
                ),
                new Pair<>("abc(?=abcde)(?=ab)", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("abcabcdeab"),
                        new LookaheadWord("abcabcde"), new LookaheadWord("abcab"))
                ),
                new Pair<>("f[oa]*(?=o)", Arrays.asList(
                        new LookaheadWord("fo"), new LookaheadWord("fa"),
                        new LookaheadWord("faaaaao"), new LookaheadWord("foaoaoaaaao"))
                ),
                new Pair<>("(?=.*[a-z])(?=.*[0-9])", Arrays.asList(
                        new LookaheadWord(""), new LookaheadWord("aa"),
                        new LookaheadWord("31"), new LookaheadWord("a3"),
                        new LookaheadWord("ab3"), new LookaheadWord("a3b"))
                ),
                new Pair<>("((?=a).)*", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa"),
                        new LookaheadWord("aaa"), new LookaheadWord("aaab"))
                ),
                new Pair<>("((?=aa)a)*a", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("aa"),
                        new LookaheadWord("aaa"), new LookaheadWord("aaaa"))
                )
        );

        /* TODO check translation of plus is what I think makes this break */
        // new Pair<>("f[oa]*(?=o)", Arrays.asList("fo", "fa", "faaaaao", "foaoaoaaaao")),

        validateTests(tests);
    }

    @Test
    public void testNestedPositiveLookaheads() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("(?=a(?=b))a", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("abb")))
        );

         validateTests(tests);
    }

    @Test
    public void testNegativeLookaheads() throws TimeoutException, AutomataException {
        List<Pair<String, List<LookaheadWord>>> tests = Arrays.asList(
                new Pair<>("(?!aa).", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("ad"))
                ),
                new Pair<>("a(?!b)", Arrays.asList(
                        new LookaheadWord("a"), new LookaheadWord("ab"),
                        new LookaheadWord("aa"), new LookaheadWord("abc"),
                        new LookaheadWord("aab"))
                ),
                new Pair<>("([ab]*)(?!b)c", Arrays.asList(
                        new LookaheadWord("ac"), new LookaheadWord("abc"),
                        new LookaheadWord("aaaac"), new LookaheadWord("abbbb"))
                ),
                new Pair<>("abc(?!d)", Arrays.asList(
                        new LookaheadWord("abcd"), new LookaheadWord("abcc"),
                        new LookaheadWord("abc"))
                ),
                new Pair<>("\\/\\*((?!\\/\\*).)*\\*\\/", Arrays.asList(
                        new LookaheadWord("/* */"), new LookaheadWord("/   */"),
                        new LookaheadWord("/*  /* */ */"),
                        new LookaheadWord("/* this is a comment */"),
                        new LookaheadWord("/* *", "/**/"))
                ),
                new Pair<>("((?!(ab)).)*", Arrays.asList(
                        new LookaheadWord("ab"), new LookaheadWord("ba"),
                        new LookaheadWord("qw"), new LookaheadWord(""),
                        new LookaheadWord("a"), new LookaheadWord("b"))
                ),
                new Pair<>("((?!(ab)).)*ab", Arrays.asList(
                        new LookaheadWord("ab"), new LookaheadWord("ba"),
                        new LookaheadWord("qw"), new LookaheadWord(""),
                        new LookaheadWord("a"), new LookaheadWord("b"))
                ),
                new Pair<>("(?=.*)(?=.*)(.{4}).*", Arrays.asList(
                        new LookaheadWord("1234"), new LookaheadWord("12345"),
                        new LookaheadWord("123"), new LookaheadWord("1"),
                        new LookaheadWord(""))
                )
        );

        validateTests(tests);
    }

    @Test
    public void testNestedNegativeLookaheads() throws TimeoutException {
        /* TODO have to handle nested positive lookaheads first */

        assertTrue(true);
    }

    private void validateTests(List<Pair<String, List<LookaheadWord>>> tests)
            throws TimeoutException, AutomataException {
        for (Pair<String, List<LookaheadWord>> test : tests) {
            String regex = test.first;
            SAFAProvider safaProvider = new SAFAProvider(regex);

            TestUtils.validateSubMatch(regex, safaProvider.getSAFA(), safaProvider.getSolver(), test.second);
        }
    }

}
