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

package pl.grizzlysoftware.service.adapter.embedded.config;

import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.listeners.KeycloakSessionDestroyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import pl.grizzlysoftware.service.adapter.embedded.util.EmbeddedKeycloakInitialContext;
import pl.grizzlysoftware.service.embedded.keycloak.EmbeddedKeycloakApplication;
import pl.grizzlysoftware.service.embedded.keycloak.model.EmbeddedKeycloakServerProperties;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;

import static org.keycloak.services.resources.KeycloakApplication.KEYCLOAK_CONFIG_PARAM_NAME;
import static org.keycloak.services.resources.KeycloakApplication.KEYCLOAK_EMBEDDED;


@Configuration
public class EmbeddedKeycloakConfig {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakConfig.class);

    @Bean
    EmbeddedKeycloakServerProperties keycloakServerProperties(ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:keycloak.properties");
        if (!resource.exists()) {
            LOG.debug("keycloak configuration properties not found, loading default configuration");
            return new EmbeddedKeycloakServerProperties();
        } else {
            try {
                Properties props = new Properties();
                props.load(resource.getInputStream());
                return new EmbeddedKeycloakServerProperties(
                        props.getProperty(EmbeddedKeycloakServerProperties.SERVER_CONTEXT_PATH),
                        props.getProperty(EmbeddedKeycloakServerProperties.SERVER_CONFIGURATION_PATH),
                        props.getProperty(EmbeddedKeycloakServerProperties.REALM_CONFIGURATION_PATH),
                        props.getProperty(EmbeddedKeycloakServerProperties.DEFAULT_REALM),
                        props.getProperty(EmbeddedKeycloakServerProperties.ADMIN_USERNAME),
                        props.getProperty(EmbeddedKeycloakServerProperties.ADMIN_PASSWORD),
                        props.getProperty(EmbeddedKeycloakServerProperties.DATASOURCE_URL),
                        props.getProperty(EmbeddedKeycloakServerProperties.DATASOURCE_USERNAME),
                        props.getProperty(EmbeddedKeycloakServerProperties.DATASOURCE_PASSWORD)
                );
            } catch (IOException e) {
                LOG.debug("exception while loading keycloak configuration, loading default configuration");
                return new EmbeddedKeycloakServerProperties();
            }
        }
    }

    @Bean
    ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(ServletContext servletContext, EmbeddedKeycloakServerProperties properties) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(properties.datasourceUrl);
        dataSource.setUser(properties.datasourceUsername);
        dataSource.setPassword(properties.datasourcePassword);
        mockJndiEnvironment(dataSource);

        ServletRegistrationBean registration = new ServletRegistrationBean<>(new HttpServlet30Dispatcher());
        registration.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
        registration.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, properties.serverContextPath);
        registration.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "false");
        registration.addUrlMappings(properties.serverContextPath + "/*");
        registration.setLoadOnStartup(2);
        registration.setAsyncSupported(true);

        /**
         * passing configuration to servlet context - servlet context is shared between dispatcher servlet and this keycloak servlet
         * it's not creating additional servlet context(don't know why)
         */
        InputStream confStream = getClass().getClassLoader().getResourceAsStream(properties.serverConfigPath);
        Objects.requireNonNull(confStream);
        String conf = IOUtils.toString(confStream, Charset.forName("US-ASCII"));
        servletContext.setInitParameter(KEYCLOAK_CONFIG_PARAM_NAME, conf);
        servletContext.setInitParameter(KEYCLOAK_EMBEDDED, "true");
        servletContext.setInitParameter(EmbeddedKeycloakApplication.SERVER_CONTEXT_PATH, properties.serverContextPath);
//        servletContext.setInitParameter(EmbeddedKeycloakApplication.SERVER_CONFIGURATION_PATH, properties.serverConfigPath);
        servletContext.setInitParameter(EmbeddedKeycloakApplication.REALM_CONFIGURATION_PATH, properties.realmConfigPath);
//        servletContext.setInitParameter(EmbeddedKeycloakApplication.DEFAULT_REALM, properties.defaultRealm);
        servletContext.setInitParameter(EmbeddedKeycloakApplication.ADMIN_USERNAME, properties.adminUser);
        servletContext.setInitParameter(EmbeddedKeycloakApplication.ADMIN_PASSWORD, properties.adminPassword);

        return registration;
    }

    @Bean
    ServletListenerRegistrationBean<KeycloakSessionDestroyListener> keycloakSessionDestroyListener() {
        return new ServletListenerRegistrationBean<>(new KeycloakSessionDestroyListener());
    }

    @Bean
    ApplicationListener<WebServerInitializedEvent> onApplicationReadyEventListener(ServletContext servletContext, EmbeddedKeycloakServerProperties keycloakServerProperties) {
        return (evt) -> {
            int port = evt.getWebServer().getPort();
            String keycloakContextPath = servletContext.getContextPath() + keycloakServerProperties.serverContextPath;

            LOG.info("Embedded Keycloak started: http://localhost:{}{} to use keycloak", port, keycloakContextPath);
        };
    }

    @Bean
    FilterRegistrationBean<KeycloakSessionServletFilter> keycloakSessionManagement(EmbeddedKeycloakServerProperties keycloakServerProperties) {
        FilterRegistrationBean<KeycloakSessionServletFilter> filter = new FilterRegistrationBean<>();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new KeycloakSessionServletFilter());
        filter.addUrlPatterns(keycloakServerProperties.serverContextPath + "/*");

        return filter;
    }

    private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
        if (NamingManager.hasInitialContextFactoryBuilder()) {
            EmbeddedKeycloakInitialContext initialContext = (EmbeddedKeycloakInitialContext) NamingManager.getInitialContext(null);
            initialContext.setDataSource(dataSource);
            return;
        }

        NamingManager.setInitialContextFactoryBuilder((env) -> environment -> new EmbeddedKeycloakInitialContext(dataSource));
    }
}
