/**
 *    Original work by Ola Aronsson 2020
 *    Courtesy of nollettnoll AB &copy; 2012 - 2020
 *
 *    Licensed under the Creative Commons Attribution 4.0 International (the "License")
 *    you may not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *                https://creativecommons.org/licenses/by/4.0/
 *
 *    The software is provided “as is”, without warranty of any kind, express or
 *    implied, including but not limited to the warranties of merchantability,
 *    fitness for a particular purpose and noninfringement. In no event shall the
 *    authors or copyright holders be liable for any claim, damages or other liability,
 *    whether in an action of contract, tort or otherwise, arising from, out of or
 *    in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils.encr;

import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Aes implements SimpleCryptOperations {

    private static final int TAG_LENGTH_BIT_128 = 128;
    private static final int TAG_LENGTH_BIT_192 = 192;
    private static final int TAG_LENGTH_BIT_256 = 256;
    private static Aes INSTANCE;
    private static final String AES = "AES";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private final SecureRandom secureRandom;
    private final Cipher cipher;
    private static final int MIN_KEY_LENGTH = 16;
    private static final int MAX_KEY_LENGTH = 32;

    private Aes() throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.secureRandom = new SecureRandom();
        this.cipher = Cipher.getInstance(ALGORITHM);
    }

    public static Aes getSilent() {
        try {
            return getInstance();
        } catch (NoSuchAlgorithmException e) {
            throw new CatchitSupervisionRuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new CatchitSupervisionRuntimeException(e);
        }
    }

    public static synchronized Aes getInstance() throws NoSuchAlgorithmException, NoSuchPaddingException {
        if (INSTANCE == null) {
            INSTANCE = new Aes();
        }
        return INSTANCE;
    }

    public void chkKey(byte[] secret) {
        if (!isKeyLengthAccepted(secret.length)) {
            throw new IllegalArgumentException(String.format("key length must be between %d and %d", MIN_KEY_LENGTH, MAX_KEY_LENGTH));
        }
    }

    public boolean isSuitableKey(String secret) {
        return isKeyLengthAccepted(secret.length());
    }

    private boolean isKeyLengthAccepted(int keyLength) {
        return keyLength >= MIN_KEY_LENGTH && keyLength <= MAX_KEY_LENGTH;
    }

    @Override
    public byte[] encrypt(byte[] rawEncryptionKey, byte[] rawData) {

        chkKey(rawEncryptionKey);

        byte[] iv = null;
        byte[] encrypted = null;
        try {
            iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rawEncryptionKey, AES), new GCMParameterSpec(TAG_LENGTH_BIT_128, iv));

            encrypted = cipher.doFinal(rawData);

            return wrap(iv, encrypted).array();
        } catch (Exception e) {
            throw new CatchitSupervisionRuntimeException("could not encrypt", e);
        } finally {
            wipeContent(iv, encrypted);
        }
    }

    @Override
    public byte[] decrypt(byte[] rawEncryptionKey, byte[] encryptedData) {
        byte[] encrypted = null;
        byte[] iv = null;
        try {

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            int ivLength = byteBuffer.get();
            iv = new byte[ivLength];
            byteBuffer.get(iv);
            encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rawEncryptionKey, AES), new GCMParameterSpec(TAG_LENGTH_BIT_128, iv));

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new CatchitSupervisionRuntimeException("could not decrypt", e);
        } finally {
            wipeContent(iv, encrypted);
        }
    }


    private void wipeContent(byte[] iv, byte[] encrypted) {
        if (encrypted != null) {
            encrypted = new byte[]{};
        }
        if (iv != null) {
            iv = new byte[]{};
        }
    }

    private ByteBuffer wrap(byte[] iv, byte[] encrypted) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        byteBuffer.put((byte) iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(encrypted);
        return byteBuffer;
    }
}
