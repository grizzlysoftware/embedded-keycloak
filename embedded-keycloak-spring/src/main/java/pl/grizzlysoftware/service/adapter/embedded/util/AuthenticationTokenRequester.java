/*
 * Copyright 2019 Grizzly Software, https://grizzlysoftware.pl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package pl.grizzlysoftware.service.adapter.embedded.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.function.Function;

public class AuthenticationTokenRequester {
    private RestTemplate client;
    private Function<HttpEntity<String>, String> tokenExtractor;

    public AuthenticationTokenRequester(RestTemplate client) {
        this(client, null);
    }

    public AuthenticationTokenRequester(RestTemplate client, Function<HttpEntity<String>, String> tokenExtractor) {
        this.client = client;
        if (tokenExtractor == null) {
            this.tokenExtractor = defaultTokenExtractor();
        } else {
            this.tokenExtractor = tokenExtractor;
        }
    }

    static Function<HttpEntity<String>, String> defaultTokenExtractor() {
        ObjectMapper m = new ObjectMapper();
        return e -> {
            try {
                JsonNode json = m.readTree(e.getBody());
                JsonNode accessTokenNode = json.get("access_token");
                if (accessTokenNode != null) {
                    return accessTokenNode.asText();
                }
                return null;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public String get(String url, String clientId, String username, String password) {
        return get(url, clientId, username, password, "password");
    }

    public String get(String url, String clientId, String username, String password, String grantType) {
        LinkedMultiValueMap params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("username", username);
        params.add("password", password);
        params.add("grant_type", grantType);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        HttpEntity entity = new HttpEntity<>(params, headers);

        ResponseEntity response = client.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            String message = String.format("Unable to authenticate in keycloak:\nUrl: %s\nCredentials: %s:%s\nHeaders: %s\nStatus code:%s\nBody: %s",
                    url, username, password, response.getHeaders().toString(), response.getStatusCodeValue(), response.getBody());
            throw new RuntimeException(message);
        }

        if (tokenExtractor == null) {
            tokenExtractor = defaultTokenExtractor();
        }

        return tokenExtractor.apply(response);
    }
}
