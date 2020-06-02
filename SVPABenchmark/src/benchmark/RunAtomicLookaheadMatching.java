package benchmark;

import benchmark.regexconverter.AtomicLookaheadMatching;
import utilities.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RunAtomicLookaheadMatching {

    public static void main(String[] args) {
        List<String> permutations = new LinkedList<>();
        for (int i = 1; i <= 9; i++) {
            List<String> curr = new LinkedList<>();
            permutation(new char[i], 0, "abc", curr);
            permutations.addAll(curr);
        }

        System.out.println("Number of test strings = " + permutations.size());

        List<String> alphabet = Arrays.asList("a", "b", "c", "(", ")", "(?=", "(?>", "*", "|");

        Pair<Integer, List<String>> tests = generateTests(alphabet, 8);

//        List<String> regexes = tests.second;
//        for (String regex : regexes) {
//            for (String w : permutations) {
//                System.out.printf("m(%s, %s)\n", regex, w);
//            }
//        }

        System.out.println("number of valid generated regexes = " + tests.second.size());
    }

    private static Pair<Integer, List<String>> generateTests(List<String> alphabet, int n) {
        int m = alphabet.size();
        int numOfStrings = (int) Math.pow(m, n);

        List<String> regexes = new LinkedList<>();

        int max = -1;
        int min = 20;
        for (int i = 0; i < numOfStrings; i++) {
            StringBuilder regex = new StringBuilder();
            for (int j = 0; j < n; j++) {
                int idx = (int) Math.pow(m, n - j - 1);
                idx = ((int) Math.floor(i / idx)) % m;
                regex.append(alphabet.get(idx));
            }
            if (regex.toString().length() > max) {
                max = regex.toString().length();
            }
            if (regex.toString().length() < min) {
                min = regex.toString().length();
            }

            try {
                Pattern.compile(regex.toString());
                regexes.add(regex.toString());
            } catch (PatternSyntaxException e) {

            }

        }

        return new Pair<>(numOfStrings, regexes);
    }

    private static void permutation(char[] perm, int pos, String str, List<String> permutations) {
        if (pos == perm.length) {
            permutations.add(new String(perm));
        } else {
            for (int i = 0 ; i < str.length() ; i++) {
                perm[pos] = str.charAt(i);
                permutation(perm, pos+1, str, permutations);
            }
        }
    }

}
