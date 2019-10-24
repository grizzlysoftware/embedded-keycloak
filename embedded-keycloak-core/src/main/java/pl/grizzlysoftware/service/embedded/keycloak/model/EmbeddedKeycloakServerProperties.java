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

package pl.grizzlysoftware.service.embedded.keycloak.model;

/**
 * Created by Bartosz Pawlowski
 */
public class EmbeddedKeycloakServerProperties {
    public static final String SERVER_CONTEXT_PATH = "keycloak.embedded.server.context-path";
    public static final String SERVER_CONFIGURATION_PATH = "keycloak.embedded.server.configuration.path";
    public static final String DEFAULT_REALM = "keycloak.embedded.realm.default.name";
    public static final String REALM_CONFIGURATION_PATH = "keycloak.embedded.realm.configuration.path";
    public static final String ADMIN_USERNAME = "keycloak.embedded.security.admin.username";
    public static final String ADMIN_PASSWORD = "keycloak.embedded.security.admin.password";
    public static final String DATASOURCE_URL = "keycloak.embedded.datasource.url";
    public static final String DATASOURCE_USERNAME = "keycloak.embedded.datasource.username";
    public static final String DATASOURCE_PASSWORD = "keycloak.embedded.datasource.password";

    public final String serverContextPath;
    public final String serverConfigPath;
    public final String realmConfigPath;
    public final String defaultRealm;
    public final String adminUser;
    public final String adminPassword;
    public final String datasourceUrl;
    public final String datasourceUsername;
    public final String datasourcePassword;

    public EmbeddedKeycloakServerProperties() {
        serverContextPath = "/auth";
        serverConfigPath = "keycloak-server.conf";
        defaultRealm = "master";
        realmConfigPath = "keycloak-realm-conf.json";
        adminUser = "admin";
        adminPassword = "admin";
        datasourceUrl = "jdbc:h2:./data/keycloak;DB_CLOSE_ON_EXIT=FALSE";
        datasourceUsername = "sa";
        datasourcePassword = null;
    }

    public EmbeddedKeycloakServerProperties(String serverContextPath, String serverConfigPath, String realmConfigPath, String defaultRealm, String adminUser, String adminPassword, String datasourceUrl, String datasourceUsername, String datasourcePassword) {
        this.serverContextPath = serverContextPath;
        this.serverConfigPath = serverConfigPath;
        this.realmConfigPath = realmConfigPath;
        this.defaultRealm = defaultRealm;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
    }
}
