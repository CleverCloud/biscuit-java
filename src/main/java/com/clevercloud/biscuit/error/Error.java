package com.clevercloud.biscuit.error;

import io.vavr.control.Option;

import java.util.List;
import java.util.Objects;

public class Error {
    public Option<List<FailedCaveat>> failed_caveats() {
        return Option.none();
    }


    public static class InternalError extends Error {}

    public static class FormatError extends Error {
        public static class Signature extends FormatError {
            public static class InvalidFormat extends Signature {
                public InvalidFormat() {}
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    return true;
                }
            }
            public static class InvalidSignature extends Signature {
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    return true;
                }
            }
        }

        public static class SealedSignature extends FormatError {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                return true;
            }
        }
        public static class EmptyKeys extends FormatError {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                return true;
            }
        }
        public static class UnknownPublicKey extends FormatError {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                return true;
            }
        }
        public static class DeserializationError extends FormatError {
            final public String e;

            public DeserializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                DeserializationError other = (DeserializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.DeserializationError{ error: "+  e + " }";
            }
        }

        public static class SerializationError extends FormatError {
            final public String e;

            public SerializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                SerializationError other = (SerializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.SerializationError{ error: "+  e + " }";
            }
        }
        public static class BlockDeserializationError extends FormatError {
            final public String e;

            public BlockDeserializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BlockDeserializationError other = (BlockDeserializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.BlockDeserializationError{ error: "+  e + " }";
            }
        }
        public static class BlockSerializationError extends FormatError {
            final public String e;

            public BlockSerializationError(String e) {
                this.e = e;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                BlockSerializationError other = (BlockSerializationError) o;
                return e.equals(other.e);
            }

            @Override
            public int hashCode() {
                return Objects.hash(e);
            }

            @Override
            public String toString() {
                return "Error.FormatError.BlockSerializationError{ error: "+  e + " }";
            }
        }

        public static class Version extends FormatError {
            final public int maximum;
            final public int actual;

            public Version(int maximum, int actual) {
                this.maximum = maximum;
                this.actual = actual;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Version version = (Version) o;

                if (maximum != version.maximum) return false;
                return actual == version.actual;
            }

            @Override
            public int hashCode() {
                int result = maximum;
                result = 31 * result + actual;
                return result;
            }

            @Override
            public String toString() {
                return "Version{" +
                        "maximum=" + maximum +
                        ", actual=" + actual +
                        '}';
            }
        }
    }
    public static class InvalidAuthorityIndex extends Error {
        final public long index;

        public InvalidAuthorityIndex(long index) {
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidAuthorityIndex other = (InvalidAuthorityIndex) o;
            return index == other.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }

        @Override
        public String toString() {
            return "Error.InvalidAuthorityIndex{ index: "+ index + " }";
        }
    }
    public static class InvalidBlockIndex extends Error {
        final public long expected;
        final public long found;

        public InvalidBlockIndex(long expected, long found) {
            this.expected = expected;
            this.found = found;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvalidBlockIndex other = (InvalidBlockIndex) o;
            return expected == other.expected && found == other.found;
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, found);
        }

        @Override
        public String toString() {
            return "Error.InvalidBlockIndex{ expected: " + expected + ", found: " + found + " }";
        }
    }
    public static class SymbolTableOverlap extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }
    public static class MissingSymbols extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }
    public static class Sealed extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }
    public static class FailedLogic extends Error {
        final public LogicError error;

        public FailedLogic(LogicError error) {
            this.error = error;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FailedLogic other = (FailedLogic) o;
            return error.equals(other.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(error);
        }

        @Override
        public String toString() {
            return "Error.FailedLogic{ error: "+ error + " }";
        }

        @Override
        public Option<List<FailedCaveat>> failed_caveats() {
            return this.error.failed_caveats();
        }

    }

    public static class TooManyFacts extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }

    public static class TooManyIterations extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }

    public static class Timeout extends Error {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return true;
        }
    }
}
