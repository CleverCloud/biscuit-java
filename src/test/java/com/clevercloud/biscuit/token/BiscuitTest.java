package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;
import java.util.*;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Utils.*;

import com.clevercloud.biscuit.token.builder.Block;

public class BiscuitTest extends TestCase {
    public BiscuitTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiscuitTest.class);
    }

    public void testBasic() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build()).get();

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data).get();

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_caveat(caveat(rule(
                "caveat1",
                Arrays.asList(var(0)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var(0), s("read")))
                )
        )));

        Biscuit b2 = deser.append(rng, keypair2, builder.build()).get();

        System.out.println(b2.print());

        System.out.println("serializing the second token");

        byte[] data2 = b2.serialize().get();

        System.out.print("data len: ");
        System.out.println(data2.length);
        System.out.println(hex(data2));

        System.out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.from_bytes(data2).get();

        System.out.println(deser2.print());

        // THIRD BLOCK
        System.out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.create_block();
        builder3.add_caveat(caveat(rule(
                "caveat2",
                Arrays.asList(s("file1")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), s("file1")))
                )
        )));

        Biscuit b3 = deser2.append(rng, keypair3, builder3.build()).get();

        System.out.println(b3.print());

        System.out.println("serializing the third token");

        byte[] data3 = b3.serialize().get();

        System.out.print("data len: ");
        System.out.println(data3.length);
        System.out.println(hex(data3));

        System.out.println("deserializing the third token");
        Biscuit final_token = Biscuit.from_bytes(data3).get();

        System.out.println(final_token.print());

        // check
        System.out.println("will check the token for resource=file1 and operation=read");

        SymbolTable check_symbols = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts = Arrays.asList(
                fact("resource", Arrays.asList(s("ambient"), s("file1"))).convert(check_symbols),
                fact("operation", Arrays.asList(s("ambient"), s("read"))).convert(check_symbols)
        );

        Either<Error, HashMap<String, Set<Fact>>> res = final_token.check(check_symbols, ambient_facts,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());

        Assert.assertTrue(res.isRight());

        System.out.println("will check the token for resource=file2 and operation=write");

        SymbolTable check_symbols2 = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts2 = Arrays.asList(
                fact("resource", Arrays.asList(s("ambient"), s("file2"))).convert(check_symbols2),
                fact("operation", Arrays.asList(s("ambient"), s("write"))).convert(check_symbols2)
        );

        Either<Error, HashMap<String, Set<Fact>>> res2 = final_token.check(check_symbols2, ambient_facts2,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        Assert.assertTrue(res2.isLeft());
        System.out.println(res2.getLeft());

        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(1, 0, "*caveat1($0) <- !resource(#ambient, $0), !operation(#ambient, #read), !right(#authority, $0, #read)"),
                        new FailedCaveat().new FailedBlock(2, 0, "*caveat2(#file1) <- !resource(#ambient, #file1)")
                ))),
                res2.getLeft());
    }

    public void testFolders() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        com.clevercloud.biscuit.token.builder.Biscuit builder = Biscuit.builder(rng, root);

        builder.add_right("/folder1/file1", "read");
        builder.add_right("/folder1/file1", "write");
        builder.add_right("/folder1/file2", "read");
        builder.add_right("/folder1/file2", "write");
        builder.add_right("/folder2/file3", "read");

        System.out.println(builder.build());
        Biscuit b = builder.build().get();

        System.out.println(b.print());

        Block block2 = b.create_block();
        block2.resource_prefix("/folder1/");
        block2.check_right("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.append(rng, keypair2, block2.build()).get();

        Verifier v1 = b2.verify(root.public_key()).get();
        v1.add_resource("/folder1/file1");
        v1.add_operation("read");
        Either<Error, Void> res = v1.verify();
        Assert.assertTrue(res.isRight());

        Verifier v2 = b2.verify(root.public_key()).get();
        v2.add_resource("/folder2/file3");
        v2.add_operation("read");
        res = v2.verify();
        Assert.assertTrue(res.isLeft());

        Verifier v3 = b2.verify(root.public_key()).get();
        v3.add_resource("/folder2/file1");
        v3.add_operation("write");
        res = v3.verify();

        Error e = res.getLeft();
        Assert.assertTrue(res.isLeft());

        for(FailedCaveat f: e.failed_caveats().get()) {
            System.out.println(f.toString());
        }
        Assert.assertEquals(
                new Error().new FailedLogic(new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(1, 0, "*prefix($0) <- !resource(#ambient, $0) @ $0 matches /folder1/*"),
                        new FailedCaveat().new FailedBlock(1, 1, "*check_right(#read) <- !resource(#ambient, $0), !operation(#ambient, #read), !right(#authority, $0, #read)")
                ))),
                e);
    }

    public void testSealedTokens() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build()).get();

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data).get();

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_caveat(caveat(rule(
                "caveat1",
                Arrays.asList(var(0)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var(0), s("read")))
                )
        )));

        Biscuit b2 = deser.append(rng, keypair2, builder.build()).get();

        System.out.println(b2.print());

        System.out.println("sealing the second token");

        byte[] sealed = b2.seal("testkey".getBytes()).get();
        System.out.print("sealed data len: ");
        System.out.println(sealed.length);
        System.out.println(hex(sealed));

        System.out.println("deserializing the sealed token with an invalid key");
        Error e = Biscuit.from_sealed(sealed, "not this key".getBytes()).getLeft();
        System.out.println(e);
        Assert.assertEquals(
                new Error().new FormatError().new Signature().new SealedSignature(),
                e);

        System.out.println("deserializing the sealed token with a valid key");
        Biscuit deser2 = Biscuit.from_sealed(sealed, "testkey".getBytes()).get();
        System.out.println(deser2.print());

        System.out.println("trying to append to a sealed token");
        Block builder2 = deser2.create_block();
        Error e2 = deser2.append(rng, keypair2, builder.build()).getLeft();
    }

    public void testBiscuitCopy() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file2"), s("read"))));
        authority_builder.add_fact(fact("right", Arrays.asList(s("authority"), s("file1"), s("write"))));

        Biscuit b = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build()).get();
        Biscuit b2 = new Biscuit(b);

        System.out.println(b.toString());
        System.out.println(b2.toString());

        // equals use equals methods and == check references for objects and values for primitives
        assertEquals(b, b2);
        assertFalse(b == b2);

        assertEquals(b.authority, b2.authority);
        assertFalse(b.authority == b2.authority);

        assertEquals(b.blocks, b2.blocks);
        assertFalse(b.blocks == b2.blocks);
        for (int i = 0; i < b.blocks.size(); i++) {
            com.clevercloud.biscuit.token.Block bBlock = b.blocks.get(i);
            com.clevercloud.biscuit.token.Block b2Block = b.blocks.get(i);

            assertEquals(bBlock, b2Block);
            assertFalse(bBlock == b2Block);

            assertEquals(bBlock.index, b2Block.index);
            assertEquals(bBlock.context, b2Block.context);
            assertFalse(bBlock.context == b2Block.context);

            assertEquals(bBlock.caveats, b2Block.caveats);
            assertFalse(bBlock.caveats == b2Block.caveats);
            for (int j = 0; j < bBlock.caveats.size(); j++) {
                Caveat c = bBlock.caveats.get(j);
                Caveat c2 = b2Block.caveats.get(j);

                assertEquals(c, c2);
                assertFalse(c == c2);

                for (int k = 0; k < c.queries().size(); k ++) {
                    Rule r = c.queries().get(k);
                    Rule r2 = c2.queries().get(k);

                    assertEquals(r, r2);
                    assertFalse(r == r2);

                    for (int l = 0; l < r.body().size(); l++) {
                        Predicate p = r.body().get(l);
                        Predicate p2 = r2.body().get(l);

                        assertEquals(p, p2);
                        assertFalse(p == p2);
                    }

                    for (int l = 0; l < r.constraints().size(); l++) {
                        Constraint ct = r.constraints().get(l);
                        Constraint ct2 = r2.constraints().get(l);

                        assertEquals(ct, ct2);
                        assertFalse(ct == ct2);
                    }
                }
            }

            assertEquals(bBlock.rules, b2Block.rules);
            assertFalse(bBlock.rules == b2Block.rules);
            for (int j = 0; j < bBlock.rules.size(); j++) {
                Rule r = bBlock.rules.get(j);
                Rule r2 = b2Block.rules.get(j);

                assertEquals(r, r2);
                assertFalse(r == r2);

                for (int l = 0; l < r.body().size(); l++) {
                    Predicate p = r.body().get(l);
                    Predicate p2 = r2.body().get(l);

                    assertEquals(p, p2);
                    assertFalse(p == p2);
                }

                for (int l = 0; l < r.constraints().size(); l++) {
                    Constraint ct = r.constraints().get(l);
                    Constraint ct2 = r2.constraints().get(l);

                    assertEquals(ct, ct2);
                    assertFalse(ct == ct2);
                }
            }

            assertEquals(bBlock.facts, b2Block.facts);
            assertFalse(bBlock.facts == b2Block.facts);
            for (int j = 0; j < bBlock.facts.size(); j++) {
                Fact f = bBlock.facts.get(j);
                Fact f2 = b2Block.facts.get(j);

                assertEquals(f, f2);
                assertFalse(f == f2);

                Predicate p = f.predicate();
                Predicate p2 = f2.predicate();

                assertEquals(p, p2);
                assertFalse(p == p2);
            }
        }

        assertEquals(b.symbols.symbols, b2.symbols.symbols);
        assertFalse(b.symbols.symbols == b2.symbols.symbols);
        for (int i = 0; i < b.symbols.symbols.size(); i++) {
            assertEquals(b.symbols.symbols.get(i), b2.symbols.symbols.get(i));
            assertFalse(b.symbols.symbols.get(i) == b2.symbols.symbols.get(i));
        }

        // token signature will never change so the deep copy is not required
        assertTrue(b.container.get().signature == b2.container.get().signature);
    }
}
