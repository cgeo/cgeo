package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.TabbedViewPagerActivity;
import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.databinding.AboutChangesPageBinding;
import cgeo.geocaching.databinding.AboutContributorsPageBinding;
import cgeo.geocaching.databinding.AboutLicensePageBinding;
import cgeo.geocaching.databinding.AboutSystemPageBinding;
import cgeo.geocaching.databinding.AboutVersionPageBinding;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.BranchDetectionHelper;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.SystemInformation;
import cgeo.geocaching.utils.Version;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.util.Consumer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import io.noties.markwon.Markwon;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class AboutActivity extends TabbedViewPagerActivity {

    private static final String EXTRA_ABOUT_STARTPAGE = "cgeo.geocaching.extra.about.startpage";

    private final GatherSystemInformationTask systemInformationTask = new GatherSystemInformationTask();

    enum Page {
        VERSION(R.string.about_version),
        CHANGELOG(R.string.about_changelog),
        SYSTEM(R.string.about_system),
        CONTRIBUTORS(R.string.about_contributors),
        LICENSE(R.string.about_license);

        @StringRes
        protected final int resourceId;
        protected final long id;

        Page(@StringRes final int resourceId) {
            this.resourceId = resourceId;
            this.id = ordinal();
        }

        static Page find(final long pageId) {
            for (Page page : Page.values()) {
                if (page.id == pageId) {
                    return page;
                }
            }
            return null;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Routing.connect();
        systemInformationTask.execute();

        int startPage = 0;
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            startPage = (int) extras.getLong(EXTRA_ABOUT_STARTPAGE, startPage);
        }

        final Page[] pages = Page.values();
        final long[] orderedPages = new long[pages.length];
        for (int i = 0; i < pages.length; i++) {
            orderedPages[i] = pages[i].id;
        }

        createViewPager(startPage, orderedPages, this::onPageChangeListener, false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        setActionBarTitle(getCurrentPageId()); // to avoid race conditions on first view creation
        return super.onCreateOptionsMenu(menu);
    }

    private void onPageChangeListener(final long currentPageId) {
        setActionBarTitle(currentPageId);
    }

    private void setActionBarTitle(final long currentPageId) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            final String prefix = getString(R.string.about);
            actionBar.setTitle((StringUtils.isNotBlank(prefix) ? prefix + " - " : "") + getTitle(currentPageId));
        }
    }

    @Override
    protected String getTitle(final long pageId) {
        if (pageId == Page.VERSION.id) {
            return getResources().getString(R.string.about_version) + " / " + getResources().getString(R.string.about_help);
        }
        return this.getString(Page.find(pageId).resourceId);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected TabbedViewPagerFragment createNewFragment(final long pageId) {
        if (pageId == Page.VERSION.id) {
            return new VersionViewCreator();
        } else if (pageId == Page.CHANGELOG.id) {
            return new ChangeLogViewCreator();
        } else if (pageId == Page.SYSTEM.id) {
            return new SystemViewCreator();
        } else if (pageId == Page.LICENSE.id) {
            return new LicenseViewCreator();
        } else if (pageId == Page.CONTRIBUTORS.id) {
            return new ContributorsViewCreator();
        }
        throw new IllegalStateException(); // cannot happen, when switch case is enum complete
    }

    public static class VersionViewCreator extends TabbedViewPagerFragment<AboutVersionPageBinding> {

        @Override
        public AboutVersionPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutVersionPageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.VERSION.id;
        }

        @Override
        public void setContent() {
            final AboutActivity activity = (AboutActivity) getActivity();
            if (activity == null || binding == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);
            binding.aboutVersionString.setText(Version.getVersionName(activity));
            setClickListener(binding.donate, "https://www.cgeo.org");
            if (StringUtils.isNotEmpty(BuildConfig.SPECIAL_BUILD)) {
                binding.aboutSpecialBuild.setText(BuildConfig.SPECIAL_BUILD);
                binding.aboutSpecialBuild.setVisibility(View.VISIBLE);
            }
            if (StringUtils.isNotEmpty(BuildConfig.BUILD_TYPE)) {
                //noinspection ConstantConditions
                if (BuildConfig.BUILD_TYPE.equals("debug")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_debug);
                } else if (BuildConfig.BUILD_TYPE.equals("nightly")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_nightly);
                } else if (BuildConfig.BUILD_TYPE.equals("rc")) {
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_rc);
                }
            }
            binding.support.setEnabled(false);

            activity.getSystemInformationTask().getSystemInformation(si -> {
                setClickListener(binding.support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(activity)) +
                    "&body=" + Uri.encode(si) + "\n");
                binding.support.setEnabled(true);
            });

            setClickListener(binding.website, "https://www.cgeo.org/");
            setClickListener(binding.facebook, "https://www.facebook.com/pages/cgeo/297269860090");
            setClickListener(binding.fangroup, "https://facebook.com/groups/cgeo.fangruppe");
            setClickListener(binding.twitter, "https://twitter.com/android_gc");
            setClickListener(binding.nutshellmanual, "https://manual.cgeo.org/");
            setClickListener(binding.faq, "https://faq.cgeo.org/");
            setClickListener(binding.github, "https://github.com/cgeo/cgeo/issues");
            binding.market.setOnClickListener(v -> ProcessUtils.openMarket(activity, activity.getPackageName()));
        }
    }

    public static class ChangeLogViewCreator extends TabbedViewPagerFragment<AboutChangesPageBinding> {

        @Override
        public AboutChangesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutChangesPageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.CHANGELOG.id;
        }

        @Override
        public void setContent() {
            final Activity activity = getActivity();
            if (activity == null || binding == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);
            final Markwon markwon = Markwon.create(activity);

            final String changelogBase = FileUtils.getChangelogMaster(activity).trim();
            final String changelogBugfix = FileUtils.getChangelogRelease(activity);
            Log.e("build info: bt=" + BuildConfig.BUILD_TYPE + ", pb=" + BranchDetectionHelper.isProductionBuild() + ", start(cl)=" + changelogBugfix.substring(0,5) + ", sw##=" + changelogBugfix.startsWith("##"));
            if (BranchDetectionHelper.isProductionBuild()) {
                // we are on release branch
                if (StringUtils.isNotEmpty(changelogBugfix)) {
                    markwon.setMarkdown(binding.changelogMaster, (changelogBugfix.startsWith("##") ? "" : "## " + getString(R.string.about_changelog_next_release) + "\n\n") + changelogBugfix);
                } else {
                    binding.changelogMaster.setVisibility(View.GONE);
                }
                markwon.setMarkdown(binding.changelogRelease, "## " + BranchDetectionHelper.FEATURE_VERSION_NAME + "\n\n" + changelogBase);
            } else {
                // we are on a non-release branch
                markwon.setMarkdown(binding.changelogMaster, "## " + getString(R.string.about_changelog_nightly_build) + "\n\n" + changelogBase);
                markwon.setMarkdown(binding.changelogRelease, changelogBugfix);
            }
            binding.changelogGithub.setOnClickListener(v -> ShareUtils.openUrl(activity, "https://github.com/cgeo/cgeo/blob/master/main/res/raw/changelog_full.md"));
        }
    }

    public static class SystemViewCreator extends TabbedViewPagerFragment<AboutSystemPageBinding> {

        @Override
        public AboutSystemPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutSystemPageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.SYSTEM.id;
        }

        @Override
        public void setContent() {
            final AboutActivity activity = (AboutActivity) getActivity();
            if (activity == null || binding == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            binding.system.setText(R.string.about_system_collecting);
            binding.copy.setEnabled(false);
            binding.share.setEnabled(false);

            activity.getSystemInformationTask().getSystemInformation(si -> {
                final Markwon markwon = Markwon.create(activity);
                markwon.setMarkdown(binding.system, si);
                binding.copy.setEnabled(true);
                binding.copy.setOnClickListener(view1 -> {
                    ClipboardUtils.copyToClipboard(si);
                    ActivityMixin.showShortToast(activity, getString(R.string.clipboard_copy_ok));
                });
                binding.share.setEnabled(true);
                binding.share.setOnClickListener(view12 -> ShareUtils.shareAsEmail(activity, getString(R.string.about_system_info), si, null, R.string.about_system_info_send_chooser));
            });

            binding.system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            binding.system.setTextIsSelectable(true);

            binding.logcat.setOnClickListener(view13 -> DebugUtils.createLogcat(activity));
        }
    }

    public static class LicenseViewCreator extends TabbedViewPagerFragment<AboutLicensePageBinding> {

        @Override
        public AboutLicensePageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutLicensePageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.LICENSE.id;
        }

        @Override
        public void setContent() {
            if (binding == null || getActivity() == null) {
                return;
            }

            binding.getRoot().setVisibility(View.VISIBLE);
            setClickListener(binding.license, "https://www.apache.org/licenses/LICENSE-2.0.html");
            final Markwon markwon = Markwon.create(getActivity());
            markwon.setMarkdown(binding.licenseText, getRawResourceString(R.raw.license));
        }

        private String getRawResourceString(@SuppressWarnings("SameParameterValue") @RawRes final int resourceId) {
            InputStream ins = null;
            Scanner scanner = null;
            try {
                ins = getResources().openRawResource(resourceId);
                scanner = new Scanner(ins, StandardCharsets.UTF_8.name());
                return scanner.useDelimiter("\\A").next();
            } finally {
                IOUtils.closeQuietly(ins);
                // Scanner does not implement Closeable on Android 4.1, so closeQuietly leads to crash there
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    public static class ContributorsViewCreator extends TabbedViewPagerFragment<AboutContributorsPageBinding> {

        @Override
        public AboutContributorsPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            return AboutContributorsPageBinding.inflate(getLayoutInflater(), container, false);
        }

        @Override
        public long getPageId() {
            return Page.CONTRIBUTORS.id;
        }

        @Override
        public void setContent() {
            final Activity activity = getActivity();
            if (activity == null || binding == null) {
                return;
            }
            binding.getRoot().setVisibility(View.VISIBLE);

            final Markwon markwon = Markwon.create(activity);

            markwon.setMarkdown(binding.aboutContributorsRecent, formatContributors(R.string.contributors_recent));
            markwon.setMarkdown(binding.aboutContributorsOthers, formatContributors(R.string.contributors_other));
            markwon.setMarkdown(binding.aboutSpecialthanksdetails, getString(R.string.about_contributors_specialthanksdetails));

            final String indentedList = "   " + getString(R.string.components2).replace("\n", "\n  ");
            markwon.setMarkdown(binding.aboutComponents, getString(R.string.components).replace("%1", indentedList.substring(0, indentedList.length() - 2)));
        }

        private String checkRoles(final String s, final String roles, final char checkFor, final int infoId) {
            return roles.indexOf(checkFor) >= 0 ? (s.isEmpty() ? "" : s + ", ") + getString(infoId) : s;
        }

        private String formatContributors(@StringRes final int resId) {
            String s = getString(resId);
            final SpannableStringBuilder sb = new SpannableStringBuilder();
            int p1;
            int p2;
            int p3;
            String name;
            String link;
            String roles;

            p1 = s.indexOf("|");
            if (p1 >= 0) {
                do {
                    name = s.substring(0, p1).trim();
                    p2 = s.indexOf("|", p1 + 1);
                    if (p2 < 0) {
                        break;
                    }
                    link = s.substring(p1 + 1, p2).trim();
                    p3 = s.indexOf("|", p2 + 1);
                    if (p3 < 0) {
                        break;
                    }
                    final String temp = s.substring(p2 + 1, p3);
                    roles = checkRoles(checkRoles(checkRoles(checkRoles(checkRoles(checkRoles(checkRoles("",
                        temp, 'c', R.string.contribution_code),
                        temp, 'd', R.string.contribution_documentation),
                        temp, 'g', R.string.contribution_graphics),
                        temp, 'i', R.string.contribution_infrastructure),
                        temp, 'p', R.string.contribution_projectleader),
                        temp, 's', R.string.contribution_support),
                        temp, 't', R.string.contribution_tester);

                    sb.append("- ")
                        .append(link.isEmpty() ? name : "[" + name + "](" + link + ")")
                        .append(roles.isEmpty() ? "" : " (" + roles + ")")
                        .append("\n");

                    s = s.substring(p3 + 1);
                    p1 = s.indexOf("|");
                } while (p1 > 0);
            }
            return sb.toString();
        }
    }

    protected static class GatherSystemInformationTask extends AsyncTask<Void, Void, String> {

        private final Object mutex = new Object();
        private String systemInformation = null;
        private final List<Consumer<String>> consumers = new ArrayList<>();

        public void getSystemInformation(final Consumer<String> callback) {
            synchronized (mutex) {
                if (systemInformation != null) {
                    callback.accept(systemInformation);
                    return;
                }
                consumers.add(callback);
            }
        }

        public void onDestroy() {
            synchronized (mutex) {
                consumers.clear();
                systemInformation = null;
            }
        }

        @Override
        protected String doInBackground(final Void[] params) {
            final Context context = CgeoApplication.getInstance().getApplicationContext();
            return SystemInformation.getSystemInformation(context);
        }

        @Override
        protected void onPostExecute(final String systemInformation) {
            Log.d("SystemInformation returned");
            synchronized (mutex) {
                this.systemInformation = systemInformation;
                for (Consumer<String> consumer : consumers) {
                    consumer.accept(this.systemInformation);
                }
                consumers.clear();
            }
        }
    }

    public static void showChangeLog(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, AboutActivity.class);
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.CHANGELOG.id);
        fromActivity.startActivity(intent);
    }

    protected GatherSystemInformationTask getSystemInformationTask() {
        return systemInformationTask;
    }

    @Override
    protected void onDestroy() {
        Routing.disconnect();
        super.onDestroy();
        systemInformationTask.onDestroy();
    }

}
