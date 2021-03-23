package com.clevercloud.biscuit.crypto;

import cafe.cryptography.curve25519.Constants;
import cafe.cryptography.curve25519.RistrettoElement;
import cafe.cryptography.curve25519.Scalar;
import com.clevercloud.biscuit.token.builder.Utils;

import java.security.SecureRandom;

/**
 * Private and public key
 */
public final class KeyPair {
    public final Scalar private_key;
    public final RistrettoElement public_key;

    public KeyPair(final SecureRandom rng) {
        byte[] b = new byte[64];
        rng.nextBytes(b);

        this.private_key = Scalar.fromBytesModOrderWide(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public byte[] toBytes() {
        return this.private_key.toByteArray();
    }

    public KeyPair(byte[] b) {
        this.private_key = Scalar.fromBytesModOrderWide(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public KeyPair(String hex) {
        byte[] b = Utils.hexStringToByteArray(hex);
        this.private_key = Scalar.fromBytesModOrder(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public PublicKey public_key() {
        return new PublicKey(this.public_key);
    }
}
