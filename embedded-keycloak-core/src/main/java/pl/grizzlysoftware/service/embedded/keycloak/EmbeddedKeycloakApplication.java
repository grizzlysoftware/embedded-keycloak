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

package pl.grizzlysoftware.service.embedded.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.exportimport.Strategy;
import org.keycloak.exportimport.singlefile.SingleFileImportProvider;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Created by Bartosz Pawlowski
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {
    public static final String SERVER_CONTEXT_PATH = "keycloak.embedded.server.context-path";
//    public static final String SERVER_CONFIGURATION_PATH = "keycloak.embedded.server.configuration.path";
//    public static final String DEFAULT_REALM = "keycloak.embedded.realm.default.name";
    public static final String REALM_CONFIGURATION_PATH = "keycloak.embedded.realm.configuration.path";
    public static final String REALM_CONFIGURATION_STRATEGY = "keycloak.embedded.realm.configuration.strategy";
    public static final String ADMIN_USERNAME = "keycloak.embedded.security.admin.username";
    public static final String ADMIN_PASSWORD = "keycloak.embedded.security.admin.password";

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

//    private String defaultRealm;
    private String adminUser;
    private String adminPassword;
//    private String serverConfigPath;
    private String realmConfigPath;
    private String realmConfigStrategy;

    public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        super(augmentToRedirectContextPath(context), dispatcher);

        //        serverConfigPath = context.getInitParameter(SERVER_CONFIGURATION_PATH);
        realmConfigPath = context.getInitParameter(REALM_CONFIGURATION_PATH);
        realmConfigStrategy = context.getInitParameter(REALM_CONFIGURATION_STRATEGY);

//        defaultRealm = context.getInitParameter(DEFAULT_REALM);
        adminUser = context.getInitParameter(ADMIN_USERNAME);
        adminPassword = context.getInitParameter(ADMIN_PASSWORD);

        createAdminUser();
        loadKeycloakRealmConfiguration(realmConfigPath);
    }

    protected Strategy resolveRealmConfigurationStrategy(String strategyName) {
        try {
            Strategy strategy = Strategy.valueOf(strategyName);
            return strategy;
        } catch (Exception e) {
            LOG.warn("Unable to resolve realm configuration strategy: {}, applying default strategy: {}", strategyName, Strategy.OVERWRITE_EXISTING.name());
            return Strategy.OVERWRITE_EXISTING;
        }
    }

    protected void loadKeycloakRealmConfiguration(String path) {
        try {
            LOG.info("Loading keycloak realm configuration from resource: {}", path);
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                LOG.error("Unable to find keycloak realm  in file: {}", path);
                return;
            }

            //constructor parameter is fake and unused as checkRealmReps uses preinitialized stream resource from above
            SingleFileImportProvider importProvider = new SingleFileImportProvider(new File(path)) {
                @Override
                protected void checkRealmReps() throws IOException {
                    realmReps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, stream);
                }
            };
            importProvider.importModel(sessionFactory, resolveRealmConfigurationStrategy(realmConfigStrategy));
            LOG.error("Keycloak realm configuration loaded successfully");
        } catch (Exception e) {
            LOG.error("Unable to load keycloak realm configuration", e);
        }
    }

    protected void createAdminUser() {
        KeycloakSession session = getSessionFactory().create();

        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

        try {
            session.getTransactionManager().begin();
            applianceBootstrap.createMasterRealmUser(adminUser, adminPassword);
            session.getTransactionManager().commit();
        } catch (Exception e) {
            LOG.warn("Couldn't create keycloak master admin user: {}", e.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }

    private static ServletContext augmentToRedirectContextPath(ServletContext context) {

        ClassLoader classLoader = context.getClassLoader();
        Class[] interfaces = {ServletContext.class};

        InvocationHandler invocationHandler = (proxy, method, args) -> {

            if ("getContextPath".equals(method.getName())) {
                String kcContextPath = context.getInitParameter(SERVER_CONTEXT_PATH);
                String appContextPath = context.getContextPath();

                return appContextPath + kcContextPath;
            }

            LOG.info("Invoke on ServletContext: method=[{}] args=[{}]", method.getName(), Arrays.toString(args));

            return method.invoke(context, args);
        };

        return ServletContext.class.cast(Proxy.newProxyInstance(classLoader, interfaces, invocationHandler));
    }
}
