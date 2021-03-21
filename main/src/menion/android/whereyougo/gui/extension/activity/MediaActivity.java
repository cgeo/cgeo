/*
 * Copyright 2017 biylda <biylda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package menion.android.whereyougo.gui.extension.activity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.Media;
import cgeo.geocaching.R;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Images;
import pl.droidsonroids.gif.GifImageView;

public class MediaActivity extends CustomActivity {
    protected int cachedMediaId;

    protected void setMedia(Media media) {
        if (media == null)
            return;
        if (media.id == cachedMediaId)
            return;
        TextView textView = (TextView) findViewById(R.id.mediaTextView);
        textView.setText(UtilsGUI.simpleHtml(media.altText));
        if (media.type == null)
            return;
        View view = null;
        switch (media.type.toLowerCase()) {
            case "mp4":
                view = findViewById(R.id.mediaVideoView);
            break;
            case "gif":
                view = findViewById(R.id.mediaGifView);
            break;
            case "jpeg":
            case "jpg":
            case "png":
            case "bmp":
                view = findViewById(R.id.mediaImageView);
        }
        if (view == null)
            return;
        byte[] data = null;
        try {
            data = Engine.mediaFile(media);
        } catch (Exception e) {
            return;
        }
        if (view.getId() == R.id.mediaImageView) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp = Images.resizeBitmap(bmp);
            ((ImageView) view).setImageBitmap(bmp);
        } else {
            // save file to cache
            String filename = FileSystem.CACHE + media.jarFilename();
            FileSystem.saveBytes(filename, data);
            File file = new File(filename);
            Uri uri = Uri.fromFile(file);
            if (view.getId() == R.id.mediaGifView) {
                ((GifImageView) view).setImageURI(uri);
            } else if (view.getId() == R.id.mediaVideoView) {
                ((VideoView) view).setVideoURI(uri);
            }
        }
        view.setVisibility(View.VISIBLE);
        cachedMediaId = media.id;
    }
}
