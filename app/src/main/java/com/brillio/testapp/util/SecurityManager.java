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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is designed to handle all the security related operations
 */
public class SecurityManager {

    private static final String LOGTAG = SecurityManager.class.getSimpleName();

    private SecurityManager() {

    }

    public static String aesEncrypt(String value, String sessionKey, String iv) {
        LogUtil.debug(LOGTAG, "aesEncrypt , initVector :" + iv + " , key :" + sessionKey);
        if (!value.isEmpty()) {
            try {
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(SecurityConstant.CHARSET_NAME));
                SecretKeySpec keySpec = new SecretKeySpec(sessionKey.getBytes(SecurityConstant.CHARSET_NAME), SecurityConstant.ENCRYPTION_TYPE);
                Cipher cipher = Cipher.getInstance(SecurityConstant.ENCRYPTION_MODE);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
                byte[] encrypted = cipher.doFinal(value.getBytes(SecurityConstant.CHARSET_NAME));

                return CommonUtil.getHexValue(encrypted);
            } catch (Exception e) {
                LogUtil.error(LOGTAG, "exe while AES encryption :" + e.getMessage());
            }
        }
        return null;
    }

    public static String aesDecrypt(String encrypted, String initVector, String key) throws Exception {
        LogUtil.debug(LOGTAG, "aesDecrypt , initVector :" + initVector + " , key :" + key);

        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        byte[] original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT));

        return new String(original);
    }

    public static String rsaEncrypt(String plainText, String publicKey) {
        String encryptedData = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(publicKey));
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            encryptedData = Base64.encodeToString(cipherText, Base64.DEFAULT);
        } catch (Exception e) {
            LogUtil.error(LOGTAG, "rsaEncrypt , exe while rsa encryption :" + e.getMessage());
        }
        return encryptedData;
    }

    private static PublicKey generateKey(String publicKeyString) {
        PublicKey publicKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] yourKey = Base64.decode(publicKeyString.getBytes(), Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(yourKey);
            publicKey = keyFactory.generatePublic(spec);
            LogUtil.debug(LOGTAG, "generateKey , Generated public key :" + publicKey);
        } catch (Exception e) {
            LogUtil.error(LOGTAG, "generateKey , exe while rsa ea„ncryption :" + e.getMessage());
        }

        return publicKey;
    }


    public static String getPublicKeyFromCertificate(String certificateString) {
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        String publicKey = null;
        try {
            if (certificateString != null && !certificateString.trim().isEmpty()) {
                certificateString = certificateString.replace("-----BEGIN CERTIFICATE-----\n", "")
                        .replace("-----END CERTIFICATE-----", "");
                byte[] certificateData = Base64.decode(certificateString, Base64.DEFAULT);
                cf = CertificateFactory.getInstance("X509");
                certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
                publicKey = Base64.encodeToString(certificate.getPublicKey().getEncoded(), 0);
            }
        } catch (CertificateException e) {
            LogUtil.error(LOGTAG, "generateKey , exe while rsa encryption :" + e.getMessage());
        }
        return publicKey;
    }
}
