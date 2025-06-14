package cgeo.geocaching;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.igreenwood.loupe.Loupe;

public class ImageDetailActivity extends AppCompatActivity {

    private ImageView imageView;
    private ViewGroup container;
    private Loupe loupe;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout
        setContentView(R.layout.imagedetail_activity);

        // Find views
        container = findViewById(R.id.container);
        imageView = findViewById(R.id.image_detail_view);

        // Attach Loupe to enable pinch-zoom, pan, double-tap and pull-to-dismiss
        loupe = new Loupe(imageView, container);

        // Optional: add pull-to-dismiss listener
        loupe.setOnViewTranslateListener(new Loupe.OnViewTranslateListener() {
            @Override
            public void onStart(@NonNull final ImageView view) {
                // gesture start
            }

            @Override
            public void onViewTranslate(@NonNull final ImageView view, final float amount) {
                // amount: drag ratio
            }

            @Override
            public void onRestore(@NonNull final ImageView view) {
                // restore if threshold not reached
            }

            @Override
            public void onDismiss(@NonNull final ImageView view) {
                // dismiss activity when pulled beyond threshold
                finish();
            }
        });

        // Optional: detect single taps by layering a GestureDetector
        final GestureDetector singleTapDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull final MotionEvent e) {
                // handle single tap if needed
                return true;
            }
        });
        container.setOnTouchListener((v, event) -> {
            // first, notify single-tap detector
            singleTapDetector.onTouchEvent(event);
            // then pass everything to Loupe
            return loupe.onTouch(v, event);
        });

        // Load image from Intent extra
        final String url = getIntent().getStringExtra("image_url");
        if (url != null && !url.isEmpty()) {
            downloadAndShowImage(url);
        }

        // Enable back arrow in ActionBar if needed
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setTitle(R.string.log_image_details);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle ActionBar back button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Download image in background and set it on the ImageView. */
    private void downloadAndShowImage(final String urlString) {
        new Thread(() -> {
            final Bitmap bmp = downloadBitmap(urlString);
            if (bmp != null) {
                runOnUiThread(() -> imageView.setImageBitmap(bmp));
            }
        }).start();
    }

    /** Download a bitmap from the given URL. */
    @Nullable
    private Bitmap downloadBitmap(final String urlString) {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            final URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            is = conn.getInputStream();
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
