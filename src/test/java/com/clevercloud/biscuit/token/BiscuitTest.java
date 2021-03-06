package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Block;
import io.vavr.control.Either;
import io.vavr.control.Option;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static com.clevercloud.biscuit.token.builder.Utils.*;

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
        builder.add_check(check(rule(
                "caveat1",
                Arrays.asList(var("resource")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var("resource"))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder.build()).get();

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
        builder3.add_check(check(rule(
                "caveat2",
                Arrays.asList(s("file1")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), s("file1")))
                )
        )));

        Biscuit b3 = deser2.attenuate(rng, keypair3, builder3.build()).get();

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
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if resource(#ambient, $resource), operation(#ambient, #read), right(#authority, $resource, #read)"),
                        new FailedCheck.FailedBlock(2, 0, "check if resource(#ambient, #file1)")
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
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build()).get();

        Verifier v1 = b2.verify(root.public_key()).get();
        v1.add_resource("/folder1/file1");
        v1.add_operation("read");
        v1.allow();
        Either<Error, Long> res = v1.verify();
        Assert.assertTrue(res.isRight());

        Verifier v2 = b2.verify(root.public_key()).get();
        v2.add_resource("/folder2/file3");
        v2.add_operation("read");
        v2.allow();
        res = v2.verify();
        Assert.assertTrue(res.isLeft());

        Verifier v3 = b2.verify(root.public_key()).get();
        v3.add_resource("/folder2/file1");
        v3.add_operation("write");
        v3.allow();
        res = v3.verify();

        Error e = res.getLeft();
        Assert.assertTrue(res.isLeft());

        System.out.println(v3.print_world());
        for (FailedCheck f : e.failed_checks().get()) {
            System.out.println(f.toString());
        }
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if resource(#ambient, $resource), $resource.starts_with(\"/folder1/\")"),
                        new FailedCheck.FailedBlock(1, 1, "check if resource(#ambient, $resource), operation(#ambient, #read), right(#authority, $resource, #read)")
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
        builder.add_check(check(rule(
                "caveat1",
                Arrays.asList(var("resource")),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var("resource"))),
                        pred("operation", Arrays.asList(s("ambient"), s("read"))),
                        pred("right", Arrays.asList(s("authority"), var("resource"), s("read")))
                )
        )));

        Biscuit b2 = deser.attenuate(rng, keypair2, builder.build()).get();

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
                new Error.FormatError.Signature.SealedSignature(),
                e);

        System.out.println("deserializing the sealed token with a valid key");
        Biscuit deser2 = Biscuit.from_sealed(sealed, "testkey".getBytes()).get();
        System.out.println(deser2.print());

        System.out.println("trying to attenuate to a sealed token");
        Block builder2 = deser2.create_block();
        Error e2 = deser2.attenuate(rng, keypair2, builder.build()).getLeft();

        Verifier v = deser2.verify_sealed().get();
        System.out.println(v.print_world());
    }

    public void testMultipleAttenuation() {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);
        Date date = Date.from(Instant.now());
        authority_builder.add_fact(fact("revocation_id", Arrays.asList(date(date))));

        Biscuit biscuit = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build()).get();

        Block builder = biscuit.create_block();
        builder.add_fact(fact(
                "right",
                Arrays.asList(s("topic"), s("tenant"), s("namespace"), s("topic"), s("produce"))
        ));

        String attenuatedB64 = biscuit.attenuate(rng, new KeyPair(rng), builder.build()).get().serialize_b64().get();

        System.out.println("attenuated: " + attenuatedB64);

        Biscuit.from_b64(attenuatedB64).get();
        String attenuated2B64 = biscuit.attenuate(rng, new KeyPair(rng), builder.build()).get().serialize_b64().get();

        System.out.println("attenuated2: " + attenuated2B64);
        Biscuit.from_b64(attenuated2B64).get();
    }

    public void testGetRevocationIds() {
        SecureRandom rng = new SecureRandom();
        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block authority_builder = new Block(0, symbols);

        UUID uuid1 = UUID.fromString("0b6d033d-83da-437f-a078-1a44890018bc");
        authority_builder.add_fact(fact("revocation_id", Arrays.asList(string(uuid1.toString()))));

        Biscuit biscuit = Biscuit.make(rng, root, Biscuit.default_symbol_table(), authority_builder.build()).get();

        Block builder = biscuit.create_block();
        builder.add_fact(fact(
                "right",
                Arrays.asList(s("topic"), s("tenant"), s("namespace"), s("topic"), s("produce"))
        ));
        UUID uuid2 = UUID.fromString("46a103de-ee65-4d04-936b-9111eac7dd3b");
        builder.add_fact(fact("revocation_id", Arrays.asList(string(uuid2.toString()))));

        String attenuatedB64 = biscuit.attenuate(rng, new KeyPair(rng), builder.build()).get().serialize_b64().get();
        Biscuit b = Biscuit.from_b64(attenuatedB64).get();

        Verifier v1 = b.verify(root.public_key()).get();
        List<UUID> revokedIds = v1.get_revocation_ids().get().stream().map(s -> UUID.fromString(s)).collect(Collectors.toList());
        org.junit.Assert.assertTrue(revokedIds.contains(uuid1));
        org.junit.Assert.assertTrue(revokedIds.contains(uuid2));
    }

    public void testReset() {
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
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build()).get();

        Verifier v1 = b2.verify(root.public_key()).get();
        v1.allow();

        Verifier v2 = v1.clone();

        v2.add_resource("/folder1/file1");
        v2.add_operation("read");


        Either<Error, Long> res = v2.verify();
        Assert.assertTrue(res.isRight());

        Verifier v3 = v1.clone();

        v3.add_resource("/folder2/file3");
        v3.add_operation("read");

        res = v3.verify();
        System.out.println(v3.print_world());

        Assert.assertTrue(res.isLeft());

        Verifier v4 = v1.clone();

        v4.add_resource("/folder2/file1");
        v4.add_operation("write");

        res = v4.verify();

        Error e = res.getLeft();
        Assert.assertTrue(res.isLeft());

        System.out.println(v4.print_world());
        for (FailedCheck f : e.failed_checks().get()) {
            System.out.println(f.toString());
        }
        Assert.assertEquals(
                new Error.FailedLogic(new LogicError.FailedChecks(Arrays.asList(
                        new FailedCheck.FailedBlock(1, 0, "check if resource(#ambient, $resource), $resource.starts_with(\"/folder1/\")"),
                        new FailedCheck.FailedBlock(1, 1, "check if resource(#ambient, $resource), operation(#ambient, #read), right(#authority, $resource, #read)")
                ))),
                e);
    }

    public void testEmptyVerifier() {
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
        Biscuit b2 = b.attenuate(rng, keypair2, block2.build()).get();

        Verifier v1 = new Verifier();
        v1.allow();

        Either<Error, Long> res = v1.verify();
        Assert.assertTrue(res.isRight());

        v1.add_token(b2, Option.some(root.public_key())).get();

        v1.add_resource("/folder2/file1");
        v1.add_operation("write");

        res = v1.verify();

        Error e = res.getLeft();
        Assert.assertTrue(res.isLeft());
    }
}
