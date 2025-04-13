package cgeo.geocaching.utils.html;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.ColorUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func4;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public final class HtmlUtils {

    public static final BitmapDrawable IMAGE_NOT_LOADED = CgeoApplication.getInstance() == null ? null :
            new BitmapDrawable(CgeoApplication.getInstance().getResources(), BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.image_not_loaded));

    public static final Function<String, Drawable> DUMMY_IMAGE_GETTER = url -> IMAGE_NOT_LOADED;

    @ColorInt public static final int COLOR_PURPLE = 0xFF800080;
    @ColorInt public static final int COLOR_BROWN = 0xFFA52A2A;
    @ColorInt public static final int COLOR_LTBLUE = 0xFFADD8E6;
    @ColorInt public static final int COLOR_REDDISH = 0xFFFF5733;


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
        Collections.sort(links, Comparator.comparingInt(spannable::getSpanStart));
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
                public void updateDrawState(@NonNull final TextPaint ds) {
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

    /** replaces found URLSpans with given text with a new action */
    public static void replaceUrlClickAction(final Spannable spannable, final String urlText, final Consumer<URLSpan> newAction) {
        replaceUrlClickAction(spannable, (span, spn, start, end) -> StringUtils.equals(spn.subSequence(start, end), urlText), newAction);
    }

    /** replaces found URLSpans with given condition with a new action */
    public static void replaceUrlClickAction(final Spannable spannable, final Func4<URLSpan, Spannable, Integer, Integer, Boolean> filter, final Consumer<URLSpan> newAction) {

        final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (final URLSpan span : spans) {
            final int start = spannable.getSpanStart(span);
            final int end = spannable.getSpanEnd(span);
            final Boolean result = filter == null ? Boolean.TRUE : filter.call(span, spannable, start, end);
            if (result == null || Boolean.TRUE.equals(result)) {
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
    public static void makeMonochrome(final Spannable spannable) {
        replaceSpans(spannable, ForegroundColorSpan.class, null);
        replaceSpans(spannable, BackgroundColorSpan.class, null);
    }

    public static Pair<Spannable, Boolean> renderHtml(final String html, final Function<String, Drawable> imageGetter) {
        final UnknownTagsHandler unknownTagsHandler = new UnknownTagsHandler();
        final SpannableStringBuilder description = new SpannableStringBuilder(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter::apply, unknownTagsHandler));
        return new Pair<>(description, unknownTagsHandler.isProblematicDetected());

    }

    /** returns formatted HTML data (syntax-corrected as well as pretty-printed and colorized according to input parameters) */
    public static Spannable getFormattedHtml(final String html, final boolean prettyPrint, final boolean colorize, final boolean isLightMode) {
        final Document document = Jsoup.parse(html);
        if (prettyPrint) {
            document.outputSettings().prettyPrint(true).indentAmount(2).outline(true);
        } else {
            document.outputSettings().prettyPrint(false).outline(false);
        }
        if (!colorize) {
            return new SpannableString(document.body().html());
        }
        final HtmlTokenAppender ca = new HtmlTokenAppender();
        document.body().html(ca);
        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(ca.textBuilder.toString());
        for (TokenData sd : ca.foundTokens) {
            sd.applyTo(ssb, isLightMode);
        }
        TextUtils.setSpan(ssb, new TypefaceSpan("monospace"));
        return ssb;
    }

    /** Data for a found HTML token */
    private static class TokenData {
        public final int start;
        public final int end;
        public final Token token;

        TokenData(final int start, final int end, final Token span) {
            this.start = start;
            this.end = end;
            this.token = span;
        }

        public void applyTo(final Spannable span, final boolean isLightMode) {
            this.token.setSpan(span, start, end, isLightMode);
        }
    }

    /** HTML tokens relevant for formatting */
    private enum Token {
        TAG(Typeface.BOLD, Color.BLUE, COLOR_PURPLE),
        TAG_END(Typeface.BOLD, Color.BLUE, COLOR_PURPLE),
        COMMENT(Typeface.NORMAL, Color.GREEN, Color.GREEN),
        ATTRIBUTE(Typeface.NORMAL, COLOR_LTBLUE, COLOR_BROWN),
        ATTRIBUTE_END(Typeface.NORMAL, COLOR_LTBLUE, COLOR_BROWN),
        ATTRIBUTE_VALUE(Typeface.NORMAL, COLOR_REDDISH, Color.BLUE);

        private final int style;
        private final int colorDark;
        private final int colorLight;

        Token(final int style, final int colorDark, final int colorLight) {
            this.style = style;
            this.colorDark = colorDark;
            this.colorLight = colorLight;
        }

        void setSpan(final Spannable span, final int start, final int end, final boolean isLightMode) {
            final int color = isLightMode ? this.colorLight : this.colorDark;
            if (color != 0) {
                span.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (this.style >= Typeface.NORMAL) {
                span.setSpan(new StyleSpan(this.style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /** Appender which scans and tokenizes certain HTML tokens (for later formatting) */
    private static class HtmlTokenAppender implements Appendable {

        public final StringBuilder textBuilder = new StringBuilder();
        public final List<TokenData> foundTokens = new ArrayList<>();

        private int tokenStart = -1;
        private Token token = null;

        private char previousChar = ' ';
        private char previousPreviousChar = ' ';

        @NonNull
        @Override
        public Appendable append(@Nullable final CharSequence csq) {
            if (csq != null) {
                final int len = textBuilder.length();
                final int csqLen = csq.length();
                for (int i = 0; i < csqLen; i++) {
                    scan(csq.charAt(i), len + i);
                }
            }
            textBuilder.append(csq);
            return this;
        }

        @NonNull
        @Override
        public Appendable append(@Nullable final CharSequence csq, final int start, final int end) {
            if (csq != null) {
                final int len = textBuilder.length();
                for (int i = start; i < end; i++) {
                    scan(csq.charAt(i), len + i);
                }
            }
            textBuilder.append(csq, start, end);
            return this;
        }

        @NonNull
        @Override
        public Appendable append(final char c) {
            scan(c, textBuilder.length());
            textBuilder.append(c);
            return this;
        }

        private void scan(final char c, final int pos) {
            switch (c) {
                case '<':
                    //is considere normal char inside comments and attribute values
                    if (!tokenOneOf(Token.ATTRIBUTE_VALUE, Token.COMMENT)) {
                        startToken(Token.TAG, pos);
                    }
                    break;
                case '!':
                    if (token == Token.TAG && previousChar == '<') {
                        token = Token.COMMENT;
                    }
                    break;
                case '/':
                    if (tokenOneOf(Token.ATTRIBUTE, Token.ATTRIBUTE_END)) {
                        endToken(pos);
                        startToken(Token.TAG_END, pos);
                    }
                    break;
                case '>':
                    final boolean commentEndCandidate = ("" + previousPreviousChar + previousChar).equals("--");
                    if (token == Token.ATTRIBUTE) {
                        endToken(pos);
                        //add a one-char-TAG_END token
                        startToken(Token.TAG_END, pos);
                        endToken(pos + 1);
                    } else if (tokenOneOf(Token.TAG, Token.TAG_END) || (token == Token.COMMENT) && commentEndCandidate) {
                        endToken(pos + 1);
                    }
                    break;
                case ' ':
                    if (tokenOneOf(Token.TAG)) {
                        endToken(pos);
                        startToken(Token.ATTRIBUTE, pos + 1);
                    }
                    break;
                case '\"':
                    if (tokenOneOf(Token.ATTRIBUTE)) {
                        endToken(pos + 1);
                        startToken(Token.ATTRIBUTE_VALUE, pos + 1);
                    } else if (tokenOneOf(Token.ATTRIBUTE_VALUE)) {
                        endToken(pos);
                        startToken(Token.ATTRIBUTE, pos);
                    }
                    break;
                default:
                    break;
            }
            previousPreviousChar = previousChar;
            previousChar = c;
        }

        private boolean tokenOneOf(final Token ... tokens) {
            for (Token t : tokens) {
                if (this.token == t) {
                    return true;
                }
            }
            return false;
        }

        private void startToken(final Token token, final int tokenStart) {
            this.token = token;
            this.tokenStart = tokenStart;
        }

        private void endToken(final int tokenEnd) {
            if (token != null && tokenStart >= 0 && tokenStart <= tokenEnd) {
                foundTokens.add(new TokenData(tokenStart, tokenEnd, token));
                token = null;
                tokenStart = -1;
            }
        }
    }

}
