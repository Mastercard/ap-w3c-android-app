/*
 * Copyright (c) 2021 Mastercard
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.brillio.testapp.util;

import android.util.Base64;

import com.brillio.testapp.constant.SecurityConstant;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import com.brillio.testapp.util.LogUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityService {

    private static String strIVSpec = "1234567812345678";
    private static String strprivateKey = "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";

    private static PublicKey getPublicKeyFromCertificate(String merchantCertificate) {
        PublicKey publicKey = null;
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        try {
            if (merchantCertificate != null && !merchantCertificate.trim().isEmpty()) {
                merchantCertificate = merchantCertificate.replace("-----BEGIN CERTIFICATE-----\n", "")
                        .replace("-----END CERTIFICATE-----", "")
                        .replace("\n", "");
                byte[] certificateData = android.util.Base64.decode(merchantCertificate.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
                cf = CertificateFactory.getInstance("X509");
                certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
                publicKey = certificate.getPublicKey();
            }
        } catch (CertificateException | UnsupportedEncodingException e) {
            LogUtil.error("SecurityService", "exception in getPublicKeyFromCertificate :" + e.getMessage());
        }
        return publicKey;
    }

    public static String encypt(String merchantCertificate, String paymentResponse) {
        String sessionKey = generateRandomString();
        String encSessionKey = null;
        String encryptedResponse = "";
        String data = null;
        try {
            encryptedResponse = aesEncrypt(paymentResponse, sessionKey).replaceAll("[\\n\\t ]", "");
            encSessionKey = rsaEncrypt(sessionKey, merchantCertificate).replaceAll("[\\n\\t ]", "");
            data = encryptedResponse + ":" + encSessionKey + ":" + strIVSpec;

        } catch (Exception e) {
            LogUtil.error("SecurityService", "exception in encypt :" + e.getMessage());
        }
        return Base64.encodeToString(data.getBytes(), Base64.DEFAULT).replaceAll("[\\n\\t ]", "");
    }

    private static String generateRandomString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String aesEncrypt(String payload, String key) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException, InvalidAlgorithmParameterException {

        IvParameterSpec iv = new IvParameterSpec(strIVSpec.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(payload.getBytes());
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    private static String rsaEncrypt(String data, String merchantCertificate) {
        byte[] encryptedByte = null;
        String encryptedString = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKeyFromCertificate(merchantCertificate));
            encryptedByte = cipher.doFinal(data.getBytes());
            encryptedString = Base64.encodeToString(encryptedByte, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            LogUtil.error("SecurityService", "exception in rsaEncrypt :" + e.getMessage());
        }
        return encryptedString;
    }


    private static PrivateKey getPrivateKey() throws UnsupportedEncodingException {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(android.util.Base64.decode(strprivateKey.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            LogUtil.error("SecurityService", "NoSuchAlgorithmException in getPrivateKey :" + e.getMessage());
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            LogUtil.error("SecurityService", "InvalidKeySpecException in getPrivateKey :" + e.getMessage());
        }
        return privateKey;
    }

    public static String aesDecrypt(String encryptedPayload, String key, String ivString) {

        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
        byte[] ency = Base64.decode(encryptedPayload, Base64.DEFAULT);
        Cipher cipher;
        byte[] original = null;
        try {
            IvParameterSpec iv = new IvParameterSpec(ivString.getBytes("UTF-8"));
            cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            original = cipher.doFinal(ency);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("SecurityService", "UnsupportedEncodingException in aesDecrypt :" + e.getMessage());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            LogUtil.error("SecurityService", "exception in aesDecrypt :" + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            LogUtil.error("SecurityService", "InvalidAlgorithmParameterException in aesDecrypt :" + e.getMessage());
        }
        return new String(original);
    }


    public static String rsaDecrypt(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        try {
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
        } catch (Exception e) {
            LogUtil.error("SecurityService", "exception in rsaDecrypt :" + e.getMessage());
        }
        return new String(cipher.doFinal(android.util.Base64.decode(data.getBytes(), Base64.DEFAULT)));
    }


    public static String decryptData(String data) {
        byte[] decodeData = android.util.Base64.decode(data.getBytes(), Base64.DEFAULT);
        try {
            data = new String(decodeData, SecurityConstant.CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("SecurityService", "UnsupportedEncodingException in decryptData :" + e.getMessage());
        }
        String[] dataStrings = data.split(Pattern.quote(":"));
        String encryptedPayload = dataStrings[0].toString();
        String encryptedSymetricKey = dataStrings[1].toString();
        String ivSpec = dataStrings[2].toString();
        String decryptedSymetricKey = null;
        try {
            decryptedSymetricKey = rsaDecrypt(encryptedSymetricKey);
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException
                | IllegalBlockSizeException e) {
            LogUtil.error("SecurityService", "exception in decryptData :" + e.getMessage());
        }
        return aesDecrypt(encryptedPayload, decryptedSymetricKey, ivSpec);
    }

}
