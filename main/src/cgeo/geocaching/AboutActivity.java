package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.SystemInformation;
import cgeo.geocaching.utils.Version;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AboutActivity extends AbstractViewPagerActivity<AboutActivity.Page> {

    private static final String EXTRA_ABOUT_STARTPAGE = "cgeo.geocaching.extra.about.startpage";

    class LicenseViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.license) protected TextView licenseLink;
        @BindView(R.id.license_text) protected TextView licenseText;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_license_page, parentView, false);
            ButterKnife.bind(this, view);
            setClickListener(licenseLink, "https://www.apache.org/licenses/LICENSE-2.0.html");
            licenseText.setText(getRawResourceString(R.raw.license));
            return view;
        }

        private String getRawResourceString(@RawRes final int resourceId) {
            InputStream ins = null;
            Scanner scanner = null;
            try {
                ins = res.openRawResource(resourceId);
                scanner = new Scanner(ins, CharEncoding.UTF_8);
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

        @BindView(R.id.about_carnerodetails) protected TextView carnerodetails;
        @BindView(R.id.about_contributors_recent) protected TextView contributors;
        @BindView(R.id.about_specialthanksdetails) protected TextView specialthanks;
        @BindView(R.id.about_contributors_others) protected TextView contributorsOthers;
        @BindView(R.id.about_components) protected TextView components;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_contributors_page, parentView, false);
            ButterKnife.bind(this, view);
            setText(contributors, R.string.contributors_recent);
            setText(contributorsOthers, R.string.contributors_other);

            final AnchorAwareLinkMovementMethod mm = AnchorAwareLinkMovementMethod.getInstance();
            carnerodetails.setMovementMethod(mm);
            contributors.setMovementMethod(mm);
            specialthanks.setMovementMethod(mm);
            contributorsOthers.setMovementMethod(mm);
            components.setMovementMethod(mm);

            return view;
        }

        private String checkRoles(final String s, final String roles, final char checkFor, final int infoId) {
            return roles.indexOf(checkFor) >= 0 ? (s.isEmpty() ? "" : s + ", ") + getString(infoId) : s;
        }

        private void setText(final TextView t, final int resId) {
            String s = getString(resId);
            final SpannableStringBuilder sb = new SpannableStringBuilder("<ul>");
            int p1 = 0;
            int p2 = 0;
            int p3 = 0;
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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                t.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                t.setText(Html.fromHtml(sb.toString()));
            }
        }
    }

    class ChangeLogViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.changelog_master) protected TextView changeLogMaster;
        @BindView(R.id.changelog_release) protected TextView changeLogRelease;
        @BindView(R.id.changelog_github) protected TextView changeLogLink;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_changes_page, parentView, false);
            ButterKnife.bind(this, view);
            changeLogRelease.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            final String changeLogMasterString = getString(R.string.changelog_master);
            if (StringUtils.isBlank(changeLogMasterString)) {
                changeLogMaster.setVisibility(View.GONE);
            } else {
                changeLogMaster.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            }
            changeLogLink.setOnClickListener(v -> startUrl("https://github.com/cgeo/cgeo/releases"));
            return view;
        }

    }

    class SystemViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.system) protected TextView system;
        @BindView(R.id.copy) protected Button copy;
        @BindView(R.id.share) protected Button share;
        @BindView(R.id.logcat) protected Button logcat;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_system_page, parentView, false);
            ButterKnife.bind(this, view);
            final String systemInfo = SystemInformation.getSystemInformation(AboutActivity.this);
            system.setText(systemInfo);
            system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            system.setTextIsSelectable(true);
            copy.setOnClickListener(view1 -> {
                ClipboardUtils.copyToClipboard(systemInfo);
                showShortToast(getString(R.string.clipboard_copy_ok));
            });
            share.setOnClickListener(view12 -> ShareUtils.shareAsEMail(AboutActivity.this, getString(R.string.about_system_info), systemInfo, null, R.string.about_system_info_send_chooser));
            logcat.setOnClickListener(view13 -> DebugUtils.createLogcat(AboutActivity.this));
            return view;
        }
    }

    class StartingViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.about_starting_btn_services) protected Button services;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_starting_page, parentView, false);
            ButterKnife.bind(this, view);

            services.setOnClickListener(v -> SettingsActivity.openForScreen(R.string.preference_screen_services, AboutActivity.this));
            return view;
        }

    }

    class VersionViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.about_version_string) protected TextView version;
        @BindView(R.id.about_special_build) protected TextView specialBuild;
        @BindView(R.id.donate) protected TextView donateButton;

        @BindView(R.id.support) protected TextView support;
        @BindView(R.id.website) protected TextView website;
        @BindView(R.id.facebook) protected TextView facebook;
        @BindView(R.id.twitter) protected TextView twitter;
        @BindView(R.id.nutshellmanual) protected TextView nutshellmanual;
        @BindView(R.id.market) protected TextView market;
        @BindView(R.id.faq) protected TextView faq;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_version_page, parentView, false);
            ButterKnife.bind(this, view);
            version.setText(Version.getVersionName(AboutActivity.this));
            setClickListener(donateButton, "https://www.cgeo.org");
            if (StringUtils.isNotEmpty(BuildConfig.SPECIAL_BUILD)) {
                specialBuild.setText(BuildConfig.SPECIAL_BUILD);
                specialBuild.setVisibility(View.VISIBLE);
            }

            setClickListener(support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(AboutActivity.this)) +
                    "&body=" + Uri.encode(SystemInformation.getSystemInformation(AboutActivity.this)) + "\n");
            setClickListener(website, "https://www.cgeo.org/");
            setClickListener(facebook, "https://www.facebook.com/pages/cgeo/297269860090");
            setClickListener(twitter, "https://twitter.com/android_gc");
            setClickListener(nutshellmanual, "https://manual.cgeo.org/");
            setClickListener(faq, "https://faq.cgeo.org/");
            market.setOnClickListener(v -> ProcessUtils.openMarket(AboutActivity.this, getPackageName()));
            return view;
        }
    }

    enum Page {
        VERSION(R.string.about_version),
        STARTING(R.string.about_starting),
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
        super.onCreate(savedInstanceState, R.layout.viewpager_activity);

        int startPage = Page.VERSION.ordinal();
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            startPage = extras.getInt(EXTRA_ABOUT_STARTPAGE, startPage);
        }
        createViewPager(startPage, position -> setTitle(res.getString(R.string.about) + " - " + getTitle(Page.values()[position])));
        reinitializeViewPager();
    }

    public final void setClickListener(final View view, final String url) {
        view.setOnClickListener(v -> startUrl(url));
    }

    private void startUrl(final String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    protected final AbstractViewPagerActivity.PageViewCreator createViewCreator(final Page page) {
        switch (page) {
            case VERSION:
                return new VersionViewCreator();
            case STARTING:
                return new StartingViewCreator();
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
        return new ImmutablePair<List<? extends Page>, Integer>(pages, 0);
    }

    public static void showChangeLog(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, AboutActivity.class);
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.CHANGELOG.ordinal());
        fromActivity.startActivity(intent);
    }

    public static void showStarting(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, AboutActivity.class);
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.STARTING.ordinal());
        fromActivity.startActivity(intent);
    }

}
