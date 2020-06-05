package benchmark.regexconverter;

import RegexParser.*;
import org.apache.commons.lang3.SerializationUtils;

import java.util.*;

public class RegexTranslator {

    public static RegexNode removeAtomicOperators(String atomicRegex) throws RegexTranslationException {
        List<RegexNode> nodes = RegexParserProvider.parse(new String[]{ atomicRegex });
        return removeAtomicOperators(nodes.get(0));
    }

    public static RegexNode removeAtomicOperators(RegexNode regexNode) throws RegexTranslationException {
        if (regexNode instanceof UnionNode) {
            UnionNode union = (UnionNode) regexNode;
            return new UnionNode(removeAtomicOperators(union.getMyRegex1()), removeAtomicOperators(union.getMyRegex2()));
        } else if (regexNode instanceof CharNode) {
            return regexNode;
        } else if (regexNode instanceof StarNode) {
            StarNode star = (StarNode) regexNode;
            return new StarNode(removeAtomicOperators(star.getMyRegex1()), star.quantifierType);
        } else if (regexNode instanceof ConcatenationNode) {
            ConcatenationNode concat = (ConcatenationNode) regexNode;
            List<RegexNode> nodes = new LinkedList<>();
            for (RegexNode rn : concat.getList()) {
                if (rn instanceof AtomicGroupNode) {
                    AtomicGroupNode agn = (AtomicGroupNode) rn;

                    if (hasNestedAtomicGroups(agn)) {
                        throw new RegexTranslationException("Nested atomic groups not supported.");
                    }

                    nodes.add(translate(agn.getMyRegex1()));
                } else {
                    nodes.add(removeAtomicOperators(rn));
                }
            }
            return new ConcatenationNode(nodes);
        } else if (regexNode instanceof PositiveLookaheadNode || regexNode instanceof NegativeLookaheadNode) {
            return regexNode;
        } else if (regexNode instanceof AtomicGroupNode) {
            AtomicGroupNode atomicGroupNode = (AtomicGroupNode) regexNode;

            if (hasNestedAtomicGroups(atomicGroupNode)) {
                throw new RegexTranslationException("Nested atomic groups not supported.");
            }

            return translate(atomicGroupNode.getMyRegex1());
        } else if (regexNode instanceof OptionalNode) {
            OptionalNode optionalNode = (OptionalNode) regexNode;
            return new OptionalNode(removeAtomicOperators(optionalNode.getMyRegex1()), optionalNode.quantifierType);
        } else {
            /* TODO handle instances and throw exception if was not any of them */
            return regexNode;
        }
    }

    public static RegexNode translate(RegexNode regexNode) throws RegexTranslationException {
        setParents(regexNode);
        return _translate(regexNode);
    }

    private static RegexNode _translate(RegexNode regexNode) throws RegexTranslationException {
        if (regexNode instanceof ConcatenationNode) {
            ConcatenationNode node = (ConcatenationNode) regexNode;

            List<RegexNode> nodes = new LinkedList<>();
            for (RegexNode child : node.getList()) {
                nodes.add(_translate(child));
            }
            regexNode = new ConcatenationNode(nodes);
        } else if (regexNode instanceof UnionNode) {
            /* E|F */
            UnionNode node = (UnionNode) regexNode;

            /* (?!Ef(E)) */
            List<RegexNode> nodes = new LinkedList<>();
            nodes.add(node.getMyRegex1());

            RegexNode f = follow(node.getMyRegex1());
            if (f != null) {
                nodes.add(f);
            }
            ConcatenationNode concat = new ConcatenationNode(nodes);
            NegativeLookaheadNode negativeLookaheadNode = new NegativeLookaheadNode(concat);

            /* t(E) */
            RegexNode left = _translate(node.getMyRegex1());

            /* (?!Ef(E))t(F) */
            RegexNode right = _translate(node.getMyRegex2());
            List<RegexNode> rightBranch = new LinkedList<>();
            rightBranch.add(negativeLookaheadNode);
            rightBranch.add(right);

            /* t(E)|(?!(Ef(E)))t(F) */
            return new UnionNode(left, new ConcatenationNode(rightBranch));

        } else if (regexNode instanceof StarNode) {
            /* E* */
            StarNode starNode = (StarNode) regexNode;

            /* no negative lookahead if E* is final subexpression */
            RegexNode follow = follow(starNode);
            if (follow != null && follow.equals(starNode)) {
                return new StarNode(_translate(starNode.getMyRegex1()), starNode.quantifierType);
            }

            /* (?!f(E)) */
            NegativeLookaheadNode neg = new NegativeLookaheadNode(follow);

            /* t(E)* */
            RegexNode _translated = new StarNode(_translate(starNode.getMyRegex1()), starNode.quantifierType);

            /* t(E)*(?!Ef(E)) */
            List<RegexNode> star = new LinkedList<>();
            star.add(_translated);
            star.add(neg);
            return new ConcatenationNode(star);
        } else if (regexNode instanceof PlusNode) {
            /* E+ */
            PlusNode plusNode = (PlusNode) regexNode;

            /* t(E.E*) */
            List<RegexNode> nodes = new LinkedList<>();

            nodes.add(SerializationUtils.clone(plusNode.getMyRegex1()));
            StarNode starNode = new StarNode(plusNode.getMyRegex1(), QuantifierType.GREEDY);
            starNode.getMyRegex1().parent = starNode;
            nodes.add(starNode);

            ConcatenationNode concat = new ConcatenationNode(nodes);
            nodes.forEach(n -> n.parent = concat);
            concat.parent = regexNode.parent;

            /* XXX: FIXME */
            /* Change PlusNode to ConcatenationNode in parse tree. */
            /* Change interface of FormulaNode in automatark to give all */
            /* child nodes of any given node in the parse tree. */
            if (regexNode.parent instanceof ConcatenationNode) {
                ConcatenationNode c = (ConcatenationNode) regexNode.parent;
                for (int i = 0; i < c.getList().size(); i++) {
                    RegexNode r = c.getList().get(i);
                    if (r == regexNode) {
                        c.getList().set(i, concat);
                        break;
                    }
                }
            }

            return _translate(concat);
        } else if (regexNode instanceof OptionalNode) {
            /* E? */
            OptionalNode optionalNode = (OptionalNode) regexNode;

            /* t(E) */
            RegexNode t = _translate(optionalNode.getMyRegex1());

            /* (?!E) */
            NegativeLookaheadNode neg = new NegativeLookaheadNode(optionalNode.getMyRegex1());

            /* t(E)|(?!E) */
            return new UnionNode(t, neg);
        }

        return regexNode;
    }

