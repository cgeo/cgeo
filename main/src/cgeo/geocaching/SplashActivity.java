package cgeo.geocaching;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        // continue by starting MainActivity and finishing this one
        final Intent main = new Intent(this, MainActivity.class);
        main.putExtras(getIntent());
        startActivity(main);
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
