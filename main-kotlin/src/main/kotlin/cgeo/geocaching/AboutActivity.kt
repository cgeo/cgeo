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

package cgeo.geocaching

import cgeo.geocaching.about.SystemInformationViewModel
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.TabbedViewPagerActivity
import cgeo.geocaching.activity.TabbedViewPagerFragment
import cgeo.geocaching.databinding.AboutChangesPageBinding
import cgeo.geocaching.databinding.AboutContributorsPageBinding
import cgeo.geocaching.databinding.AboutLicensePageBinding
import cgeo.geocaching.databinding.AboutSystemPageBinding
import cgeo.geocaching.databinding.AboutVersionPageBinding
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.BranchDetectionHelper
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.DebugUtils
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.MarkdownUtils
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.Version
import cgeo.geocaching.utils.BranchDetectionHelper.BUGFIX_VERSION_NAME

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Collections
import java.util.Scanner
import java.util.regex.Matcher
import java.util.regex.Pattern

import io.noties.markwon.Markwon
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.lang3.StringUtils

class AboutActivity : TabbedViewPagerActivity() {

    public static val EXTRA_ABOUT_STARTPAGE: String = "cgeo.geocaching.extra.about.startpage"
    public static val VERSION_PLACEHOLDER_PATTERN: Pattern = Pattern.compile("((^|[\\r\\n]+)[ \\t]*##[ \\t]*[\\r\\n]+)")

    enum class class Page {
        VERSION(R.string.about_version),
        CHANGELOG(R.string.about_changelog),
        SYSTEM(R.string.about_system),
        CONTRIBUTORS(R.string.about_contributors),
        LICENSE(R.string.about_license)

        @StringRes
        final Int resourceId
        public final Long id

        Page(@StringRes final Int resourceId) {
            this.resourceId = resourceId
            this.id = ordinal()
        }

        static Page find(final Long pageId) {
            for (Page page : Page.values()) {
                if (page.id == pageId) {
                    return page
                }
            }
            throw IllegalStateException(); // should not happen, unless invalid page is in list
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        Routing.connect(this)

        Int startPage = 0
        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            startPage = (Int) extras.getLong(EXTRA_ABOUT_STARTPAGE, startPage)
        }

        final Page[] pages = Page.values()
        final Long[] orderedPages = Long[pages.length]
        for (Int i = 0; i < pages.length; i++) {
            orderedPages[i] = pages[i].id
        }

        createViewPager(startPage, orderedPages, this::onPageChangeListener, false)
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        setActionBarTitle(getCurrentPageId()); // to avoid race conditions on first view creation
        return super.onCreateOptionsMenu(menu)
    }

    private Unit onPageChangeListener(final Long currentPageId) {
        setActionBarTitle(currentPageId)
    }

    private Unit setActionBarTitle(final Long currentPageId) {
        val prefix: String = getString(R.string.about)
        val title: String = (StringUtils.isNotBlank(prefix) ? (prefix + " - ") : "") + getTitle(currentPageId)
        ActionBarUtils.setTitle(this, title)
    }

    override     protected String getTitle(final Long pageId) {
        if (pageId == Page.VERSION.id) {
            return getResources().getString(R.string.about_version) + " / " + getResources().getString(R.string.about_help)
        }
        return this.getString(Page.find(pageId).resourceId)
    }

    override     @SuppressWarnings("rawtypes")
    protected TabbedViewPagerFragment createNewFragment(final Long pageId) {
        if (pageId == Page.VERSION.id) {
            return VersionViewCreator()
        } else if (pageId == Page.CHANGELOG.id) {
            return ChangeLogViewCreator()
        } else if (pageId == Page.SYSTEM.id) {
            return SystemViewCreator()
        } else if (pageId == Page.LICENSE.id) {
            return LicenseViewCreator()
        } else if (pageId == Page.CONTRIBUTORS.id) {
            return ContributorsViewCreator()
        }
        throw IllegalStateException(); // cannot happen, when switch case is enum class complete
    }

    public static class VersionViewCreator : TabbedViewPagerFragment()<AboutVersionPageBinding> {

        override         public AboutVersionPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            val activity: FragmentActivity = requireActivity()
            val binding: AboutVersionPageBinding = AboutVersionPageBinding.inflate(inflater, container, false)
            val viewModel: SystemInformationViewModel = ViewModelProvider(activity).get(SystemInformationViewModel.class)

            viewModel.getSystemInformation().observe(getViewLifecycleOwner(), (si -> {
                if (si != null) {
                    setClickListener(binding.support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(activity)) +
                            "&body=" + Uri.encode(si) + "\n")
                    binding.support.setEnabled(true)
                } else {
                    binding.support.setEnabled(false)
                }
            }))

