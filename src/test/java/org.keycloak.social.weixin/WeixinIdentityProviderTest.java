package org.keycloak.social.weixin;

import org.jboss.resteasy.spi.HttpRequest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.social.weixin.mock.MockedAuthenticationSessionModel;
import org.keycloak.social.weixin.mock.MockedHttpRequest;

import java.util.UUID;

import org.powermock.api.mockito.PowerMockito;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, WeiXinIdentityProvider.class})
public class WeixinIdentityProviderTest {
    WeiXinIdentityProvider weiXinIdentityProvider;

    @BeforeClass
    public static void beforeClass() {

    }

    @Before
    public void before() {
        UUID uuid = PowerMockito.mock(UUID.class);
        Mockito.when(uuid.toString()).thenReturn("ccec3eea-fd08-4ca2-b83a-2921228f2480");

        PowerMockito.mockStatic(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuid);

        OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig();
        config.setClientId("clientId");
        weiXinIdentityProvider = new WeiXinIdentityProvider(null, config);
    }

    @AfterClass
    public static void afterClass() {

    }

    @After
    public void after() {

    }

    @Test
    public void performLoginThrowsIfHttpRequestIsNull() {
        try {
            AuthenticationRequest request = new AuthenticationRequest(null, null, null, null, null, null, null);

            weiXinIdentityProvider.performLogin(request);
        } catch (RuntimeException ex) {
            Assert.assertEquals(ex.toString(), "org.keycloak.broker.provider.IdentityBrokerException: Could not create authentication request because java.lang.NullPointerException");
//            Assert.assertEquals("pc goes to customized login url", "", weiXinIdentityProvider.performLogin(request));
        }
    }

    @Test
    public void pcGoesToQRConnect() {
        IdentityBrokerState state = IdentityBrokerState.decoded("state", "clientId", "tabId");
        var authSession = new MockedAuthenticationSessionModel();

        HttpRequest httpRequest = new MockedHttpRequest();
        AuthenticationRequest request = new AuthenticationRequest(null, null, authSession, httpRequest, null, state, "https" +
                "://redirect.to.customized/url");

        var res = weiXinIdentityProvider.performLogin(request);

        Assert.assertEquals("303 redirect", Response.Status.SEE_OTHER.getStatusCode(), res.getStatus());
        Assert.assertEquals("pc goes to customized login url", "https://open.weixin.qq" +
                ".com/connect/qrconnect?scope=snsapi_login&state=state.tabId" +
                ".clientId&appid=clientId&redirect_uri=https%3A%2F%2Fredirect.to" +
                ".customized%2Furl&nonce=ccec3eea-fd08-4ca2-b83a-2921228f2480", res.getLocation().toString());
    }

    @Test
    public void pcGoesToCustomizedURLIfPresent() {
        var config = new WeixinProviderConfig();
        config.setClientId("clientId");
        config.setCustomizedLoginUrlForPc("https://another.url/path");

        Assert.assertEquals("set config get config", "https://another.url/path", config.getCustomizedLoginUrlForPc());

        weiXinIdentityProvider = new WeiXinIdentityProvider(null, config);

        IdentityBrokerState state = IdentityBrokerState.decoded("state", "clientId", "tabId");
        var authSession = new MockedAuthenticationSessionModel();

        HttpRequest httpRequest = new MockedHttpRequest();
        AuthenticationRequest request = new AuthenticationRequest(null, null, authSession, httpRequest, null, state, "https" +
                "://redirect.to.customized/url");

        var res = weiXinIdentityProvider.performLogin(request);

        Assert.assertEquals("303 redirect", Response.Status.SEE_OTHER.getStatusCode(), res.getStatus());
        Assert.assertEquals("pc goes to customized login url", "https://another.url/path", res.getLocation().toString());
    }
}