package pl.grizzlysoftware.service.adapter.embedded.keycloak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import pl.grizzlysoftware.service.adapter.embedded.util.AuthenticationTokenRequester;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

/**
 * Created by Bartosz Pawlowski
 */
public abstract class KeycloakSecurityTestHelper {
    private static final String APP_URL_TEMPLATE = "http://localhost:%s/%s";
    private static final String KEYCLOAK_URL_TEMPLATE = "%s/%s/realms/%s/protocol/%s/token";

    @LocalServerPort
    protected int localServerPort;

    @Autowired
    protected ServletContext servletContext;

    @Autowired
    protected TestRestTemplate client;

    protected AuthenticationTokenRequester authTokenRequester;

    protected String clientId;
    protected String realm;
    protected String protocol;

    public KeycloakSecurityTestHelper() {
        initialize();
    }

    void initialize() {

    }

    @PostConstruct
    void postConstruct() {
        this.authTokenRequester = new AuthenticationTokenRequester(client.getRestTemplate());
    }

    protected String apiUrl() {
        return String.format(APP_URL_TEMPLATE, localServerPort, servletContext.getContextPath());
    }

    protected String authUrl() {
        return String.format(KEYCLOAK_URL_TEMPLATE, apiUrl(), "embedded-keycloak", realm, protocol);
    }

    protected String getToken(String username, String password) {
        return getToken(clientId, username, password);
    }

    protected String getToken(String clientId, String username, String password) {
        String url = authUrl();
        return authTokenRequester.get(url, clientId, username, password);
    }

    protected ResponseEntity<String> httpGet(String endpoint) {
        return invokeGet(endpoint, null);
    }

    protected ResponseEntity<String> httpGet(String endpoint, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        bearer(headers, authToken);
        return invokeGet(endpoint, headers);
    }

    protected ResponseEntity<String> httpPost(String endpoint, Object body, String contentType, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        bearer(headers, authToken);
        return invokePost(endpoint, body, headers);
    }

    protected ResponseEntity<String> httpDelete(String endpoint, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        bearer(headers, authToken);
        return invokeDelete(endpoint, headers);
    }

    private ResponseEntity<String> invokeGet(String endpoint, HttpHeaders headers) {
        return invoke(endpoint, HttpMethod.GET, null, headers);
    }

    private ResponseEntity<String> invokePost(String endpoint, Object body, HttpHeaders headers) {
        return invoke(endpoint, HttpMethod.GET, body, headers);
    }

    private ResponseEntity<String> invokeDelete(String endpoint, HttpHeaders headers) {
        return invoke(endpoint, HttpMethod.GET, null, headers);
    }

    private ResponseEntity<String> invoke(String endpoint, HttpMethod method, Object body, HttpHeaders headers) {
        String url = apiUrl() + endpoint;
        HttpEntity entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = client.exchange(
                url,
                method,
                entity,
                String.class);
        return response;
    }

    private static void bearer(HttpHeaders headers, String token) {
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
    }
}