    public static String printNode(RegexNode regexNode) {
        if (regexNode == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        regexNode.toString(sb);
        return sb.toString().replaceAll("Char:", "");
    }

    public static RegexNode follow(RegexNode regexNode) {
        List<RegexNode> subexpressions = new LinkedList<>();
        Set<RegexNode> visited = new HashSet<>();

        if (regexNode instanceof CharNode && regexNode.parent instanceof ConcatenationNode) {
            ConcatenationNode node = (ConcatenationNode) regexNode.parent;
            for (RegexNode r : node.getList()) {
                if (r instanceof CharNode) {
                    visited.add(r);
                }
            }
        }

        follow(regexNode, subexpressions, visited);

        if (subexpressions.size() == 0) {
            return null;
        } else if (subexpressions.size() == 1) {
            return subexpressions.get(0);
        }

        RegexNode followRegex = subexpressions.get(0);
        for (int i = 1; i < subexpressions.size(); i++) {
            List<RegexNode> concat = new LinkedList<>();
            concat.add(followRegex);
            concat.add(subexpressions.get(i));
            followRegex = new ConcatenationNode(concat);
        }

        return followRegex;
    }

    private static RegexNode follow(RegexNode regexNode,
                                    List<RegexNode> subexpressions,
                                    Set<RegexNode> visited) {
        /* f(G|H) = f(G|H) if G|H is a subexpression */
        if (regexNode instanceof UnionNode) {
            visited.add(regexNode);
            visited.add(((UnionNode) regexNode).getMyRegex1());
            addRemainingIfCan(regexNode, subexpressions, visited);
            if (regexNode.parent != null) {
                return follow(regexNode.parent, subexpressions, visited);
            }
        /* f(G*) = Gf(F*) if G* is a subexpression */
        } else if (regexNode instanceof StarNode) {
            visited.add(((StarNode) regexNode).getMyRegex1());
            visited.add(regexNode);
            subexpressions.add(((StarNode) regexNode).getMyRegex1());
            addRemainingIfCan(regexNode, subexpressions, visited);
            return follow(regexNode.parent, subexpressions, visited);
        }

        visited.add(regexNode);

        if (regexNode.parent == null) {
            return regexNode;
        }

        return follow(regexNode.parent, subexpressions, visited);
    }

    private static void addRemainingIfCan(RegexNode regexNode,
                                          List<RegexNode> subexpressions,
                                          Set<RegexNode> visited) {
        RegexNode current = regexNode;
        while (current.parent instanceof ConcatenationNode) {
            ConcatenationNode concat = (ConcatenationNode) current.parent;
            int idx = concat.getList().indexOf(current);
            assert idx != -1;

            for (int i = idx + 1; i < concat.getList().size(); i++) {
                RegexNode node = concat.getList().get(i);

                if (!visited.contains(node)) {
                    visited.add(node);
                    subexpressions.add(node);
                }
            }

            current = current.parent;
        }
    }

    private static boolean hasNestedAtomicGroups(RegexNode node) {
        for (RegexNode n : node.children()) {
            if (n instanceof AtomicGroupNode || hasNestedAtomicGroups(n)) {
                return true;
            }
        }

        return false;
    }

    public static void setParents(RegexNode regexNode) throws RegexTranslationException {
        setParents(regexNode, null);
    }

    private static void setParents(RegexNode child, RegexNode parent) throws RegexTranslationException {
        child.parent = parent;
        if (child instanceof AnchorNode) {
            AnchorNode node = (AnchorNode) child;
            /* TODO */
            // setParents(node.getMyRegex1(), node);
        } else if (child instanceof AtomicGroupNode) {
            AtomicGroupNode node = (AtomicGroupNode) child;
            setParents(node.myRegex1, node);
        } else if (child instanceof CharacterClassNode) {
        } else if (child instanceof CharNode) {
        } else if (child instanceof ConcatenationNode) {
            ConcatenationNode node = (ConcatenationNode) child;
            for (RegexNode c : node.getList()) {
                setParents(c, node);
            }
        } else if (child instanceof IntervalNode) {
        } else if (child instanceof ModifierNode) {
            ModifierNode node = (ModifierNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof NegativeLookaheadNode) {
            NegativeLookaheadNode node = (NegativeLookaheadNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof NotCharacterClassNode) {
        } else if (child instanceof OptionalNode) {
            OptionalNode node = (OptionalNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof PlusNode) {
            PlusNode node = (PlusNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof PositiveLookaheadNode) {
            PositiveLookaheadNode node = (PositiveLookaheadNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof RepetitionNode) {
            RepetitionNode node = (RepetitionNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof StarNode) {
            StarNode node = (StarNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof UnionNode) {
            UnionNode node = (UnionNode) child;
            setParents(node.getMyRegex1(), node);
            setParents(node.getMyRegex2(), node);
        } else {
            throw new RegexTranslationException("Unsupported RegexNode child.");
        }
    }

}
