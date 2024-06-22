package cgeo.geocaching.utils.html;

import cgeo.geocaching.utils.ColorUtils;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

public final class HtmlUtils {

    private HtmlUtils() {
        // utility class
    }

    /**
     * Extract the text from a HTML based string. This is similar to what HTML.fromHtml(...) does, but this method also
     * removes the embedded images instead of replacing them by a small rectangular representation character.
     */
    @NonNull
    public static String extractText(final CharSequence html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY;
        }
        String result = html.toString();

        // recognize images in textview HTML contents
        if (html instanceof Spanned) {
            final Spanned text = (Spanned) html;
            final Object[] styles = text.getSpans(0, text.length(), Object.class);
            final List<Pair<Integer, Integer>> removals = new ArrayList<>();
            for (final Object style : styles) {
                if (style instanceof ImageSpan) {
                    final int start = text.getSpanStart(style);
                    final int end = text.getSpanEnd(style);
                    removals.add(new Pair<>(start, end));
                }
            }

            // sort reversed and delete image spans
            Collections.sort(removals, (lhs, rhs) -> rhs.second.compareTo(lhs.second));
            result = text.toString();
            for (final Pair<Integer, Integer> removal : removals) {
                result = result.substring(0, removal.first) + result.substring(removal.second);
            }
        }

        // now that images are gone, do a normal html to text conversion
        return HtmlCompat.fromHtml(result, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    /**
     * remove all tags that completely encapsulate the given HTML, e.g. all p and span tags around the content
     */
    @NonNull
    public static String removeExtraTags(final String htmlIn) {
        String html = StringUtils.trim(htmlIn);
        while (StringUtils.startsWith(html, "<") && StringUtils.endsWith(html, ">")) {
            final String tag = "<" + StringUtils.substringBetween(html, "<", ">") + ">";
            final int tagLength = tag.length();
            if (tagLength >= 10) {
                break;
            }
            final String endTag = "</" + StringUtils.substring(tag, 1);
            final int endTagIndex = html.length() - endTag.length();
            if (!StringUtils.startsWith(html, tag) || !StringUtils.endsWith(html, endTag) || StringUtils.indexOf(html, endTag) != endTagIndex) {
                break;
            }
            html = StringUtils.substring(html, tagLength, endTagIndex).trim();
        }
        return html;
    }

    /** adds a custom click action to all images found in the spannable which don't have a click action already */
    public static void addImageClick(final Spannable spannable, final Consumer<ImageSpan> clickAction) {
        //don't make images clickable which are surrounded by a clickable span, this would suppress the "original" click
        //(most prominent example: <a href> links with an <img> tag inside, e.g. for challenge checkers)
        final List<URLSpan> links = new ArrayList<>(Arrays.asList(spannable.getSpans(0, spannable.length(), URLSpan.class)));
        Collections.sort(links, (l1, l2) -> Integer.compare(spannable.getSpanStart(l1), spannable.getSpanStart(l2)));
        int start = 0;
        for (URLSpan link : links) {
            final int end = spannable.getSpanStart(link);
            registerImageClickListener(clickAction, spannable, spannable.getSpans(start, end, ImageSpan.class));
            start = spannable.getSpanEnd(link);
        }
        registerImageClickListener(clickAction, spannable, spannable.getSpans(start, spannable.length(), ImageSpan.class));
    }

    private static void registerImageClickListener(final Consumer<ImageSpan> clickAction, final Spannable spannable, final ImageSpan[] spans) {

        for (final ImageSpan span : spans) {
            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull final View textView) {
                    clickAction.accept(span);
                }

                @Override
                public void updateDrawState(final TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            spannable.setSpan(clickableSpan, spannable.getSpanStart(span), spannable.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** searches a spannable for relative links and replaces them with absolute links using the given baseUrl */
    public static void fixRelativeLinks(final Spannable spannable, final String baseUrl) {
        final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        String baseScheme = Uri.parse(baseUrl).getScheme();
        if (StringUtils.isBlank(baseScheme)) {
            baseScheme = "https";
        }
        for (final URLSpan span : spans) {
            final Uri uri = Uri.parse(span.getURL());
            if (uri.getScheme() == null) {
                final int start = spannable.getSpanStart(span);
                final int end = spannable.getSpanEnd(span);
                final int flags = spannable.getSpanFlags(span);
                final Uri absoluteUri = uri.getHost() == null ? Uri.parse(baseUrl + uri) :
                        uri.buildUpon().scheme(baseScheme).build();
                spannable.removeSpan(span);
                spannable.setSpan(new URLSpan(absoluteUri.toString()), start, end, flags);
            }
        }
    }

    /** replaces found URLSpans with given text with a new action
     */
    public static void replaceUrlClickAction(final Spannable spannable, final String urlText, final Consumer<URLSpan> newAction) {

        final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (final URLSpan span : spans) {
            final int start = spannable.getSpanStart(span);
            final int end = spannable.getSpanEnd(span);
            if (StringUtils.equals(spannable.subSequence(start, end), urlText)) {
                final int flags = spannable.getSpanFlags(span);
                spannable.removeSpan(span);
                spannable.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(final @NonNull View widget) {
                        newAction.accept(span);
                    }
                }, start, end, flags);
            }
        }
    }

    /** adds a background color span to foreground color textblocks which would otherwise remain unreadable */
    public static void addColorContrast(final Spannable spannable, final int backgroundColor, final float contrastThreshold) {
        final ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);

        for (final ForegroundColorSpan span : spans) {
            if (ColorUtils.getContrastRatio(span.getForegroundColor(), backgroundColor) < contrastThreshold) {
                final int start = spannable.getSpanStart(span);
                final int end = spannable.getSpanEnd(span);

                //  Assuming that backgroundColor can be either white or black,
                // this will set opposite background color (white for black and black for white)
                spannable.setSpan(new BackgroundColorSpan(backgroundColor ^ 0x00ffffff), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /** replaces searches and removes or replaces spans in a spannable */
    public static <T> void replaceSpans(final Spannable spannable, final Class<T> type, final Function<T, Object> replacementSpan)  {
        final T[] spans = spannable.getSpans(0, spannable.length(), type);
        for (final T span : spans) {
            final int start = spannable.getSpanStart(span);
            final int end = spannable.getSpanEnd(span);
            final int flags = spannable.getSpanFlags(span);
            spannable.removeSpan(span);
            final Object newSpan = replacementSpan == null ? null : replacementSpan.apply(span);
            if (newSpan != null) {
                spannable.setSpan(newSpan, start, end, flags);
            }
        }
    }

    /** makes text in spannable of monochrome color (by removing all foreground and background colors) */
    private static void makeMonochrome(final Spannable spannable) {
        replaceSpans(spannable, ForegroundColorSpan.class, null);
        replaceSpans(spannable, BackgroundColorSpan.class, null);
    }

    public static Pair<Spannable, Boolean> renderHtml(final String html, final Function<String, Drawable> imageGetter, final boolean makeMonochrome) {
        final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
        final SpannableStringBuilder description = new SpannableStringBuilder(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter::apply, unknownTagsHandler));
        if (makeMonochrome) {
            makeMonochrome(description);
        }
        return new Pair<>(description, unknownTagsHandler.isProblematicDetected());

    }

}
