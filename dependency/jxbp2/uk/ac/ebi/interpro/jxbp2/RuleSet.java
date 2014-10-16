package uk.ac.ebi.interpro.jxbp2;


import uk.ac.ebi.interpro.common.performance.*;
import org.w3c.dom.*;

import java.util.*;

public class RuleSet extends RCO {
    private String key = String.valueOf(System.identityHashCode(this));

    public class NodeInfo {
        public BindingAction action;
        public Shredded shredded;
        public Node node;

        public NodeInfo(Node node) {
            this.node = node;
            for (Rule rule : rules) {
                action = rule.test(node);
                if (action != null) return;
            }

            if (node instanceof Text || node instanceof Attr) {
                shredded = new Shredded(node.getNodeValue(), null, null);

                for (Rule rule : rules) {
                    int[] found = new int[2];

                    Shredded process = shredded;

                    while (process != null) {
                        BindingAction action;
                        if ((action = rule.test(node, process.prior, found)) != null) process.split(found, action);
                        process = process.next;
                    }
                }
            }
        }
    }

    public NodeInfo getNodeInfo(Node node) {
        NodeInfo ni= (NodeInfo) node.getUserData(key);
        if (ni==null) node.setUserData(key,ni=new NodeInfo(node),null);
        return ni;

    }


    public RuleSet(List<Rule> rules) {
        this.rules = rules;
    }

    public RuleSet(Rule... rules) {
        this.rules = Arrays.asList(rules);
    }

    List<Rule> rules=new ArrayList<Rule>();


    public class Shredded {
        public String prior;
        public BindingAction action;
        public Shredded next;
        public String replaced;

        public String toString() {
            return prior + " " + action + (next == null ? "" : " -> " + next);
        }

        public Shredded(String prior, BindingAction action, Shredded next) {
            this.prior = prior;
            this.action = action;
            this.next = next;
        }

        public void split(int[] found, BindingAction newAction) {
            next = new Shredded(prior.substring(found[1]), action, next);
            action = newAction;
            replaced=prior.substring(found[0],found[1]);
            prior = prior.substring(0, found[0]);


        }
    }


}
