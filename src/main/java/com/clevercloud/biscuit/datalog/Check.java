package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Check {
    private final List<Rule> queries;

    public Check(List<Rule> queries) {
        this.queries = queries;
    }

    public List<Rule> queries() {
        return queries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queries);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public Schema.CheckV1 serialize() {
        Schema.CheckV1.Builder b = Schema.CheckV1.newBuilder();

        for(int i = 0; i < this.queries.size(); i++) {
            b.addQueries(this.queries.get(i).serialize());
        }

        return b.build();
    }

    public static Either<Error.FormatError, Check> deserializeV0(Schema.CaveatV0 caveat) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (Schema.RuleV0 query: caveat.getQueriesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV0(query);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                queries.add(res.get());
            }
        }

        return Right(new Check(queries));
    }

    public static Either<Error.FormatError, Check> deserializeV1(Schema.CheckV1 check) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (Schema.RuleV1 query: check.getQueriesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV1(query);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                queries.add(res.get());
            }
        }

        return Right(new Check(queries));
    }
}
