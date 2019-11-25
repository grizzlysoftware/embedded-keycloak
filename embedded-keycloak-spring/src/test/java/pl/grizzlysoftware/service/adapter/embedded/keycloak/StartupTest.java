package pl.grizzlysoftware.service.adapter.embedded.keycloak;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = StartupTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
public class StartupTest extends KeycloakSecurityTestHelper {

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
