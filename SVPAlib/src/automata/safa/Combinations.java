package automata.safa;

import java.util.*;

public class Combinations {

    /**
     * Combines several collections of elements and create permutations of all of them, taking one element from each
     * collection, and keeping the same order in resultant lists as the one in original list of collections.
     *
     * <ul>Example
     * <li>Input  = { {a,b,c} , {1,2,3,4} }</li>
     * <li>Output = { {a,1} , {a,2} , {a,3} , {a,4} , {b,1} , {b,2} , {b,3} , {b,4} , {c,1} , {c,2} , {c,3} , {c,4} }</li>
     * </ul>
     *
     * @param collections Original list of collections which elements have to be combined.
     * @return Resultant collection of lists with all permutations of original list.
     */
    public static <T> List<Set<T>> calculateCombinations(List<Set<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<Set<T>> res = new LinkedList<>();
            combinationsImpl(collections, res, 0, new HashSet<T>());
            return res;
        }
    }

    /**
     * Recursive implementation for permutations
     */
    private static <T> void combinationsImpl(List<Set<T>> ori, List<Set<T>> res, int d, Set<T> current) {
        // if depth equals number of original collections, final reached, add and return
        if (d == ori.size()) {
            res.add(current);
            return;
        }

        // iterate from current collection and copy 'current' element N times, one for each element
        Collection<T> currentCollection = ori.get(d);
        for (T element : currentCollection) {
            Set<T> copy = new HashSet<>(current);
            copy.add(element);
            combinationsImpl(ori, res, d + 1, copy);
        }
    }

}
