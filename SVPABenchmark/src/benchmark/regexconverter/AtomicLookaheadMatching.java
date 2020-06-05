package benchmark.regexconverter;

import RegexParser.*;
import org.sat4j.specs.TimeoutException;
import utilities.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AtomicLookaheadMatching {

    public static List<Pair<String, String>> match(FormulaNode r, String w) throws TimeoutException {
        if (!(r instanceof RegexNode)) {
            System.out.println("Argument to AtomicLookaheadMatching.match must be of type RegexNode!");
            System.exit(1);
        }
        RegexNode re = (RegexNode) r;

        RegexNode noLook = removeLookaheads(re.copy());
        RegexNode noAtomic = removeAtomicOperators(re.copy());

//        Pair<SAFA<CharPred, Character>, HashStringEncodingUnaryCharIntervalSolver> p = RegexSubMatching.constructSubMatchingSAFA(noAtomic);
//        char delimiter = p.second.getDelimiter();

        /* XXX
           Instead of
         */

        StringBuilder sb = new StringBuilder();
        r.toRaw(sb);

        List<Pair<String, String>> filtered = new LinkedList<>();
        for (Pair<String, String> curr : m(noLook, w)) {

            if (Pattern.compile(sb.toString()).matcher(curr.first + curr.second).find()) {
                filtered.add(curr);
            }
//            if (p.first.accepts(TestUtils.lOfS(curr.first + delimiter + curr.second), p.second)) {
//            filtered.add(curr);
//            }
        }

        return filtered;
    }

    public static List<Pair<String, String>> m(RegexNode r, String w) {
        List<Pair<String, String>> seq = new LinkedList<>();
        if (r instanceof CharNode) {
            String c = Character.toString(((CharNode) r).getChar());
            if (w.startsWith(c)) {
                seq.add(new Pair<>(c, w.substring(1)));
            } else {
                seq = new LinkedList<>();
            }
        } else if (r instanceof UnionNode) {
            UnionNode unionNode = (UnionNode) r;
            List<Pair<String, String>> tmp = new LinkedList<>();
            tmp.addAll(m(unionNode.getMyRegex1(), w));
            tmp.addAll(m(unionNode.getMyRegex2(), w));
            return dedup(tmp);
        } else if (r instanceof StarNode) {
            StarNode starNode = (StarNode) r;

            List<Pair<String, String>> toDedup = new LinkedList<>();
            for (Pair<String, String> curr : m(starNode.getMyRegex1(), w)) {
                if (curr.first.equals("") && curr.second.equals(w)) {
                    toDedup.add(new Pair<>(curr.first, w));
                } else {
                    for (Pair<String, String> rem : m(r, customSetMinus(curr.first, w))) {
                        rem.first = curr.first + rem.first;
                        toDedup.add(rem);
                    }
                }
            }
            toDedup.add(new Pair<>("", w));

            return dedup(toDedup);
        } else if (r instanceof ConcatenationNode) {
            ConcatenationNode concatNode = (ConcatenationNode) r;

            if (concatNode.getList().size() == 1) {
                return m(concatNode.getList().get(0), w);
            }

            RegexNode left = concatNode.getList().get(0);
            RegexNode right = new ConcatenationNode(concatNode.getList().subList(1, concatNode.getList().size()));

            List<Pair<String, String>> toDedup = new LinkedList<>();
            for (Pair<String, String> curr : m(left, w)) {
                for (Pair<String, String> rem : m(right, customSetMinus(curr.first, w))) {
                    rem.first = curr.first + rem.first;
                    toDedup.add(rem);
                }
            }

            return dedup(toDedup);
        } else if (r instanceof AtomicGroupNode) {
            AtomicGroupNode atomicGroupNode = (AtomicGroupNode) r;
            List<Pair<String, String>> vSeq = m(atomicGroupNode.getMyRegex1(), w);

            if (vSeq.size() == 0) {
                return vSeq;
            } else {
                return vSeq.subList(0, 1);
            }
        }

        return seq;
    }

    private static List<Pair<String, String>> dedup(List<Pair<String, String>> seq) {
        return seq.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private static String customSetMinus(String v, String w) {
        if (w.startsWith(v)) {
            return w.substring(v.length());
        }

        System.exit(2);
        return null;
    }

    private static RegexNode removeLookaheads(RegexNode r) {
        RegexNode node = null;
        if (r instanceof UnionNode) {
            UnionNode union = (UnionNode) r;
            node = new UnionNode(removeLookaheads(union.getMyRegex1()), removeLookaheads(union.getMyRegex2()));
        } else if (r instanceof CharNode) {
            node = r;
        } else if (r instanceof StarNode) {
            StarNode star = (StarNode) r;
            node = new StarNode(removeLookaheads(star.getMyRegex1()), star.quantifierType);
        } else if (r instanceof ConcatenationNode) {
            ConcatenationNode concat = (ConcatenationNode) r;
            List<RegexNode> nodes = new LinkedList<>();
            for (RegexNode rn : concat.getList()) {
                if (!(rn instanceof PositiveLookaheadNode || rn instanceof NegativeLookaheadNode)) {
                    nodes.add(removeLookaheads(rn));
                }
            }
            node = new ConcatenationNode(nodes);
        } else if (r instanceof AtomicGroupNode) {
            AtomicGroupNode atomic = (AtomicGroupNode) r;
            node = new AtomicGroupNode(removeLookaheads(atomic.getMyRegex1()));
        } else {
            System.out.println("Wrong instance of RegexNode in removeLookaheads!");
            System.exit(1);
        }

        return node;
    }

    private static RegexNode removeAtomicOperators(RegexNode r) {
        RegexNode node = null;
        if (r instanceof UnionNode) {
            UnionNode union = (UnionNode) r;
            node = new UnionNode(removeAtomicOperators(union.getMyRegex1()), removeAtomicOperators(union.getMyRegex2()));
        } else if (r instanceof CharNode) {
            node = r;
        } else if (r instanceof StarNode) {
            StarNode star = (StarNode) r;
            node = new StarNode(removeAtomicOperators(star.getMyRegex1()), star.quantifierType);
        } else if (r instanceof ConcatenationNode) {
            ConcatenationNode concat = (ConcatenationNode) r;
            List<RegexNode> nodes = new LinkedList<>();
            for (RegexNode rn : concat.getList()) {
                if (rn instanceof AtomicGroupNode) {
                    AtomicGroupNode agn = (AtomicGroupNode) rn;
                    nodes.add(removeAtomicOperators(agn.myRegex1));
                } else {
                    nodes.add(removeAtomicOperators(rn));
                }
            }
            node = new ConcatenationNode(nodes);
        } else if (r instanceof PositiveLookaheadNode || r instanceof NegativeLookaheadNode) {
            node = r;
        } else if (r instanceof AtomicGroupNode) {
            AtomicGroupNode atomicGroupNode = (AtomicGroupNode) r;
            return removeAtomicOperators(atomicGroupNode.getMyRegex1());
        } else {
            System.out.println("Wrong instance of RegexNode in removeAtomicGroupNode!");
            System.exit(1);
        }

        return node;
    }

}
