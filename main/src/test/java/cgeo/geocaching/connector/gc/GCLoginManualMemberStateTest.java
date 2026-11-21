package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCLoginManualMemberStateTest {

    @Test
    public void manualSessionResetForcesPremiumMemberStateReload() {
        final GCLogin.ManualLoginState state = new GCLogin.ManualLoginState();
        final GCLogin.ServerParameters oldParameters = serverParameters("Premium");
        assertThat(state.applyServerParameters(oldParameters)).isEqualTo(GCMemberState.PREMIUM);

        final GCLogin.ServerParameters refreshedParameters = serverParameters("Premium");
        assertThat(state.resetAndApply(refreshedParameters)).isEqualTo(GCMemberState.PREMIUM);
        assertThat(state.serverParameters).isSameAs(refreshedParameters);
    }

    @Test
    public void failedMemberStateReloadStaysUnknown() {
        final GCLogin.ManualLoginState state = new GCLogin.ManualLoginState();
        assertThat(state.applyServerParameters(serverParameters("Premium"))).isEqualTo(GCMemberState.PREMIUM);

        assertThat(state.resetAndApply(null)).isEqualTo(GCMemberState.UNKNOWN);
        assertThat(state.serverParameters).isNull();
    }

    @Test
    public void emptyMemberStateReloadStaysUnknownAndIsNotCached() {
        final GCLogin.ManualLoginState state = new GCLogin.ManualLoginState();
        assertThat(state.applyServerParameters(serverParameters("Premium"))).isEqualTo(GCMemberState.PREMIUM);

        final GCLogin.ServerParameters emptyParameters = serverParameters("");

        assertThat(state.resetAndApply(emptyParameters)).isEqualTo(GCMemberState.UNKNOWN);
        assertThat(state.serverParameters).isNull();
    }

    private static GCLogin.ServerParameters serverParameters(final String userType) {
        final GCLogin.ServerParameters params = new GCLogin.ServerParameters();
        params.userInfo = new GCLogin.ServerParameters.UserInfo();
        params.userInfo.userType = userType;
        return params;
    }
}
