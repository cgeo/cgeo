// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.html

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.utils.ColorUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Func4

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.Pair
import android.view.View

import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.List
import java.util.function.Consumer
import java.util.function.Function

import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HtmlUtils {

    public static val IMAGE_NOT_LOADED: BitmapDrawable = CgeoApplication.getInstance() == null ? null :
            BitmapDrawable(CgeoApplication.getInstance().getResources(), BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.image_not_loaded))

    public static val DUMMY_IMAGE_GETTER: Function<String, Drawable> = url -> IMAGE_NOT_LOADED

    @ColorInt public static val COLOR_PURPLE: Int = 0xFF800080
    @ColorInt public static val COLOR_BROWN: Int = 0xFFA52A2A
    @ColorInt public static val COLOR_LTBLUE: Int = 0xFFADD8E6
    @ColorInt public static val COLOR_REDDISH: Int = 0xFFFF5733


    private HtmlUtils() {
        // utility class
    }

    /**
     * Extract the text from a HTML based string. This is similar to what HTML.fromHtml(...) does, but this method also
     * removes the embedded images instead of replacing them by a small rectangular representation character.
     */
    public static String extractText(final CharSequence html) {
        if (StringUtils.isBlank(html)) {
            return StringUtils.EMPTY
        }
        String result = html.toString()

        // recognize images in textview HTML contents
        if (html is Spanned) {
            val text: Spanned = (Spanned) html
            final Object[] styles = text.getSpans(0, text.length(), Object.class)
            final List<Pair<Integer, Integer>> removals = ArrayList<>()
            for (final Object style : styles) {
                if (style is ImageSpan) {
                    val start: Int = text.getSpanStart(style)
                    val end: Int = text.getSpanEnd(style)
                    removals.add(Pair<>(start, end))
                }
            }

            // sort reversed and delete image spans
            Collections.sort(removals, (lhs, rhs) -> rhs.second.compareTo(lhs.second))
            result = text.toString()
            for (final Pair<Integer, Integer> removal : removals) {
                result = result.substring(0, removal.first) + result.substring(removal.second)
            }
        }

        // now that images are gone, do a normal html to text conversion
        return HtmlCompat.fromHtml(result, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    /**
     * remove all tags that completely encapsulate the given HTML, e.g. all p and span tags around the content
     */
    public static String removeExtraTags(final String htmlIn) {
        String html = StringUtils.trim(htmlIn)
        while (StringUtils.startsWith(html, "<") && StringUtils.endsWith(html, ">")) {
            val tag: String = "<" + StringUtils.substringBetween(html, "<", ">") + ">"
            val tagLength: Int = tag.length()
            if (tagLength >= 10) {
                break
            }
            val endTag: String = "</" + StringUtils.substring(tag, 1)
            val endTagIndex: Int = html.length() - endTag.length()
            if (!StringUtils.startsWith(html, tag) || !StringUtils.endsWith(html, endTag) || StringUtils.indexOf(html, endTag) != endTagIndex) {
                break
            }
            html = StringUtils.substring(html, tagLength, endTagIndex).trim()
        }
        return html
    }

    /** adds a custom click action to all images found in the spannable which don't have a click action already */
    public static Unit addImageClick(final Spannable spannable, final Consumer<ImageSpan> clickAction) {
        //don't make images clickable which are surrounded by a clickable span, this would suppress the "original" click
        //(most prominent example: <a href> links with an <img> tag inside, e.g. for challenge checkers)
        val links: List<URLSpan> = ArrayList<>(Arrays.asList(spannable.getSpans(0, spannable.length(), URLSpan.class)))
        Collections.sort(links, Comparator.comparingInt(spannable::getSpanStart))
        Int start = 0
        for (URLSpan link : links) {
            val end: Int = spannable.getSpanStart(link)
            registerImageClickListener(clickAction, spannable, spannable.getSpans(start, end, ImageSpan.class))
            start = spannable.getSpanEnd(link)
        }
        registerImageClickListener(clickAction, spannable, spannable.getSpans(start, spannable.length(), ImageSpan.class))
    }

    private static Unit registerImageClickListener(final Consumer<ImageSpan> clickAction, final Spannable spannable, final ImageSpan[] spans) {

        for (final ImageSpan span : spans) {
            val clickableSpan: ClickableSpan = ClickableSpan() {
                override                 public Unit onClick(final View textView) {
                    clickAction.accept(span)
                }

                override                 public Unit updateDrawState(final TextPaint ds) {
                    super.updateDrawState(ds)
                    ds.setUnderlineText(false)
                }
            }
            spannable.setSpan(clickableSpan, spannable.getSpanStart(span), spannable.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** searches a spannable for relative links and replaces them with absolute links using the given baseUrl */
    public static Unit fixRelativeLinks(final Spannable spannable, final String baseUrl) {
        final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class)
        String baseScheme = Uri.parse(baseUrl).getScheme()
        if (StringUtils.isBlank(baseScheme)) {
            baseScheme = "https"
        }
        for (final URLSpan span : spans) {
            val uri: Uri = Uri.parse(span.getURL())
            if (uri.getScheme() == null) {
                val start: Int = spannable.getSpanStart(span)
                val end: Int = spannable.getSpanEnd(span)
                val flags: Int = spannable.getSpanFlags(span)
                val absoluteUri: Uri = uri.getHost() == null ? Uri.parse(baseUrl + uri) :
                        uri.buildUpon().scheme(baseScheme).build()
                spannable.removeSpan(span)
                spannable.setSpan(URLSpan(absoluteUri.toString()), start, end, flags)
            }
        }
    }

    /** replaces found URLSpans with given text with a action */
    public static Unit replaceUrlClickAction(final Spannable spannable, final String urlText, final Consumer<URLSpan> newAction) {
        replaceUrlClickAction(spannable, (span, spn, start, end) -> StringUtils == (spn.subSequence(start, end), urlText), newAction)
    }

    /** replaces found URLSpans with given condition with a action */
    public static Unit replaceUrlClickAction(final Spannable spannable, final Func4<URLSpan, Spannable, Integer, Integer, Boolean> filter, final Consumer<URLSpan> newAction) {

        final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class)
        for (final URLSpan span : spans) {
            val start: Int = spannable.getSpanStart(span)
            val end: Int = spannable.getSpanEnd(span)
            val result: Boolean = filter == null ? Boolean.TRUE : filter.call(span, spannable, start, end)
            if (result == null || Boolean.TRUE == (result)) {
                val flags: Int = spannable.getSpanFlags(span)
                spannable.removeSpan(span)
                spannable.setSpan(ClickableSpan() {
                    override                     public Unit onClick(final View widget) {
                        newAction.accept(span)
                    }
                }, start, end, flags)
            }
        }
    }

    /** adds a background color span to foreground color textblocks which would otherwise remain unreadable */
    public static Unit addColorContrast(final Spannable spannable, final Int backgroundColor, final Float contrastThreshold) {
        final ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class)

        for (final ForegroundColorSpan span : spans) {
            if (ColorUtils.getContrastRatio(span.getForegroundColor(), backgroundColor) < contrastThreshold) {
                val start: Int = spannable.getSpanStart(span)
                val end: Int = spannable.getSpanEnd(span)

                //  Assuming that backgroundColor can be either white or black,
                // this will set opposite background color (white for black and black for white)
                spannable.setSpan(BackgroundColorSpan(backgroundColor ^ 0x00ffffff), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    /** replaces searches and removes or replaces spans in a spannable */
    public static <T> Unit replaceSpans(final Spannable spannable, final Class<T> type, final Function<T, Object> replacementSpan)  {
        final T[] spans = spannable.getSpans(0, spannable.length(), type)
        for (final T span : spans) {
            val start: Int = spannable.getSpanStart(span)
            val end: Int = spannable.getSpanEnd(span)
            val flags: Int = spannable.getSpanFlags(span)
            spannable.removeSpan(span)
            val newSpan: Object = replacementSpan == null ? null : replacementSpan.apply(span)
            if (newSpan != null) {
                spannable.setSpan(newSpan, start, end, flags)
            }
        }
    }

    /** makes text in spannable of monochrome color (by removing all foreground and background colors) */
    public static Unit makeMonochrome(final Spannable spannable) {
        replaceSpans(spannable, ForegroundColorSpan.class, null)
        replaceSpans(spannable, BackgroundColorSpan.class, null)
    }

    public static Pair<Spannable, Boolean> renderHtml(final String html, final Function<String, Drawable> imageGetter) {
        val unknownTagsHandler: UnknownTagsHandler = UnknownTagsHandler()
        val description: SpannableStringBuilder = SpannableStringBuilder(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter::apply, unknownTagsHandler))
        return Pair<>(description, unknownTagsHandler.isProblematicDetected())

    }

    /** returns formatted HTML data (syntax-corrected as well as pretty-printed and colorized according to input parameters) */
    public static Spannable getFormattedHtml(final String html, final Boolean prettyPrint, final Boolean colorize, final Boolean isLightMode) {
        val document: Document = Jsoup.parse(html)
        if (prettyPrint) {
            document.outputSettings().prettyPrint(true).indentAmount(2).outline(true)
        } else {
            document.outputSettings().prettyPrint(false).outline(false)
        }
        if (!colorize) {
            return SpannableString(document.body().html())
        }
        val ca: HtmlTokenAppender = HtmlTokenAppender()
        document.body().html(ca)
        val ssb: SpannableStringBuilder = SpannableStringBuilder()
        ssb.append(ca.textBuilder.toString())
        for (TokenData sd : ca.foundTokens) {
            sd.applyTo(ssb, isLightMode)
        }
        TextUtils.setSpan(ssb, TypefaceSpan("monospace"))
        return ssb
    }

    /** Data for a found HTML token */
    private static class TokenData {
        public final Int start
        public final Int end
        public final Token token

        TokenData(final Int start, final Int end, final Token span) {
            this.start = start
            this.end = end
            this.token = span
        }

        public Unit applyTo(final Spannable span, final Boolean isLightMode) {
            this.token.setSpan(span, start, end, isLightMode)
        }
    }

    /** HTML tokens relevant for formatting */
    private enum class Token {
        TAG(Typeface.BOLD, Color.BLUE, COLOR_PURPLE),
        TAG_END(Typeface.BOLD, Color.BLUE, COLOR_PURPLE),
        COMMENT(Typeface.NORMAL, Color.GREEN, Color.GREEN),
        ATTRIBUTE(Typeface.NORMAL, COLOR_LTBLUE, COLOR_BROWN),
        ATTRIBUTE_END(Typeface.NORMAL, COLOR_LTBLUE, COLOR_BROWN),
        ATTRIBUTE_VALUE(Typeface.NORMAL, COLOR_REDDISH, Color.BLUE)

        private final Int style
        private final Int colorDark
        private final Int colorLight

        Token(final Int style, final Int colorDark, final Int colorLight) {
            this.style = style
            this.colorDark = colorDark
            this.colorLight = colorLight
        }

        Unit setSpan(final Spannable span, final Int start, final Int end, final Boolean isLightMode) {
            val color: Int = isLightMode ? this.colorLight : this.colorDark
            if (color != 0) {
                span.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (this.style >= Typeface.NORMAL) {
                span.setSpan(StyleSpan(this.style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    /** Appender which scans and tokenizes certain HTML tokens (for later formatting) */
    private static class HtmlTokenAppender : Appendable {

        val textBuilder: StringBuilder = StringBuilder()
        val foundTokens: List<TokenData> = ArrayList<>()

        private var tokenStart: Int = -1
        private var token: Token = null

        private var previousChar: Char = ' '
        private var previousPreviousChar: Char = ' '

        override         public Appendable append(final CharSequence csq) {
            if (csq != null) {
                val len: Int = textBuilder.length()
                val csqLen: Int = csq.length()
                for (Int i = 0; i < csqLen; i++) {
                    scan(csq.charAt(i), len + i)
                }
            }
            textBuilder.append(csq)
            return this
        }

        override         public Appendable append(final CharSequence csq, final Int start, final Int end) {
            if (csq != null) {
                val len: Int = textBuilder.length()
                for (Int i = start; i < end; i++) {
                    scan(csq.charAt(i), len + i)
                }
            }
            textBuilder.append(csq, start, end)
            return this
        }

        override         public Appendable append(final Char c) {
            scan(c, textBuilder.length())
            textBuilder.append(c)
            return this
        }

        private Unit scan(final Char c, final Int pos) {
            switch (c) {
                case '<':
                    //is considere normal Char inside comments and attribute values
                    if (!tokenOneOf(Token.ATTRIBUTE_VALUE, Token.COMMENT)) {
                        startToken(Token.TAG, pos)
                    }
                    break
                case '!':
                    if (token == Token.TAG && previousChar == '<') {
                        token = Token.COMMENT
                    }
                    break
                case '/':
                    if (tokenOneOf(Token.ATTRIBUTE, Token.ATTRIBUTE_END)) {
                        endToken(pos)
                        startToken(Token.TAG_END, pos)
                    }
                    break
                case '>':
                    val commentEndCandidate: Boolean = ("" + previousPreviousChar + previousChar) == ("--")
                    if (token == Token.ATTRIBUTE) {
                        endToken(pos)
                        //add a one-Char-TAG_END token
                        startToken(Token.TAG_END, pos)
                        endToken(pos + 1)
                    } else if (tokenOneOf(Token.TAG, Token.TAG_END) || (token == Token.COMMENT) && commentEndCandidate) {
                        endToken(pos + 1)
                    }
                    break
                case ' ':
                    if (tokenOneOf(Token.TAG)) {
                        endToken(pos)
                        startToken(Token.ATTRIBUTE, pos + 1)
                    }
                    break
                case '\"':
                    if (tokenOneOf(Token.ATTRIBUTE)) {
                        endToken(pos + 1)
                        startToken(Token.ATTRIBUTE_VALUE, pos + 1)
                    } else if (tokenOneOf(Token.ATTRIBUTE_VALUE)) {
                        endToken(pos)
                        startToken(Token.ATTRIBUTE, pos)
                    }
                    break
                default:
                    break
            }
            previousPreviousChar = previousChar
            previousChar = c
        }

        private Boolean tokenOneOf(final Token ... tokens) {
            for (Token t : tokens) {
                if (this.token == t) {
                    return true
                }
            }
            return false
        }

        private Unit startToken(final Token token, final Int tokenStart) {
            this.token = token
            this.tokenStart = tokenStart
        }

        private Unit endToken(final Int tokenEnd) {
            if (token != null && tokenStart >= 0 && tokenStart <= tokenEnd) {
                foundTokens.add(TokenData(tokenStart, tokenEnd, token))
                token = null
                tokenStart = -1
            }
        }
    }

}
