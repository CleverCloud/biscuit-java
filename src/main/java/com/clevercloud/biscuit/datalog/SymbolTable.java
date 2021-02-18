package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import io.vavr.control.Option;

public final class SymbolTable implements Serializable {
   public final List<String> symbols;

   public long insert(final String symbol) {
      int index = this.symbols.indexOf(symbol);
      if (index == -1) {
         this.symbols.add(symbol);
         return this.symbols.size() - 1;
      } else {
         return index;
      }
   }

   public ID add(final String symbol) {
      return new ID.Symbol(this.insert(symbol));
   }

   public Option<Long> get(final String symbol) {
      long index = this.symbols.indexOf(symbol);
      if (index == -1) {
         return Option.none();
      } else {
         return Option.some(index);
      }
   }

   public String print_rule(final Rule r) {
      String res = this.print_predicate(r.head());
      res += " <- " + this.print_rule_body(r);

      return res;
   }

   public String print_rule_body(final Rule r) {
      final List<String> preds = r.body().stream().map((p) -> this.print_predicate(p)).collect(Collectors.toList());
      final List<String> expressions = r.expressions().stream().map((c) -> this.print_expression(c)).collect(Collectors.toList());

      String res =  String.join(", ", preds);
      if(!expressions.isEmpty()) {
         res += ", " + String.join(", ", expressions);
      }
      return res;
   }

   public String print_expression(final Expression e) {
      return e.print(this).get();
   }


   public String print_predicate(final Predicate p) {
      List<String> ids = p.ids().stream().map((i) -> {
         if (i instanceof ID.Variable) {
            return "$" + this.print_symbol((int) ((ID.Variable) i).value());
         } else if (i instanceof ID.Symbol) {
            return "#" + this.print_symbol((int) ((ID.Symbol) i).value());
         } else if (i instanceof ID.Date) {
            return Date.from(Instant.ofEpochSecond(((ID.Date) i).value())).toString();
         } else if (i instanceof ID.Integer) {
            return "" + ((ID.Integer) i).value();
         } else if (i instanceof ID.Str) {
            return "\""+((ID.Str) i).value()+"\"";
         } else {
            return "???";
         }
      }).collect(Collectors.toList());
      return Optional.ofNullable(this.print_symbol((int) p.name())).orElse("<?>") + "(" + String.join(", ", ids) + ")";
   }

   public String print_fact(final Fact f) {
      return this.print_predicate(f.predicate());
   }

   public String print_check(final Check c) {
      String res = "check if ";
      final List<String> queries = c.queries().stream().map((q) -> this.print_rule_body(q)).collect(Collectors.toList());
      return res + String.join(" or ", queries);
   }

   public String print_world(final World w) {
      final List<String> facts = w.facts().stream().map((f) -> this.print_fact(f)).collect(Collectors.toList());
      final List<String> rules = w.rules().stream().map((r) -> this.print_rule(r)).collect(Collectors.toList());
      final List<String> checksStr = w.checks().stream().map((c) -> this.print_check(c)).collect(Collectors.toList());

      StringBuilder b = new StringBuilder();
      b.append("World {\n\tfacts: [\n\t\t");
      b.append(String.join(",\n\t\t", facts));
      b.append("\n\t],\n\trules: [\n\t\t");
      b.append(String.join(",\n\t\t", rules));
      b.append("\n\t],\n\tchecks: [\n\t\t");
      b.append(String.join(",\n\t\t", checksStr));
      b.append("\n\t]\n}");

      return b.toString();
   }

   public String print_symbol(int i) {
      if (i >=0 && i < this.symbols.size()) {
         return this.symbols.get(i);
      } else {
         return "<"+i+"?>";
      }
   }

   public SymbolTable() {
      this.symbols = new ArrayList<>();
   }
   public SymbolTable(SymbolTable s) {
      this.symbols = new ArrayList<>();
      for(String symbol: s.symbols) {
         this.symbols.add(symbol);
      }
   }
}
