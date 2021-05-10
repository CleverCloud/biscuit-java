package com.clevercloud.biscuit.token.builder;

import com.clevercloud.biscuit.datalog.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Rule {
    Predicate head;
    List<Predicate> body;
    List<Expression> expressions;

    public Rule(Predicate head, List<Predicate> body, List<Expression> expressions) {
        this.head = head;
        this.body = body;
        this.expressions = expressions;
    }

    public com.clevercloud.biscuit.datalog.Rule convert(SymbolTable symbols) {
        com.clevercloud.biscuit.datalog.Predicate head = this.head.convert(symbols);
        ArrayList<com.clevercloud.biscuit.datalog.Predicate> body = new ArrayList<>();
        ArrayList<com.clevercloud.biscuit.datalog.expressions.Expression> expressions = new ArrayList<>();

        for(Predicate p: this.body) {
            body.add(p.convert(symbols));
        }

        for(Expression e: this.expressions) {
            expressions.add(e.convert(symbols));
        }

        return new com.clevercloud.biscuit.datalog.Rule(head, body, expressions);
    }

    public static Rule convert_from(com.clevercloud.biscuit.datalog.Rule r, SymbolTable symbols) {
        Predicate head = Predicate.convert_from(r.head(), symbols);

        ArrayList<Predicate> body = new ArrayList<>();
        ArrayList<Expression> expressions = new ArrayList<>();

        for(com.clevercloud.biscuit.datalog.Predicate p: r.body()) {
            body.add(Predicate.convert_from(p, symbols));
        }

        for(com.clevercloud.biscuit.datalog.expressions.Expression e: r.expressions()) {
            expressions.add(Expression.convert_from(e, symbols));
        }

        return new Rule(head, body, expressions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rule rule = (Rule) o;

        if (head != null ? !head.equals(rule.head) : rule.head != null) return false;
        if (body != null ? !body.equals(rule.body) : rule.body != null) return false;
        return expressions != null ? expressions.equals(rule.expressions) : rule.expressions == null;
    }

    @Override
    public int hashCode() {
        int result = head != null ? head.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (expressions != null ? expressions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final List<String> b = body.stream().map((pred) -> pred.toString()).collect(Collectors.toList());
        String res = head.toString() + " <- " + String.join(", ", b);

        if(!expressions.isEmpty()) {
            final List<String> e = expressions.stream().map((expression) -> expression.toString()).collect(Collectors.toList());
            res += ", "+ String.join(", ", e);
        }

        return res;
    }
}
