package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import io.vavr.control.Either;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Block.*;

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

        Biscuit b = Biscuit.make(rng, root, authority_builder.build()).get();

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data, root.public_key).get();

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);

        Block builder = deser.create_block();
        builder.add_caveat(rule(
                "caveat1",
                Arrays.asList(var(0)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var(0))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var(0), s("read")))
                )
        ));

        Biscuit b2 = deser.append(rng, keypair2, builder.build()).get();

        System.out.println(b2.print());

        System.out.println("serializing the second token");

        byte[] data2 = b2.serialize().get();

        System.out.print("data len: ");
        System.out.println(data2.length);
        System.out.println(hex(data2));

        System.out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.from_bytes(data2, root.public_key).get();

        System.out.println(deser2.print());

        // THIRD BLOCK
        System.out.println("preparing the third block");

        KeyPair keypair3 = new KeyPair(rng);

        Block builder3 = deser2.create_block();
        builder3.add_caveat(rule(
                "caveat2",
                Arrays.asList(s("file1")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), s("file1")))
                )
        ));

        Biscuit b3 = deser2.append(rng, keypair3, builder3.build()).get();

        System.out.println(b3.print());

        System.out.println("serializing the third token");

        byte[] data3 = b3.serialize().get();

        System.out.print("data len: ");
        System.out.println(data3.length);
        System.out.println(hex(data3));

        System.out.println("deserializing the third token");
        Biscuit final_token = Biscuit.from_bytes(data3, root.public_key).get();

        System.out.println(final_token.print());

        // check
        System.out.println("will check the token for resource=file1 and operation=read");

        SymbolTable check_symbols = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts = Arrays.asList(
                fact("resource", Arrays.asList(s("ambient"), s("file1"))).convert(check_symbols),
                fact("operation", Arrays.asList(s("ambient"), s("read"))).convert(check_symbols)
        );

        Either<LogicError, Void> res = final_token.check(check_symbols, ambient_facts, new ArrayList<>(), new ArrayList<>());

        Assert.assertTrue(res.isRight());

        System.out.println("will check the token for resource=file2 and operation=write");

        SymbolTable check_symbols2 = new SymbolTable(final_token.symbols);
        List<Fact> ambient_facts2 = Arrays.asList(
                fact("resource", Arrays.asList(s("ambient"), s("file2"))).convert(check_symbols2),
                fact("operation", Arrays.asList(s("ambient"), s("write"))).convert(check_symbols2)
        );

        Either<LogicError, Void> res2 = final_token.check(check_symbols2, ambient_facts2, new ArrayList<>(), new ArrayList<>());
        Assert.assertTrue(res2.isLeft());
        System.out.println(res2.getLeft());

        Assert.assertEquals(
                new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(0, 0, "caveat1(0?) <- resource(#ambient, 0?) && operation(#ambient, #read) && right(#authority, 0?, #read) | "),
                        new FailedCaveat().new FailedBlock(1, 0, "caveat2(#file1) <- resource(#ambient, #file1) | ")
                )),
                res2.getLeft());
    }

    public void testFolders() {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_block = new Block(0, symbols);
        authority_block.add_right("/folder1/file1", "read");
        authority_block.add_right("/folder1/file1", "write");
        authority_block.add_right("/folder1/file2", "read");
        authority_block.add_right("/folder1/file2", "write");
        authority_block.add_right("/folder2/file3", "read");

        Biscuit b = Biscuit.make(rng, root, authority_block.build()).get();

        System.out.println(b.print());

        Block block2 = b.create_block();
        block2.resource_prefix("/folder1/");
        block2.check_right("read");

        KeyPair keypair2 = new KeyPair(rng);
        Biscuit b2 = b.append(rng, keypair2, block2.build()).get();

        Verifier v1 = new Verifier();
        v1.add_resource("/folder1/file1");
        v1.add_operation("read");
        Either<LogicError, Void> res = v1.verify(b2);
        Assert.assertTrue(res.isRight());

        Verifier v2 = new Verifier();
        v2.add_resource("/folder2/file3");
        v2.add_operation("read");
        res = v2.verify(b2);
        Assert.assertTrue(res.isLeft());

        Verifier v3 = new Verifier();
        v3.add_resource("/folder2/file1");
        v3.add_operation("write");
        res = v3.verify(b2);

        LogicError e = res.getLeft();
        Assert.assertTrue(res.isLeft());

        for(FailedCaveat f: e.failed_caveats().get()) {
            System.out.println(f.toString());
        }
        Assert.assertEquals(
                new LogicError().new FailedCaveats(Arrays.asList(
                        new FailedCaveat().new FailedBlock(0, 0, "prefix(0?) <- resource(#ambient, 0?) | 0? matches /folder1/*"),
                        new FailedCaveat().new FailedBlock(0, 1, "check_right(#read) <- resource(#ambient, 0?) && operation(#ambient, #read) && right(#authority, 0?, #read) | ")
                )),
                e);
    }
}