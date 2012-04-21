package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class LiveMapInfo extends AbstractActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.transparent);
        setContentView(R.layout.livemapinfo);

        final int showCount = Settings.getLiveMapHintShowCount();

        if (showCount > 2) {
            final CheckBox cb = (CheckBox) findViewById(R.id.live_map_hint_hide);
            cb.setVisibility(View.VISIBLE);
        }

        final Button closeButton = (Button) findViewById(R.id.live_map_hint_ok);
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final CheckBox cb = (CheckBox) findViewById(R.id.live_map_hint_hide);
                if (cb.isChecked()) {
                    Settings.setHideLiveHint(true);
                }
                finish();
            }
        });

        Settings.setLiveMapHintShowCount(showCount + 1);
    }

    @Override
    protected void onStop() {

        final CheckBox cb = (CheckBox) findViewById(R.id.live_map_hint_hide);
        if (cb.isChecked()) {
            Settings.setHideLiveHint(true);
        }

        super.onStop();
    }

}
