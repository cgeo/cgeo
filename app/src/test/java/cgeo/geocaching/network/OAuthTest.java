package cgeo.geocaching.network;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class OAuthTest {

    /**
     * Test example from https://dev.twitter.com/oauth/overview/creating-signatures
     */
    @Test
    public void testOAuthSignature() {
        final Parameters params = new Parameters(
                "status", "Hello Ladies + Gentlemen, a signed OAuth request!",
                "include_entities", "true",
                "oauth_consumer_key", "xvz1evFS4wEEPTGEFPHBog",
                "oauth_nonce", "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg",
                "oauth_signature_method", "HMAC-SHA1",
                "oauth_timestamp", "1318622958",
                "oauth_token", "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb",
                "oauth_version", "1.0");
        assertThat(OAuth.signCompletedOAuth("api.twitter.com", "/1/statuses/update.json", "POST", true,
                params, "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE", "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw"))
                .isEqualTo("tnnArxj06cWHq44gCs1OSKk/jLY=");
    }

}
