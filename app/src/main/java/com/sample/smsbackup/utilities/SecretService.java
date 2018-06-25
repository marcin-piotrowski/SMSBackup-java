package com.sample.smsbackup.utilities;

import android.util.Base64;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecretService {

    private static final String ENCRYPTION_METHOD = "AES";

    public static String encrypt(String textToEncrypt,GoogleSignInAccount googleSignInAccount) throws Exception {
        try {
            byte[] idInBytes = googleSignInAccount.getId().getBytes("UTF-8");
            byte[] secret = fitBytes(idInBytes);
            SecretKeySpec secretKey = new SecretKeySpec(secret, ENCRYPTION_METHOD);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_METHOD);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] cipherText = cipher.doFinal(textToEncrypt.getBytes());
            byte[] base64encodedSecretData = Base64.encode(cipherText, Base64.DEFAULT);

            return new String(base64encodedSecretData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String decrypt(String textToDecrypt, GoogleSignInAccount googleSignInAccount) throws Exception {
        try {
            byte[] idInBytes = googleSignInAccount.getId().getBytes("UTF-8");
            byte[] secret = fitBytes(idInBytes);
            SecretKeySpec secretKey = new SecretKeySpec(secret, ENCRYPTION_METHOD);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_METHOD);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] plainText = cipher.doFinal(Base64.decode(textToDecrypt.getBytes(), Base64.DEFAULT));

            return new String(plainText);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static byte[] fitBytes(byte[] idInBytes){
        byte[] secret = new byte[16];
        if(idInBytes.length > 16){
            int i = 0;
            while(i<16){
                secret[i] = idInBytes[i];
                i++;
            }
        } else {
            int i = 0;
            while(i<=idInBytes.length){
                secret[i] = idInBytes[i];
                i++;
            }
            while(i<16){
                secret[i] = (byte)1;
                i++;
            }
        }
        return secret;
    }
}
