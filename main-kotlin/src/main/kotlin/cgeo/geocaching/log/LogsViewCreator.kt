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

package cgeo.geocaching.log

import cgeo.geocaching.ImageGalleryActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.TabbedViewPagerFragment
import cgeo.geocaching.databinding.LogsPageBinding
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.SmileyImage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod
import cgeo.geocaching.ui.DecryptTextClickListener
import cgeo.geocaching.ui.FastScrollListener
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.OfflineTranslateUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.utils.html.HtmlUtils
import cgeo.geocaching.utils.html.UnknownTagsHandler
import cgeo.geocaching.utils.offlinetranslate.TranslateAccessor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils

abstract class LogsViewCreator : TabbedViewPagerFragment()<LogsPageBinding> {
    public OfflineTranslateUtils.Status translationStatus = OfflineTranslateUtils.Status()

    override     public LogsPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return LogsPageBinding.inflate(inflater, container, false)
    }

    override     public Unit setContent() {
        if (!isValid()) {
            return
        }
        binding.getRoot().setVisibility(View.VISIBLE)

        addHeaderView()
        binding.logsItems.setAdapter(ArrayAdapter<LogEntry>(getActivity(), R.layout.logs_item, getLogs()) {

            override             public View getView(final Int position, final View convertView, final ViewGroup parent) {
                View rowView = convertView
                if (rowView == null) {
                    rowView = getActivity().getLayoutInflater().inflate(R.layout.logs_item, parent, false)
                }
                LogViewHolder holder = (LogViewHolder) rowView.getTag()
                if (holder == null) {
                    holder = LogViewHolder(rowView)
                }
                holder.setPosition(position)

                val log: LogEntry = getItem(position)
                if (log != null) {
                    fillViewHolder(convertView, holder, log)
                }
                return rowView
            }
        })
        binding.logsItems.setOnScrollListener(FastScrollListener(binding.logsItems))
    }

    protected Unit fillViewHolder(@SuppressWarnings("unused") final View convertView, final LogViewHolder holder, final LogEntry log) {
        if (log.date > 0) {
            holder.binding.added.setText(Formatter.formatShortDateVerbally(log.date))
            holder.binding.added.setVisibility(View.VISIBLE)
        } else {
            holder.binding.added.setVisibility(View.GONE)
        }

        holder.binding.type.setText(log.logType.getL10n())
        holder.binding.type.setCompoundDrawablesWithIntrinsicBounds(log.logType.getLogOverlay(), 0, 0, 0)
        holder.binding.type.setCompoundDrawablePadding(4)

        holder.binding.author.setText(StringEscapeUtils.unescapeHtml4(log.author))

        fillCountOrLocation(holder, log)

        // log text, avoid parsing HTML if not necessary
        if (TextUtils.containsHtml(log.log)) {
            val unknownTagsHandler: UnknownTagsHandler = UnknownTagsHandler()
            holder.binding.log.setText(TextUtils.trimSpanned(HtmlCompat.fromHtml(log.getDisplayText(), HtmlCompat.FROM_HTML_MODE_LEGACY, SmileyImage(getGeocode(), holder.binding.log), unknownTagsHandler)), TextView.BufferType.SPANNABLE)
        } else {
            holder.binding.log.setText(log.log, TextView.BufferType.SPANNABLE)
        }

        // images
        if (log.hasLogImages()) {
            holder.binding.logImages.setText(log.getImageTitles())
            holder.binding.logImages.setVisibility(View.VISIBLE)
            holder.binding.logImages.setOnClickListener(v -> ImageGalleryActivity.startActivity(getActivity(), getGeocode(), ArrayList<>(log.logImages)))
        } else {
            holder.binding.logImages.setVisibility(View.GONE)
        }

        // colored marker
        val marker: Int = log.logType.markerId
        if (marker != 0) {
            holder.binding.logMark.setVisibility(View.VISIBLE)
            holder.binding.logMark.setImageResource(marker)
        } else {
            holder.binding.logMark.setVisibility(View.GONE)
        }

        holder.binding.author.setOnClickListener(createUserActionsListener(log))
        holder.binding.log.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance())

        final View.OnClickListener logContextMenuClickListener = createOnLogClickListener(holder, log)

        holder.binding.log.setOnClickListener(logContextMenuClickListener)
        holder.binding.detailBox.setOnClickListener(logContextMenuClickListener)
    }

    protected View.OnClickListener createOnLogClickListener(final LogViewHolder holder, final LogEntry log) {
        return v -> {
            val activity: AbstractActionBarActivity = (AbstractActionBarActivity) getActivity()
            val author: String = StringEscapeUtils.unescapeHtml4(log.author)
            val title: String = LocalizationUtils.getString(R.string.cache_log_menu_popup_title, author)

            val ctxMenu: ContextMenuDialog = ContextMenuDialog(activity).setTitle(title)

            //Decrypt/encrypt
            ctxMenu.addItem(R.string.cache_log_menu_decrypt, R.drawable.ic_menu_rot13, DecryptTextClickListener(holder.binding.log))

            //Edit/Delete Log Entry
            if (!StringUtils.isBlank(getGeocode())) {
                val cache: Geocache = DataStore.loadCache(getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
                if (LogUtils.canEditLog(cache, log)) {
                    ctxMenu.addItem(R.string.cache_log_menu_edit, R.drawable.ic_menu_edit,
                        it -> LogUtils.startEditLog(activity, cache, log))
                }
                if (LogUtils.canDeleteLog(cache, log)) {
                    ctxMenu.addItem(R.string.cache_log_menu_delete, R.drawable.ic_menu_delete,
                        it -> LogActivityHelper(activity)
                            .setLogResultConsumer((type, result) -> {
                                if (!result.isOk()) {
                                    SimpleDialog.of(activity)
                                        .setTitle(R.string.info_log_delete_failed)
                                        .setMessage(TextParam.id(R.string.info_log_delete_failed_simple_reason))
                                        .show()
                                }
                            })
                            .deleteLog(cache, log))
                }

            }

            // expand/collapse
            if (holder.binding.log.isCollapsible()) {
                ctxMenu.addItem(holder.binding.log.isCollapsed() ? R.string.menu_expand_log : R.string.menu_collapse_log, holder.binding.log.isCollapsed() ? R.drawable.expand_less : R.drawable.expand_more, it -> holder.binding.log.setCollapse(!holder.binding.log.isCollapsed()))
            }

            //Copy to clipboard
            ctxMenu.addItem(LocalizationUtils.getString(R.string.copy_to_clipboard), R.drawable.ic_menu_copy, i -> {
                ClipboardUtils.copyToClipboard(holder.binding.log.getText().toString())
                    activity.showToast(activity.getString(R.string.clipboard_copy_ok))
            })

            // translation
            if (TranslationUtils.isEnabled()) {
                ctxMenu.addItem(TranslationUtils.getTranslationLabel().toString(), R.drawable.ic_menu_translate, it ->
                    TranslationUtils.translate(activity, TranslationUtils.prepareForTranslation(log.log)))
            }
            if (OfflineTranslateUtils.isTargetLanguageValid() && !TranslateAccessor.get().getSupportedLanguages().isEmpty()) {
                ctxMenu.addItem(R.string.translator_tooltip, R.drawable.ic_menu_translate, it -> {
                    if (translationStatus.isTranslated()) {
                        translationStatus.setNotTranslated()
                        fillViewHolder(null, holder, log)
                    } else {
                        val logText: String = HtmlUtils.extractText(log.log)
                        translationStatus.startTranslation(1, null, null)
                        OfflineTranslateUtils.translateTextAutoDetectLng(getActivity(), translationStatus, logText,
                                unsupportedLng -> Toast.makeText(getContext(), getString(R.string.translator_language_unsupported, unsupportedLng), Toast.LENGTH_LONG).show(),
                                downloadingModel -> Toast.makeText(getContext(), R.string.translator_model_download_notification, Toast.LENGTH_SHORT).show(),
                                translator -> OfflineTranslateUtils.translateParagraph(translator, translationStatus, logText, holder.binding.log::setText, e -> Toast.makeText(getContext(), getString(R.string.translator_translation_error, e.getMessage()), Toast.LENGTH_LONG).show()))
                        }
                })
            }

            // share
            ctxMenu.addItem(R.string.context_share_as_text, R.drawable.ic_menu_share, it ->
                    ShareUtils.sharePlainText(activity, holder.binding.log.getText().toString()))

            // subclass specific entries
            extendContextMenu(ctxMenu, log).show()
        }

    }

    /**
     * for subclasses to overwrite and add own entries
     */
    protected ContextMenuDialog extendContextMenu(final ContextMenuDialog ctxMenu, final LogEntry log) {
        return ctxMenu
    }

    protected abstract View.OnClickListener createUserActionsListener(LogEntry log)

    protected abstract String getGeocode()

    protected abstract List<LogEntry> getLogs()

    protected abstract Unit addHeaderView()

    protected abstract Unit fillCountOrLocation(LogViewHolder holder, LogEntry log)

    protected abstract Boolean isValid()


}
