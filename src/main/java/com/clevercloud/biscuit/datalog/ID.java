package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.builder.Atom;
import io.vavr.control.Either;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;


public abstract class ID implements Serializable {
   public abstract boolean match(final ID other);
   public abstract Schema.ID serialize();
   static public Either<Error.FormatError, ID> deserialize_enum(Schema.ID id) {
      if(id.getKind() == Schema.ID.Kind.DATE) {
         return Date.deserialize(id);
      } else if(id.getKind() == Schema.ID.Kind.INTEGER) {
         return Integer.deserialize(id);
      } else if(id.getKind() == Schema.ID.Kind.STR) {
         return Str.deserialize(id);
      } else if(id.getKind() == Schema.ID.Kind.SYMBOL) {
         return Symbol.deserialize(id);
      } else if(id.getKind() == Schema.ID.Kind.VARIABLE) {
         return Variable.deserialize(id);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
      }
   }

   public abstract Atom toAtom(SymbolTable symbols);

   public final static class Date extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         return false;
      }

      public Date(final long value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Date date = (Date) o;
         return value == date.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return "@" + this.value;
      }

      public Schema.ID serialize() {
         return Schema.ID.newBuilder()
                 .setKind(Schema.ID.Kind.DATE)
                 .setDate(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserialize(Schema.ID id) {
         if(id.getKind() != Schema.ID.Kind.DATE) {
            return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
         } else {
            return Right(new Date(id.getDate()));
         }
      }

      public Atom toAtom(SymbolTable symbols) {
         return new Atom.Date(this.value);
      }
   }

   public final static class Integer extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Integer) {
            return this.value == ((Integer) other).value;
         }
         return false;
      }

      public Integer(final long value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Integer integer = (Integer) o;
         return value == integer.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return "" + this.value;
      }

      public Schema.ID serialize() {
         return Schema.ID.newBuilder()
                 .setKind(Schema.ID.Kind.INTEGER)
                 .setInteger(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserialize(Schema.ID id) {
         if(id.getKind() != Schema.ID.Kind.INTEGER) {
            return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
         } else {
            return Right(new Integer(id.getInteger()));
         }
      }

      public Atom toAtom(SymbolTable symbols) {
         return new Atom.Integer(this.value);
      }
   }

   public final static class Str extends ID implements Serializable {
      private final String value;

      public String value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Str) {
            return this.value.equals(((Str) other).value);
         }
         return false;
      }

      public Str(final String value) {
         this.value = new String(value);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Str str = (Str) o;
         return Objects.equals(value, str.value);
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return this.value;
      }

      public Schema.ID serialize() {
         return Schema.ID.newBuilder()
                 .setKind(Schema.ID.Kind.STR)
                 .setStr(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserialize(Schema.ID id) {
         if(id.getKind() != Schema.ID.Kind.STR) {
            return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
         } else {
            return Right(new Str(id.getStr()));
         }
      }

      public Atom toAtom(SymbolTable symbols) {
         return new Atom.Str(this.value);
      }
   }

   public final static class Symbol extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Symbol) {
            return this.value == ((Symbol) other).value;
         }
         return false;
      }

      public Symbol(final long value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Symbol symbol = (Symbol) o;
         return value == symbol.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return "#" + this.value;
      }

      public Schema.ID serialize() {
         return Schema.ID.newBuilder()
                 .setKind(Schema.ID.Kind.SYMBOL)
                 .setSymbol(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserialize(Schema.ID id) {
         if(id.getKind() != Schema.ID.Kind.SYMBOL) {
            return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
         } else {
            return Right(new Symbol(id.getSymbol()));
         }
      }

      public Atom toAtom(SymbolTable symbols) {
         return new Atom.Symbol(symbols.print_symbol((int) this.value));
      }
   }

   public final static class Variable extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         return true;
      }

      public Variable(final long value) {
         this.value = value;
      }

      public Variable(final String name) {
         long value = 0;
         try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] res = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            value = Byte.toUnsignedLong(res[0]) + (Byte.toUnsignedLong(res[1]) << 8) + (Byte.toUnsignedLong(res[2]) << 16) + (Byte.toUnsignedLong(res[3]) << 24);
         } catch (NoSuchAlgorithmException e) {
            assert e == null;
         }
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Variable variable = (Variable) o;
         return value == variable.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return this.value + "?";
      }

      public Schema.ID serialize() {
         return Schema.ID.newBuilder()
                 .setKind(Schema.ID.Kind.VARIABLE)
                 .setVariable((int) this.value).build();
      }

      static public Either<Error.FormatError, ID> deserialize(Schema.ID id) {
         if(id.getKind() != Schema.ID.Kind.VARIABLE) {
            return Left(new Error().new FormatError().new DeserializationError("invalid ID kind"));
         } else {
            return Right(new Variable(id.getVariable()));
         }
      }

      public Atom toAtom(SymbolTable symbols) {
         return new Atom.Variable((int) this.value);
      }
   }
}
