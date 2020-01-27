package benchmark.regexconverter;

import RegexParser.*;

import java.util.*;

public class RegexTranslator {

    public static RegexNode translate(RegexNode regexNode) {
        setParents(regexNode);
        return _translate(regexNode);
    }

    public static RegexNode _translate(RegexNode regexNode) {
        if (regexNode instanceof ConcatenationNode) {
            ConcatenationNode node = (ConcatenationNode) regexNode;

            List<RegexNode> nodes = new LinkedList<>();
            node.getList().forEach(child -> nodes.add(_translate(child)));
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

            /* Et(E*) */
            List<RegexNode> nodes = new LinkedList<>();
            nodes.add(plusNode.getMyRegex1());

            nodes.add(_translate(new StarNode(plusNode.getMyRegex1(), QuantifierType.GREEDY)));

            return new ConcatenationNode(nodes);
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
        return sb.toString();
    }

    public static RegexNode follow(RegexNode regexNode) {
        List<RegexNode> subexpressions = new LinkedList<>();
        Set<RegexNode> visited = new HashSet<>();

        StringBuilder sb = new StringBuilder();
        regexNode.toString(sb);

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
        } else if (regexNode.parent !=  null && regexNode instanceof StarNode) {
            visited.add(((StarNode) regexNode).getMyRegex1());
            visited.add(regexNode);
            subexpressions.add(((StarNode) regexNode).getMyRegex1());
            addRemainingIfCan(regexNode, subexpressions, visited);
            return follow(regexNode.parent, subexpressions, visited);
        } else if (regexNode.parent instanceof StarNode) {
            visited.add(regexNode);
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
        if (regexNode.parent instanceof ConcatenationNode) {
            ConcatenationNode concat = (ConcatenationNode) regexNode.parent;
            int idx = concat.getList().indexOf(regexNode);
            assert idx != -1;

            for (int i = idx + 1; i < concat.getList().size(); i++) {
                RegexNode node = concat.getList().get(i);

                if (!visited.contains(node)) {
                    visited.add(node);
                    subexpressions.add(node);
                }
            }
        }
    }

    public static void setParents(RegexNode regexNode) {
        setParents(regexNode, null);
    }

    private static void setParents(RegexNode child, RegexNode parent) {
        child.parent = parent;
        if (child instanceof AnchorNode) {
            AnchorNode node = (AnchorNode) child;
            setParents(node.getMyRegex1(), node);
        } else if (child instanceof AtomicGroupNode) {
            AtomicGroupNode node = (AtomicGroupNode) child;
            setParents(node.myRegex1, node);
        } else if (child instanceof CharacterClassNode) {
        } else if (child instanceof CharNode) {
        } else if (child instanceof ConcatenationNode) {
            ConcatenationNode node = (ConcatenationNode) child;
            node.getList().forEach(c -> setParents(c, node));
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
            System.err.println("Unsupported RegexNode child");
            System.exit(-1);
        }

    }

}