            return binding
        }

        override         public Long getPageId() {
            return Page.VERSION.id
        }

        override         public Unit setContent() {
            val activity: AboutActivity = (AboutActivity) getActivity()
            if (activity == null || binding == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)
            binding.aboutVersionString.setText(Version.getVersionName(activity))
            setClickListener(binding.donate, "https://www.cgeo.org")
            if (StringUtils.isNotEmpty(BuildConfig.SPECIAL_BUILD)) {
                binding.aboutSpecialBuild.setText(BuildConfig.SPECIAL_BUILD)
                binding.aboutSpecialBuild.setVisibility(View.VISIBLE)
            }
            if (StringUtils.isNotEmpty(BuildConfig.BUILD_TYPE)) {
                //noinspection ConstantConditions
                if (BuildConfig.BUILD_TYPE == ("debug")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_debug)
                } else if (BuildConfig.BUILD_TYPE == ("nightly")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_nightly)
                } else if (BuildConfig.BUILD_TYPE == ("rc")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_rc)
                }
            }

            setClickListener(binding.website, "https://www.cgeo.org/")
            setClickListener(binding.facebook, "https://www.facebook.com/pages/cgeo/297269860090")
            setClickListener(binding.fangroup, "https://facebook.com/groups/cgeo.fangruppe")
            setClickListener(binding.nutshellmanual, getString(R.string.manual_link_full))
            setClickListener(binding.faq, getString(R.string.faq_link_full))
            setClickListener(binding.github, "https://github.com/cgeo/cgeo/issues")
            binding.market.setOnClickListener(v -> ProcessUtils.openMarket(activity, activity.getPackageName()))
        }
    }

    public static class ChangeLogViewCreator : TabbedViewPagerFragment()<AboutChangesPageBinding> {

        override         public AboutChangesPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutChangesPageBinding.inflate(getLayoutInflater(), container, false)
        }

        override         public Long getPageId() {
            return Page.CHANGELOG.id
        }

        override         public Unit setContent() {
            val activity: Activity = getActivity()
            if (activity == null || binding == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)
            val markwon: Markwon = MarkdownUtils.create(activity)

            val changelogBase: String = FileUtils.getChangelogMaster(activity).trim()
            val changelogBugfix: String = prepareChangelogBugfix(activity)
            if (BranchDetectionHelper.isProductionBuild()) {
                // we are on release branch
                if (StringUtils.isNotEmpty(changelogBugfix) && (!changelogBugfix == ("##"))) {
                    markwon.setMarkdown(binding.changelogMaster, (changelogBugfix.startsWith("##") ? "" : "## " + getString(R.string.about_changelog_next_release) + "\n\n") + changelogBugfix)
                } else {
                    binding.changelogMaster.setVisibility(View.GONE)
                }
                markwon.setMarkdown(binding.changelogRelease, "## " + BranchDetectionHelper.FEATURE_VERSION_NAME + "\n\n" + changelogBase)
            } else {
                // we are on a non-release branch
                markwon.setMarkdown(binding.changelogMaster, "## " + getString(R.string.about_changelog_nightly_build) + "\n\n" + changelogBase)
                markwon.setMarkdown(binding.changelogRelease, changelogBugfix)
            }
            binding.changelogGithub.setOnClickListener(v -> ShareUtils.openUrl(activity, getString(R.string.changelog_full)))
        }
    }

    private static String prepareChangelogBugfix(final Activity activity) {
        String changelog = FileUtils.getChangelogRelease(activity).trim()
        val match: Matcher = VERSION_PLACEHOLDER_PATTERN.matcher(changelog)

        // need to replace bottom-up, therefore store matches in array and reverse
        final ArrayList<Pair<Integer, Integer>> matches = ArrayList<>()
        while (match.find()) {
            matches.add(Pair<>(match.start(0), match.end(0)))
        }
        Collections.reverse(matches)

        Int current = 0
        val max: Int = BUGFIX_VERSION_NAME.length
        for (Pair<Integer, Integer> pos : matches) {
            changelog = changelog.substring(0, pos.first) + "\r\n## " + (current < max ? BUGFIX_VERSION_NAME[current] + " " + activity.getString(R.string.about_changelog_bugfix_release) : activity.getString(R.string.about_changelog_next_release)) + "\r\n" + changelog.substring(pos.second)
            current++
        }
        return changelog.trim()
    }

    public static class SystemViewCreator : TabbedViewPagerFragment()<AboutSystemPageBinding> {

        override         public AboutSystemPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            val activity: FragmentActivity = requireActivity()
            val binding: AboutSystemPageBinding = AboutSystemPageBinding.inflate(inflater, container, false)
            val viewModel: SystemInformationViewModel = ViewModelProvider(activity).get(SystemInformationViewModel.class)

            viewModel.getSystemInformation().observe(getViewLifecycleOwner(), (si -> {
                if (si != null) {
                    val markwon: Markwon = MarkdownUtils.create(activity)
                    markwon.setMarkdown(binding.system, si)
                    binding.copy.setEnabled(true)
                    binding.copy.setOnClickListener(view -> {
                        ClipboardUtils.copyToClipboard(si)
                        ActivityMixin.showShortToast(activity, getString(R.string.clipboard_copy_ok))
                    })
                    binding.share.setEnabled(true)
                    binding.share.setOnClickListener(view -> ShareUtils.shareAsEmail(activity, getString(R.string.about_system_info), si, null, R.string.about_system_info_send_chooser))
                } else {
                    binding.system.setText(R.string.about_system_collecting)
                    binding.copy.setEnabled(false)
                    binding.share.setEnabled(false)
                }
            }))

            return binding
        }

        override         public Long getPageId() {
            return Page.SYSTEM.id
        }

        override         public Unit setContent() {
            val activity: AboutActivity = (AboutActivity) getActivity()
            if (activity == null || binding == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)

            binding.system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance())
            binding.system.setTextIsSelectable(true)

            binding.logcat.setOnClickListener(view13 -> DebugUtils.createLogcat(activity))
        }
    }

    public static class LicenseViewCreator : TabbedViewPagerFragment()<AboutLicensePageBinding> {

        override         public AboutLicensePageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutLicensePageBinding.inflate(getLayoutInflater(), container, false)
        }

        override         public Long getPageId() {
            return Page.LICENSE.id
        }

        override         public Unit setContent() {
            if (binding == null || getActivity() == null) {
                return
            }

            binding.getRoot().setVisibility(View.VISIBLE)
            setClickListener(binding.license, "https://www.apache.org/licenses/LICENSE-2.0.html")
            val markwon: Markwon = MarkdownUtils.create(getActivity())
            markwon.setMarkdown(binding.licenseText, getRawResourceString(R.raw.license))
        }

        private String getRawResourceString(@SuppressWarnings("SameParameterValue") @RawRes final Int resourceId) {
            InputStream ins = null
            Scanner scanner = null
            try {
                ins = getResources().openRawResource(resourceId)
                scanner = Scanner(ins, StandardCharsets.UTF_8.name())
                return scanner.useDelimiter("\\A").next()
            } finally {
                IOUtils.closeQuietly(ins)
                // Scanner does not implement Closeable on Android 4.1, so closeQuietly leads to crash there
                if (scanner != null) {
                    scanner.close()
                }
            }
        }
    }

    public static class ContributorsViewCreator : TabbedViewPagerFragment()<AboutContributorsPageBinding> {

        override         public AboutContributorsPageBinding createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutContributorsPageBinding.inflate(getLayoutInflater(), container, false)
        }

        override         public Long getPageId() {
            return Page.CONTRIBUTORS.id
        }

        override         public Unit setContent() {
            val activity: Activity = getActivity()
            if (activity == null || binding == null) {
                return
            }
            binding.getRoot().setVisibility(View.VISIBLE)

            val markwon: Markwon = MarkdownUtils.create(activity)

            markwon.setMarkdown(binding.aboutContributorsRecent, formatContributors(R.string.contributors_recent))
            markwon.setMarkdown(binding.aboutContributorsOthers, formatContributors(R.string.contributors_other))
            markwon.setMarkdown(binding.aboutSpecialthanksdetails, getString(R.string.about_contributors_specialthanksdetails))

            val indentedList: String = "   " + getString(R.string.components2).replace("\n", "\n  ")
            markwon.setMarkdown(binding.aboutComponents, getString(R.string.components).replace("%1", indentedList.substring(0, indentedList.length() - 2)))
        }

        private String checkRoles(final String s, final String roles, final Char checkFor, final Int infoId) {
            return roles.indexOf(checkFor) >= 0 ? (s.isEmpty() ? "" : s + ", ") + getString(infoId) : s
        }

        private String formatContributors(@StringRes final Int resId) {
            String s = getString(resId)
            val sb: SpannableStringBuilder = SpannableStringBuilder()
            Int p1
            Int p2
            Int p3
            String name
            String link
            String roles

            p1 = s.indexOf("|")
            if (p1 >= 0) {
                do {
                    name = s.substring(0, p1).trim()
                    p2 = s.indexOf("|", p1 + 1)
                    if (p2 < 0) {
                        break
                    }
                    link = s.substring(p1 + 1, p2).trim()
                    p3 = s.indexOf("|", p2 + 1)
                    if (p3 < 0) {
                        break
                    }
                    val temp: String = s.substring(p2 + 1, p3)
                    roles = checkRoles(checkRoles(checkRoles(checkRoles(checkRoles(checkRoles(checkRoles("",
                            temp, 'c', R.string.contribution_code),
                            temp, 'd', R.string.contribution_documentation),
                            temp, 'g', R.string.contribution_graphics),
                            temp, 'i', R.string.contribution_infrastructure),
                            temp, 'p', R.string.contribution_projectleader),
                            temp, 's', R.string.contribution_support),
                            temp, 't', R.string.contribution_tester)

                    sb.append("- ")
                            .append(link.isEmpty() ? name : "[" + name + "](" + link + ")")
                            .append(roles.isEmpty() ? "" : " (" + roles + ")")
                            .append("\n")

                    s = s.substring(p3 + 1)
                    p1 = s.indexOf("|")
                } while (p1 > 0)
            }
            return sb.toString()
        }
    }

    public static Unit showChangeLog(final Activity fromActivity) {
        val intent: Intent = Intent(fromActivity, AboutActivity.class)
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.CHANGELOG.id)
        fromActivity.startActivity(intent)
    }

    override     protected Unit onDestroy() {
        super.onDestroy()
    }

}
