package pl.grizzlysoftware.service.adapter.embedded.keycloak;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pl.grizzlysoftware.service.adapter.embedded.config.EnableEmbeddedKeycloakAutoConfiguration;

@SpringBootTest(classes = StartupTest.Configuration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class StartupTest extends KeycloakSecurityTestHelper {

    @SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
    @EnableEmbeddedKeycloakAutoConfiguration
    static class Configuration {

    }

    @Override
    void initialize() {
        super.clientId = "example-client-frontend";
        super.realm = "internal";
        super.protocol = "openid-connect";
    }

    @Test
    public void isKeycloakResourceAvailable() {
        String token = getToken("admin", "a");
        Assert.assertNotNull(token);
    }
}
