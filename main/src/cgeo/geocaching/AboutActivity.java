package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.databinding.AboutChangesPageBinding;
import cgeo.geocaching.databinding.AboutContributorsPageBinding;
import cgeo.geocaching.databinding.AboutLicensePageBinding;
import cgeo.geocaching.databinding.AboutSystemPageBinding;
import cgeo.geocaching.databinding.AboutVersionPageBinding;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.LiUtils;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Consumer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AboutActivity extends AbstractViewPagerActivity<AboutActivity.Page> {

    private static final String EXTRA_ABOUT_STARTPAGE = "cgeo.geocaching.extra.about.startpage";

    private final GatherSystemInformationTask systemInformationTask = new GatherSystemInformationTask();

    class LicenseViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final AboutLicensePageBinding binding = AboutLicensePageBinding.inflate(getLayoutInflater(), parentView, false);
            setClickListener(binding.license, "https://www.apache.org/licenses/LICENSE-2.0.html");
            binding.licenseText.setText(getRawResourceString(R.raw.license));
            return binding.getRoot();
        }

        private String getRawResourceString(@SuppressWarnings("SameParameterValue") @RawRes final int resourceId) {
            InputStream ins = null;
            Scanner scanner = null;
            try {
                ins = res.openRawResource(resourceId);
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

    class ContributorsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final AboutContributorsPageBinding binding = AboutContributorsPageBinding.inflate(getLayoutInflater(), parentView, false);

            setText(binding.aboutContributorsRecent, R.string.contributors_recent);
            setText(binding.aboutContributorsOthers, R.string.contributors_other);

            final AnchorAwareLinkMovementMethod mm = AnchorAwareLinkMovementMethod.getInstance();
            binding.aboutCarnerodetails.setMovementMethod(mm);
            binding.aboutContributorsRecent.setMovementMethod(mm);
            binding.aboutSpecialthanksdetails.setMovementMethod(mm);
            binding.aboutContributorsOthers.setMovementMethod(mm);
            binding.aboutComponents.setMovementMethod(mm);

            return binding.getRoot();
        }

        private String checkRoles(final String s, final String roles, final char checkFor, final int infoId) {
            return roles.indexOf(checkFor) >= 0 ? (s.isEmpty() ? "" : s + ", ") + getString(infoId) : s;
        }

        private void setText(final TextView t, final int resId) {
            String s = getString(resId);
            final SpannableStringBuilder sb = new SpannableStringBuilder("<ul>");
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
                    roles = checkRoles(checkRoles(checkRoles(checkRoles(checkRoles(checkRoles("",
                        temp, 'c', R.string.contribution_code),
                        temp, 'd', R.string.contribution_documentation),
                        temp, 'g', R.string.contribution_graphics),
                        temp, 'p', R.string.contribution_projectleader),
                        temp, 's', R.string.contribution_support),
                        temp, 't', R.string.contribution_tester);

                    sb.append("· ")
                        .append(link.isEmpty() ? name : "<a href=\"" + link + "\">" + name + "</a>")
                        .append(roles.isEmpty() ? "" : " (" + roles + ")")
                        .append("<br />");

                    s = s.substring(p3 + 1);
                    p1 = s.indexOf("|");
                } while (p1 > 0);
            }
            sb.append("</ul>");

            t.setText(HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    class ChangeLogViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final AboutChangesPageBinding binding = AboutChangesPageBinding.inflate(getLayoutInflater(), parentView, false);
            binding.changelogRelease.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            final String changeLogMasterString = getString(R.string.changelog_master);
            if (StringUtils.isBlank(changeLogMasterString)) {
                binding.changelogMaster.setVisibility(View.GONE);
            } else {
                binding.changelogMaster.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            }
            binding.changelogGithub.setOnClickListener(v -> ShareUtils.openUrl(AboutActivity.this, "https://github.com/cgeo/cgeo/releases"));
            binding.changelogMaster.setText(LiUtils.formatHTML(getString(R.string.changelog_master)));
            binding.changelogRelease.setText(LiUtils.formatHTML(getString(R.string.changelog_release)));

            return binding.getRoot();
        }
    }

    class SystemViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final AboutSystemPageBinding binding = AboutSystemPageBinding.inflate(getLayoutInflater(), parentView, false);
            binding.system.setText(R.string.about_system_collecting);
            binding.copy.setEnabled(false);
            binding.share.setEnabled(false);
            systemInformationTask.getSystemInformation(si -> {
                    binding.system.setText(si);
                    binding.copy.setEnabled(true);
                    binding.copy.setOnClickListener(view1 -> {
                        ClipboardUtils.copyToClipboard(si);
                        showShortToast(getString(R.string.clipboard_copy_ok));
                    });
                    binding.share.setEnabled(true);
                    binding.share.setOnClickListener(view12 -> ShareUtils.shareAsEmail(AboutActivity.this, getString(R.string.about_system_info), si, null, R.string.about_system_info_send_chooser));

            });
            binding.system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            binding.system.setTextIsSelectable(true);

            binding.logcat.setOnClickListener(view13 -> DebugUtils.createLogcat(AboutActivity.this));
            return binding.getRoot();
        }
    }

    class VersionViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final AboutVersionPageBinding binding = AboutVersionPageBinding.inflate(getLayoutInflater(), parentView, false);
            binding.aboutVersionString.setText(Version.getVersionName(AboutActivity.this));
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
                    binding.aboutVersionIcon.setImageResource(R.mipmap.ic_launcher_beta);
                }
            }
            binding.support.setEnabled(false);
            systemInformationTask.getSystemInformation(si -> {
                setClickListener(binding.support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(AboutActivity.this)) +
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
            binding.market.setOnClickListener(v -> ProcessUtils.openMarket(AboutActivity.this, getPackageName()));
            return binding.getRoot();
        }
    }

    enum Page {
        VERSION(R.string.about_version),
        CHANGELOG(R.string.about_changelog),
        SYSTEM(R.string.about_system),
        CONTRIBUTORS(R.string.about_contributors),
        LICENSE(R.string.about_license);

        @StringRes
        private final int resourceId;

        Page(@StringRes final int resourceId) {
            this.resourceId = resourceId;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.viewpager_activity);

        Routing.connect();
        systemInformationTask.execute();

        int startPage = Page.VERSION.ordinal();
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            startPage = extras.getInt(EXTRA_ABOUT_STARTPAGE, startPage);
        }
        createViewPager(startPage, position -> setTitle(res.getString(R.string.about) + " - " + getTitle(Page.values()[position])));
        reinitializeViewPager();
    }

    @Override
    protected void onDestroy() {
        Routing.disconnect();
        super.onDestroy();
        systemInformationTask.onDestroy();
    }

    public final void setClickListener(final View view, final String url) {
        view.setOnClickListener(v -> ShareUtils.openUrl(this, url));
    }

    @Override
    protected final AbstractViewPagerActivity.PageViewCreator createViewCreator(final Page page) {
        switch (page) {
            case VERSION:
                return new VersionViewCreator();
            case CHANGELOG:
                return new ChangeLogViewCreator();
            case SYSTEM:
                return new SystemViewCreator();
            case CONTRIBUTORS:
                return new ContributorsViewCreator();
            case LICENSE:
                return new LicenseViewCreator();
        }
        throw new IllegalStateException(); // cannot happen, when switch case is enum complete
    }

    @Override
    protected final String getTitle(final Page page) {
        if (page == Page.VERSION) {
            return res.getString(R.string.about_version) + " / " + res.getString(R.string.about_help);
        }
        return res.getString(page.resourceId);
    }

    @Override
    protected final Pair<List<? extends Page>, Integer> getOrderedPages() {
        final List<Page> pages = Arrays.asList(Page.values());
        return new ImmutablePair<>(pages, 0);
    }

    public static void showChangeLog(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, AboutActivity.class);
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.CHANGELOG.ordinal());
        fromActivity.startActivity(intent);
    }

    private static class GatherSystemInformationTask extends AsyncTask<Void, Void, String> {

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
}
