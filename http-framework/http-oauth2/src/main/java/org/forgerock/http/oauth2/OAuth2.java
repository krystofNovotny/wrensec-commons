/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.oauth2;

/** OAuth2 utility class. */
public final class OAuth2 {

    private static final String BEARER_TOKEN_KEY = "BEARER";

    /**
     * Extracts the bearer token from the request's authorization header.
     * <p>
     * Expected ABNF format (as per RFC 6750):
     * <pre>
     *     {@code
     *     b64token    = 1*( ALPHA / DIGIT / "-" / "." / "_" / "~" / "+" / "/" ) *"="
     *     credentials = "Bearer" 1*SP b64token
     *     }
     * </pre>
     *
     * @param authorizationHeader
     *         The authorization header from the request.
     * @return The access token, or {@code null} if the access token was not present or was not using Bearer
     * authorization.
     */
    public static String getBearerAccessToken(final String authorizationHeader) {

        if (authorizationHeader == null) {
            return null;
        }
        String authorization = authorizationHeader.trim();
        final int index = authorization.indexOf(' ');
        if (index <= 0) {
            return null;
        }

        final String tokenType = authorization.substring(0, index);

        if (BEARER_TOKEN_KEY.equalsIgnoreCase(tokenType)) {
            return authorization.substring(index + 1);
        }

        return null;
    }

    private OAuth2() {
        /* utility class */
    }
}
