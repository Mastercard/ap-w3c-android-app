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
package com.brillio.testapp.constant;

public class SecurityConstant {
    public static final String ENCRYPTION_KEY = "12345678901234561234567890123456";
    public static final String CHARSET_NAME = "UTF-8";
    public static final String ENCRYPTION_TYPE = "AES";
    public static final String ENCRYPTION_MODE = "AES/CBC/PKCS5PADDING";

    private SecurityConstant() {
    }
}
