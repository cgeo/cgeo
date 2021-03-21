/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.gui.extension;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import cgeo.geocaching.R;
import menion.android.whereyougo.utils.Const;
import menion.android.whereyougo.utils.Images;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class IconedListAdapter extends BaseAdapter {

    private static final int TYPE_LIST_VIEW = 0;
    private static final int TYPE_SPINNER_VIEW = 1;
    private static final int TYPE_OTHER = 2;
    private static final int PADDING = (int) Utils.getDpPixels(4.0f);
    private static final String TAG = "IconedListAdapter";
    private final ArrayList<DataInfo> mData;
    private final Context context;
    private int type = TYPE_LIST_VIEW;
    /**
     * visibility of bottom view
     */
    private int textView02Visibility = View.VISIBLE;
    /**
     * hide bottom view if no text is available
     */
    private boolean textView02HideIfEmpty = false;
    /* min height for line */
    private int minHeight = Integer.MIN_VALUE;
    // rescale image size
    private float multiplyImageSize = 1.0f;

    // public static final Drawable SEPARATOR =
    // A.getApp().getResources().getDrawable(R.drawable.var_separator);

    public IconedListAdapter(Context context, ArrayList<DataInfo> data, View view) {
        this.mData = data;

        if (view instanceof ListView) {
            ListView listView = (ListView) view;
            listView.setBackgroundColor(Color.WHITE);
            this.type = TYPE_LIST_VIEW;
        } else if (view instanceof Spinner) {
            this.type = TYPE_SPINNER_VIEW;
        } else {
            setTextView02Visible(View.GONE, true);

            this.type = TYPE_OTHER;
        }

        this.context = context;
    }

    private static LinearLayout createEmptyView(Context context) {
        return (LinearLayout) LinearLayout.inflate(context, R.layout.iconed_list_adapter, null);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    public DataInfo getDataInfo(int position) {
        return mData.get(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createEmptyView(context);
        }
        return getViewItem(position, convertView, true);
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = createEmptyView(context);
        }
        return getViewItem(position, convertView, false);
    }

    private View getViewItem(int position, View convertView, boolean dropDown) {
        try {
            final DataInfo di = mData.get(position);

            LinearLayout llMain = (LinearLayout) convertView.findViewById(R.id.linear_layout_main);
            llMain.setPadding(PADDING, PADDING, PADDING, PADDING);
            if (minHeight != Integer.MIN_VALUE) {
                llMain.setMinimumHeight(minHeight);
            }

            TextView tv01 = (TextView) convertView.findViewById(R.id.layoutIconedListAdapterTextView01);
            TextView tv02 = (TextView) convertView.findViewById(R.id.layoutIconedListAdapterTextView02);
            ImageView iv01 =
                    (ImageView) convertView.findViewById(R.id.layoutIconedListAdapterImageView01);
            ImageView iv02 =
                    (ImageView) convertView.findViewById(R.id.layoutIconedListAdapterImageView02);

            // set TextView top
            tv01.setBackgroundColor(Color.TRANSPARENT);
            tv01.setTextColor(Color.BLACK);
            String name = di.getName();
            if (name == null) {
                tv01.setVisibility(View.GONE);
            } else {
                tv01.setVisibility(View.VISIBLE);
                tv01.setText(Html.fromHtml(name));
            }

            // set TextView bottom
            tv02.setTextColor(Color.DKGRAY);

            // set additional parameters
            if (textView02Visibility != View.GONE) {
                tv02.setVisibility(View.VISIBLE);
                String desc = di.getDescription();
                if (desc == null) {
                    desc = "";
                }
                if (desc.length() > 0) {
                    tv02.setText(Html.fromHtml(desc));
                } else {
                    if (textView02HideIfEmpty) {
                        tv02.setVisibility(View.GONE);
                    } else {
                        tv02.setText(R.string.no_description);
                    }
                }
            } else {
                tv02.setVisibility(View.GONE);
            }

            // compute MULTI
            float multi = 1.0f;
            if (type == TYPE_SPINNER_VIEW && !dropDown) {
                multi = 0.75f;
            } else if (type == TYPE_SPINNER_VIEW && dropDown) {
                multi = 1.25f;
                // hack to fix spinnerView
                tv01.setHeight((int) (multi * Images.SIZE_BIG));
            } else if (type == TYPE_LIST_VIEW) {
                multi = 1.0f;
            } else if (type == TYPE_OTHER) {
                // for dialogs and similar things
                multi = 1.0f;
            }
            multi *= multiplyImageSize;

            // set ImageView left
            int iv01Width = (int) (multi * Images.SIZE_BIG);
            if (di.getImage() != -1) {
                iv01.setImageResource(di.getImage());
            } else if (di.getImageD() != null) {
                iv01.setImageDrawable(di.getImageD());
            } else if (di.getImageB() != null) {
                // resize image if is too width
                Bitmap bitmap = di.getImageB();
                if (bitmap.getWidth() > Const.SCREEN_WIDTH / 2 && di.getName() != null
                        && di.getName().length() > 0) {
                    bitmap = Images.resizeBitmap(bitmap, Const.SCREEN_WIDTH / 2);
                } else if (bitmap.getWidth() > Const.SCREEN_WIDTH) {
                    bitmap = Images.resizeBitmap(bitmap, Const.SCREEN_WIDTH);
                }

                iv01.setImageBitmap(bitmap);
            } else {
                iv01Width = 0;
            }

            // set visibility and size
            ViewGroup.LayoutParams params = iv01.getLayoutParams();
            params.width = iv01Width;
            params.height = (int) (multi * Images.SIZE_BIG);
            iv01.setLayoutParams(params);
            iv01.setVisibility(View.VISIBLE);

            // set ImageView right
            iv02.setVisibility(View.GONE);

            if (di.getImageRight() != null) {
                iv02.setVisibility(View.VISIBLE);
                iv02.setImageBitmap(di.getImageRight());
            }

            // now set enabled
            if (di.enabled)
                llMain.setBackgroundColor(Color.TRANSPARENT);
            else
                llMain.setBackgroundColor(Color.LTGRAY);
        } catch (Exception e) {
            Logger.e(TAG, "getView(" + position + ", " + convertView + ")", e);
        }

        convertView.forceLayout();
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        try {
            return mData.get(position).enabled;
        } catch (Exception e) {
            Logger.e(TAG, "isEnabled(" + position + ")", e);
            return false;
        }
    }

    public void setMinHeight(int i) {
        this.minHeight = i;
    }

    public void setMultiplyImageSize(float multiplyImageSize) {
        this.multiplyImageSize = multiplyImageSize;
    }

    public void setTextView02Visible(int visibility, boolean hideIfEmpty) {
        this.textView02Visibility = visibility;
        this.textView02HideIfEmpty = hideIfEmpty;
    }
}
