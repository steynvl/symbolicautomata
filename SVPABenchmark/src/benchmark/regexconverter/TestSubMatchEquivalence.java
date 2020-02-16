package benchmark.regexconverter;

import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import org.junit.Test;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestSubMatchEquivalence {

    private final BooleanExpressionFactory<PositiveBooleanExpression> boolExpr = SAFA.getBooleanExpressionFactory();

    private final UnaryCharIntervalSolver unarySolver = new UnaryCharIntervalSolver();

    @Test
    public void testSubMatchEquivalence01() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("a|aa", "aa|a");
        char delimiter = rsm.getSolver().getDelimiter();

        assertFalse(rsm.isEquivalent());
        testEquivalence(rsm, false, Arrays.asList(
                Utils.lOfS(String.format("a%ca", delimiter)), Utils.lOfS(String.format("aa%c", delimiter))
        ));
    }

    @Test
    public void testSubMatchEquivalence02() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("a*(b|abb)", "a*(abb|b)");

        testEquivalence(rsm, true, Arrays.asList());
    }

    @Test
    public void testSubMatchEquivalence03() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("a*(ab)*b", "abb|a*(ab)*b");

        testEquivalence(rsm, false, Arrays.asList());
    }

    @Test
    public void testSubMatchEquivalence04() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("b|(a|b)*b", "bb|(a|b)*b");

//        testEquivalence(rsm, false, Arrays.asList());
    }

    @Test
    public void testSubMatchEquivalence05() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("((?=aa)a|aa)", "((?=aa)aa)");
        char delimiter = rsm.getSolver().getDelimiter();

        testEquivalence(rsm, false, Arrays.asList(
                Utils.lOfS(String.format("a%ca", delimiter))
        ));
    }

    @Test
    public void testSubMatchEquivalence06() throws TimeoutException {
        RegexSubMatching rsm = new RegexSubMatching("((?=aaa)a|aa)", "((?=aa)aa)");
        char delimiter = rsm.getSolver().getDelimiter();

        testEquivalence(rsm, false, Arrays.asList(
                Utils.lOfS(String.format("a%caa", delimiter))
        ));
    }

    private void testEquivalence(RegexSubMatching rsm,
                                 boolean equivalent,
                                 List<List<Character>> witnesses) throws TimeoutException {
        /* TODO still need to modify algorithm for checking equivalence for SAFA with eps transitions */
        // testEquivalence(rsm, equivalent, witnesses, false);

        testEquivalence(rsm, equivalent, witnesses, true);
    }

    private void testEquivalence(RegexSubMatching rsm,
                                 boolean equivalent,
                                 List<List<Character>> witnesses,
                                 boolean removeEpsilons) throws TimeoutException {
        // assert !equivalent && witnesses.size() > 0 || equivalent && witnesses.isEmpty();

        SAFA<CharPred, Character> safa1 = removeEpsilons ? SAFA.removeEpsilonMovesFrom(rsm.getSAFA01(), rsm.getSolver()) : rsm.getSAFA01();
        SAFA<CharPred, Character> safa2 = removeEpsilons ? SAFA.removeEpsilonMovesFrom(rsm.getSAFA02(), rsm.getSolver()) : rsm.getSAFA02();

        Pair<Boolean, List<Character>> equiv = SAFA.isEquivalent(safa1, safa2, unarySolver, boolExpr);
        assertEquals(equivalent, equiv.first);

        if (!equivalent && removeEpsilons) {
            assertTrue(witnesses.contains(equiv.second));
        }
    }

}
