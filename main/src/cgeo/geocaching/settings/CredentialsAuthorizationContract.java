package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.connector.gc.GCConnector;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CredentialsAuthorizationContract extends ActivityResultContract<CredentialsAuthorizationContract.Input, Intent> {

    public static final class Input {
        @NonNull
        public final Credentials credentials;
        @NonNull
        public final Class<?> authActivity;

        public Input(@NonNull final Credentials credentials, @NonNull final Class<?> authActivity) {
            this.credentials = credentials;
            this.authActivity = authActivity;
        }
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull final Context context, final Input input) {
        final Intent checkIntent = new Intent(context, input.authActivity);
        final Credentials credentials = GCConnector.getInstance().getCredentials();
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, credentials.getUsernameRaw());
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, credentials.getPasswordRaw());
        return checkIntent;
    }

    @Override
    public Intent parseResult(final int resultCode, @Nullable final Intent intent) {
        return intent;
    }
}
