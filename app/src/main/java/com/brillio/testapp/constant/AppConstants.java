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

public class AppConstants {
    private AppConstants() {
    }

    public static final String BUNDLE_KEY_EVENT_TYPE = "event_type";
    public static final String BUNDLE_KEY_EVENT_VALUE = "event_value";
    public static final String BUNDLE_KEY_BROWSER_NAME = "browser_name";
    public static final String ACTION_W3C_PAY = "org.chromium.intent.action.PAY";
    public static final String REGISTRATION_STATUS = "registrationStatus";
    public static final String UNREGISTRATION_STATUS = "unregistrationStatus";
    public static final String PAYMENT = "payment";
    public static final String W3C_PAYMENT = "w3cpayment";
    public static final String TOTAL_AMOUNT = "totalAmount";
    public static final String BROWSER = "browser";
    public static final String PAYMENT_REQUEST_OBJECT = "orderObject";
    public static final String SIGNED_TOKEN = "signedtoken";
    public static final String HASH_DATA = "hashData";
}
