package cgeo.geocaching.unifiedmap.mapsforgevtm.legend;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.WrappingGridView;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeThemeHelper;
import cgeo.geocaching.utils.DisplayUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.theme.XmlThemeResourceProvider;
import static org.oscim.backend.CanvasAdapter.getBitmapAsset;

public class RenderThemeLegend {

    private RenderThemeLegend() {
        // utility class
    }

    public static void showLegend(final Activity activity, final MapsforgeThemeHelper rth) {
        final LegendCategory[] categories;
        final ArrayList<LegendEntry> entries = new ArrayList<>();
        final ThemeLegend legend;

        // add elements depending on currently selected map rendering theme
        final MapsforgeThemeHelper.RenderThemeType rtt = MapsforgeThemeHelper.getRenderThemeType();
        switch (rtt) {
            case RTT_ELEVATE:
                legend = new ThemeLegendElevate();
                break;
            case RTT_FZK_BASE:
            case RTT_FZK_OUTDOOR_CONTRAST:
            case RTT_FZK_OUTDOOR_SOFT:
                legend = new ThemeLegendFreizeitkarte();
                break;
            case RTT_PAWS:
                legend = new ThemeLegendOSMPaws();
                break;
            case RTT_VOLUNTARY:
                legend = new ThemeLegendVoluntary();
                break;
            default:
                SimpleDialog.of(activity).setMessage(TextParam.text("No legend available for current theme")).show();
                return;
        }
        categories = legend.loadLegend(rtt, entries);

        final LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.legend_scrollview, null);
        final LinearLayout listView = dialogView.findViewById(R.id.list);

        for (LegendCategory cat : categories) {
            final View v = inflater.inflate(R.layout.legend_item, null);
            listView.addView(v);

            final TextView title = v.findViewById(R.id.category);
            title.setText(cat.category);

            final WrappingGridView grid1 = v.findViewById(R.id.legend_grid);
            final int count1 = fillGrid(activity, entries, grid1, cat.columns1, cat.id1, rth);

            final WrappingGridView grid2 = v.findViewById(R.id.legend_grid2);
            final int count2 = fillGrid(activity, entries, grid2, cat.columns2, cat.id2, rth);

            title.setOnClickListener(v1 -> {
                grid1.setVisibility(count1 == 0 || grid1.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                grid2.setVisibility(count2 == 0 || grid2.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                final int arrowRes = grid1.getVisibility() == View.VISIBLE || grid2.getVisibility() == View.VISIBLE ? R.drawable.expand_less : R.drawable.expand_more;
                title.setCompoundDrawablesWithIntrinsicBounds(0, 0, arrowRes, 0);
            });

        }
        dialogView.findViewById(R.id.info_button).setOnClickListener(v1 -> ShareUtils.openUrl(activity, activity.getString(legend.getInfoUrl())));
        Dialogs.bottomSheetDialogWithActionbar(activity, dialogView, R.string.map_theme_legend).show();
    }

    private static int fillGrid(final Activity activity, final ArrayList<LegendEntry> entries, final WrappingGridView grid, final int columns, final int id, final MapsforgeThemeHelper rth) {
        grid.setNumColumns(columns);
        grid.setColumnWidth((int) (DisplayUtils.getDisplaySize().x * 0.75f / columns));

        final ArrayList<LegendEntry> a = new ArrayList<>();
        for (LegendEntry entry : entries) {
            if (entry.category == id) {
                a.add(entry);
            }
        }
        grid.setAdapter(new LegendGridAdapter(activity, a, rth));
        grid.setVisibility(View.GONE);
        return a.size();
    }

    static class LegendCategory {
        public String category;
        public int id1;
        public int id2;
        public int columns1;
        public int columns2;

        LegendCategory(final int id1, final int id2, @StringRes final int category, final int columns1, final int columns2) {
            this.category = CgeoApplication.getInstance().getString(category);
            this.id1 = id1;
            this.id2 = id2;
            this.columns1 = columns1;
            this.columns2 = columns2;
        }
    }

    static class LegendEntry {
        public int category;
        public String legend;
        // either provide this
        public int drawable;
        // or those
        public String relativePath;
        public String filename;

        LegendEntry(final int category, @DrawableRes final int drawable, final String legend) {
            this(category, legend, drawable, "", "");
        }

        LegendEntry(final int category, @DrawableRes final int drawable, @StringRes final int legend) {
            this(category, CgeoApplication.getInstance().getString(legend), drawable, "", "");
        }

        LegendEntry(final int category, final String relativePath, final String filename) {
            this(category, (StringUtils.equals(filename.substring(1, 2), "_") ? filename.substring(2) : filename).replace("_", " "), 0, relativePath, filename + ".svg");
        }

        LegendEntry(final int category, final String relativePath, final String filename, @StringRes final int legend) {
            this(category, CgeoApplication.getInstance().getString(legend), 0, relativePath, filename + ".svg");
        }

        LegendEntry(final int category, final String legend, @DrawableRes final int drawable, final String relativePath, final String filename) {
            this.category = category;
            this.legend = legend;
            this.drawable = drawable;
            this.relativePath = relativePath;
            this.filename = filename;
        }
    }

    private static class LegendGridAdapter extends BaseAdapter {
        private final Activity activity;
        private final ArrayList<LegendEntry> entries;
        private final int height;
        private final XmlThemeResourceProvider resourceProvider;

        LegendGridAdapter(final Activity activity, final ArrayList<LegendEntry> entries, final MapsforgeThemeHelper rth) {
            this.activity = activity;
            this.entries = entries;
            this.height = DisplayUtils.getPxFromDp(activity.getResources(), 40, 1.0f);
            this.resourceProvider = rth.getResourceProvider();
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(final int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view = convertView != null ? convertView : activity.getLayoutInflater().inflate(R.layout.legend_imageview, null);
            final ImageView im = view.findViewById(R.id.image);
            if (height > 0) {
                im.setMaxHeight(height);
                im.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }

            final LegendEntry entry = (LegendEntry) getItem(position);
            if (entry.drawable == 0) {
                if (resourceProvider != null) {
                    try {
                        final Bitmap b = getBitmapAsset(entry.relativePath, entry.filename, resourceProvider, 0, height, 100);
                        if (b != null) {
                            im.setImageBitmap(AndroidGraphics.getBitmap(b));
                        } else {
                            Log.w("UnifiedMap.RenderThemeLegend: missing bitmap at position " + position);
                        }
                    } catch (IOException ignore) {
                    }
                }
            } else {
                im.setImageResource(entry.drawable);
            }
            ViewUtils.setTooltip(im, TextParam.text(entry.legend));
            return view;
        }
    }

    public static boolean supportsLegend() {
        final MapsforgeThemeHelper.RenderThemeType rtt = MapsforgeThemeHelper.getRenderThemeType();
        switch (rtt) {
            case RTT_ELEVATE:
            case RTT_FZK_BASE:
            case RTT_FZK_OUTDOOR_CONTRAST:
            case RTT_FZK_OUTDOOR_SOFT:
            case RTT_PAWS:
            case RTT_VOLUNTARY:
                return true;
            default:
                return false;
        }

    }

}
