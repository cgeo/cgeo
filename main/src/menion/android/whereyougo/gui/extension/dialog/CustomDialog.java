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

package menion.android.whereyougo.gui.extension.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cgeo.geocaching.R;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.utils.Utils;

public class CustomDialog {

    public static final int NO_IMAGE = Integer.MIN_VALUE;
    public static final int BOTTOM_COLOR_A3 = 0xFFDDDDDD;
    private static final int TITLE_BUTTON_RIGHT = 1;
    private static final int TITLE_BUTTON_LEFT = 2;

    private static void addViewToContent(View viewContent, LinearLayout.LayoutParams llLp, View view) {
        LinearLayout llContent = (LinearLayout) viewContent;
        llContent.removeAllViews();
        if (llLp == null) {
            llContent.addView(view,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else {
            llContent.addView(view, llLp);
        }
    }

    public static void setBottom(Activity activity, String positiveButtonText,
                                 OnClickListener positiveButtonClickListener, String neutralButtonText,
                                 OnClickListener neutralButtonClickListener, String negativeButtonText,
                                 OnClickListener negativeButtonClickListener) {
        setCustomDialogBottom(activity.findViewById(R.id.linear_layout_bottom), positiveButtonText,
                positiveButtonClickListener, neutralButtonText, neutralButtonClickListener,
                negativeButtonText, negativeButtonClickListener);
    }

    private static boolean setButton(View layout, int btnId, final int btnType, String text,
                                     final OnClickListener click) {
        if (text != null && click != null) {
            // set button
            Button btn = (Button) layout.findViewById(btnId);
            btn.setText(text);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    click.onClick(null, v, btnType);
                }
            });
            btn.setVisibility(View.VISIBLE);
            return true;
        } else {
            // if no confirm button just set the visibility to GONE
            layout.findViewById(btnId).setVisibility(View.GONE);
            return false;
        }
    }

    public static void setContent(Activity activity, View view, int margins, boolean fillHeight,
                                  boolean dialog) {
        // set width to correct values if dialog is shown
        if (dialog) {
            UtilsGUI.setWindowDialogCorrectWidth(activity.getWindow());
        }
        LayoutParams lp =
                new LayoutParams(LayoutParams.MATCH_PARENT, fillHeight ? LayoutParams.MATCH_PARENT
                        : LayoutParams.WRAP_CONTENT);
        if (margins > 0)
            lp.setMargins(margins, activity.getResources().getDimensionPixelSize(R.dimen.shadow_height)
                    + margins, margins, margins);
        LinearLayout llCon = (LinearLayout) activity.findViewById(R.id.linear_layout_content);
        llCon.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                fillHeight ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT));
        addViewToContent(llCon, lp, view);
    }

    private static void setCustomDialogBottom(View view, String positiveButtonText,
                                              OnClickListener positiveButtonClickListener, String neutralButtonText,
                                              OnClickListener neutralButtonClickListener, String negativeButtonText,
                                              OnClickListener negativeButtonClickListener) {

        // change colors for 3.0+
        if (Utils.isAndroid30OrMore()) {
            view.findViewById(R.id.linear_layout_bottom).setBackgroundColor(BOTTOM_COLOR_A3);
        }

        int btnCount = 0;
        if (setButton(view, R.id.button_positive, DialogInterface.BUTTON_POSITIVE, positiveButtonText,
                positiveButtonClickListener))
            btnCount++;
        if (setButton(view, R.id.button_negative, DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
                negativeButtonClickListener))
            btnCount++;
        if (setButton(view, R.id.button_neutral, DialogInterface.BUTTON_NEUTRAL, neutralButtonText,
                neutralButtonClickListener))
            btnCount++;

        if (btnCount == 0) {
            view.findViewById(R.id.linear_layout_bottom).setVisibility(View.GONE);
        } else if (btnCount == 1) {
            view.findViewById(R.id.linear_layout_bottom).setVisibility(View.VISIBLE);
            view.findViewById(R.id.linear_layout_left_spacer).setVisibility(View.VISIBLE);
            view.findViewById(R.id.linear_layout_right_spacer).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.linear_layout_bottom).setVisibility(View.VISIBLE);
            view.findViewById(R.id.linear_layout_left_spacer).setVisibility(View.GONE);
            view.findViewById(R.id.linear_layout_right_spacer).setVisibility(View.GONE);
        }
    }

    private static void setCustomDialogTitle(View view, CharSequence titleText, Bitmap titleImage,
                                             int titleExtraImg1, final OnClickListener titleExtraClick1, int titleExtraImg2,
                                             final OnClickListener titleExtraClick2) {
        // set the dialog title
        if (titleImage == null && titleText == null && titleExtraImg1 == NO_IMAGE
                && titleExtraImg2 == NO_IMAGE) {
            // hide title
            view.findViewById(R.id.linear_layout_title).setVisibility(View.GONE);
        } else {
            // set title image
            if (titleImage == null) {
                view.findViewById(R.id.image_view_title_logo).setVisibility(View.INVISIBLE);
            } else {
                ((ImageView) view.findViewById(R.id.image_view_title_logo)).setImageBitmap(titleImage);
            }

            // set title text
            ((TextView) view.findViewById(R.id.text_view_title_text)).setText(titleText);

            // set title extra buttons
            setCustomDialogTitleButton(view, TITLE_BUTTON_RIGHT, titleExtraImg1, titleExtraClick1);
            setCustomDialogTitleButton(view, TITLE_BUTTON_LEFT, titleExtraImg2, titleExtraClick2);
        }
    }

    private static void setCustomDialogTitleButton(View view, int button, int titleExtraImg,
                                                   final OnClickListener titleExtraClick) {
        // set title extra
        if (titleExtraImg != NO_IMAGE && titleExtraClick != null) {
            ImageView iv;
            ImageButton ib;
            if (button == TITLE_BUTTON_RIGHT) {
                iv = (ImageView) view.findViewById(R.id.image_view_separator1);
                ib = (ImageButton) view.findViewById(R.id.image_button_title1);
            } else {
                iv = (ImageView) view.findViewById(R.id.image_view_separator2);
                ib = (ImageButton) view.findViewById(R.id.image_button_title2);
            }

            iv.setVisibility(View.VISIBLE);
            ib.setVisibility(View.VISIBLE);
            ib.setImageResource(titleExtraImg);
            ib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    titleExtraClick.onClick(null, v, 0);
                }
            });
        }
    }

    // public static void setContent(Activity activity, View view, int margins, boolean dialog) {
    // setContent(activity, view, margins, false, dialog);
    // }

    public static void setTitle(Activity activity, CharSequence titleText, Bitmap titleImage,
                                int titleExtraImg, final OnClickListener titleExtraClick) {
        setCustomDialogTitle(activity.findViewById(R.id.linear_layout_main), titleText, titleImage,
                titleExtraImg, titleExtraClick, NO_IMAGE, null);
    }

    public interface OnClickListener {
        boolean onClick(CustomDialog dialog, View v, int btn);
    }
}
