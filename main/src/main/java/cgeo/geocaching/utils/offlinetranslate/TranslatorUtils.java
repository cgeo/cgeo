package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.TranslationsettingsDialogBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

public class TranslatorUtils {

    private TranslatorUtils() {
        //no instance
    }

    @Nullable
    public static String getEffectiveSourceLanguage(final String sourceLanguage, final String sourceLanguageDetected) {
        return sourceLanguage != null ? sourceLanguage : sourceLanguageDetected;
    }

    @NonNull
    public static String getEffectiveTargetLanguage(final String targetLanguage) {
        if (targetLanguage != null) {
            return targetLanguage;
        }
        if (TranslationModelManager.get().getSupportedLanguages().contains(Settings.getUserLanguage())) {
            return Settings.getUserLanguage();
        }
        return "en";
    }

    /** translates texts with special handling in cases it is normal text or contains HTML */
    public static void translateAny(final Translator translator, final String textOrHtml, final Consumer<String> consumer) {
        if (StringUtils.isBlank(textOrHtml)) {
            //Nothing to translate
            AndroidRxUtils.runOnUi(() -> consumer.accept(textOrHtml));
        } else if (TextUtils.containsHtml(textOrHtml)) {
            translateHtml(translator, textOrHtml, consumer);
        } else {
            translator.translate(textOrHtml, consumer);
        }
    }

    /** translates texts in HTML */
    private static void translateHtml(final Translator translator, final String html, final Consumer<String> consumer) {
        final Document document = Jsoup.parseBodyFragment(html);
        final List<TextNode> elements = document.children().select("*").textNodes();
        final AtomicInteger remaining = new AtomicInteger(elements.size());
        if (remaining.get() == 0) {
            //Nothing to translate
            AndroidRxUtils.runOnUi(() -> consumer.accept(html));
        } else {
            for (TextNode textNode : elements) {
                translator.translate(textNode.text(), translation -> {
                    textNode.text(translation == null ? "?" + textNode.text() : translation);
                    //check if all done
                    if (remaining.decrementAndGet() == 0) {
                        consumer.accept(document.body().html());
                    }
                });
            }
        }
    }

    public static void changeSettings(final Context context, final Translator transCtx) {

        final TranslationsettingsDialogBinding binding = TranslationsettingsDialogBinding.inflate(LayoutInflater.from(context));
        final List<String> sourceLanguageList = new ArrayList<>(TranslationModelManager.get().getSupportedLanguages());
        Collections.sort(sourceLanguageList, getLanguageSortComparator(true, transCtx.getSourceLanguage(), transCtx.getSourceLanguageDetected()));
        final List<String> targetLanguageList = new ArrayList<>(sourceLanguageList);
        Collections.sort(targetLanguageList, getLanguageSortComparator(false, transCtx.getTargetLanguage(), null));

        final TextSpinner<String> sourceLanguage = new TextSpinner<>();
        sourceLanguage.setTextView(binding.sourceLanguageSelect)
            .setDisplayMapper(lng -> TextParam.text(getUserDisplayableString(lng, false, transCtx.getSourceLanguageDetected())))
            .setTextDisplayMapper(lng -> TextParam.text(getUserDisplayableString(lng, true, transCtx.getSourceLanguageDetected())))
            .setValues(sourceLanguageList)
            .set(transCtx.getEffectiveSourceLanguage());

        final TextSpinner<String> targetLanguage = new TextSpinner<>();
        targetLanguage.setTextView(binding.targetLanguageSelect)
                .setDisplayMapper(lng -> TextParam.text(getUserDisplayableString(lng, false, null)))
                .setTextDisplayMapper(lng -> TextParam.text(getUserDisplayableString(lng, true, null)))
                .setValues(targetLanguageList)
                .set(transCtx.getEffectiveTargetLanguage());

        final SimpleDialog dialog = SimpleDialog.ofContext(context)
            .setTitle(TextParam.text("Translation"))
            .setCustomView(binding)
            .setPositiveButton(TextParam.text("Translate"));

        final Consumer<Boolean> clickAction = enable -> {
            transCtx.set(sourceLanguage.get(), targetLanguage.get(), enable);
        };

        if (transCtx.isEnabled()) {
            dialog.setNeutralButton(TextParam.text("Turn Off"))
                .setNeutralAction(() -> transCtx.set(sourceLanguage.get(), targetLanguage.get(), false));
        }
        dialog.confirm(() -> {
            if (!TranslationModelManager.get().isAvailableOrPending(sourceLanguage.get()) ||
                !TranslationModelManager.get().isAvailableOrPending(targetLanguage.get())) {
                SimpleDialog.ofContext(context).setTitle(R.string.translator_model_download_confirm_title)
                    .setMessage(TextParam.id(R.string.translator_model_download_confirm_txt, String.join(sourceLanguage.get(), ", ", targetLanguage.get())))
                    .confirm(() -> {
                        TranslationModelManager.get().downloadLanguage(sourceLanguage.get());
                        TranslationModelManager.get().downloadLanguage(targetLanguage.get());
                        transCtx.set(sourceLanguage.get(), targetLanguage.get(), true);
                });

            } else {
                transCtx.set(sourceLanguage.get(), targetLanguage.get(), true);
            }
        });
    }

    private static Comparator<String> getLanguageSortComparator(final boolean forSource, final String selected, final String detected) {
        final String systemLanguage = getEffectiveTargetLanguage(null);
        return (lng1, lng2) -> {
            if (lng1.equals(lng2)) {
                return 0;
            }

            //Prio 1: selected
            if (lng1.equals(selected) || lng2.equals(selected)) {
                return lng1.equals(selected) ? -1 : 1;
            }
            //Prio 2 for source: detected
            if (forSource && (lng1.equals(detected) || lng2.equals(detected))) {
                return lng1.equals(detected) ? -1 : 1;
            }
            //Prio 3 for target: system language
            if (!forSource && (lng1.equals(systemLanguage) || lng2.equals(systemLanguage))) {
                return lng1.equals(systemLanguage) ? -1 : 1;
            }
            //Prio 4: availabililty
            if (TranslationModelManager.get().isAvailable(lng1) != TranslationModelManager.get().isAvailable(lng2)) {
                return TranslationModelManager.get().isAvailable(lng1) ? -1 : 1;
            }
            //Prio 5: pending
            if (TranslationModelManager.get().isPending(lng1) != TranslationModelManager.get().isPending(lng2)) {
                return TranslationModelManager.get().isPending(lng1) ? -1 : 1;
            }
            //Prio 6: alphabetically
            return lng1.compareTo(lng2);
        };
    }

    private static CharSequence getUserDisplayableString(final String language, final boolean doShort, final String detected) {
        if (language == null) {
            return "--";
        }

        final StringBuilder sb = new StringBuilder(LocalizationUtils.getLocaleDisplayName(new Locale(language), doShort, true));
        if (language.equals(detected)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.RED_FLAG));
        }
        if (TranslationModelManager.get().isAvailable(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GREEN_CHECK_BOXED));
        } else if (TranslationModelManager.get().isPending(language)) {
            sb.append(" ").append(EmojiUtils.getEmojiAsString(EmojiUtils.GRAY_CHECK_BOXED));
        }
        return sb.toString();
    }
}
