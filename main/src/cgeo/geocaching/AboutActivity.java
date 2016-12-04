package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.SystemInformation;
import cgeo.geocaching.utils.Version;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

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

        @BindView(R.id.contributors) protected TextView contributors;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_contributors_page, parentView, false);
            ButterKnife.bind(this, view);
            contributors.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            return view;
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
            changeLogLink.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(final View v) {
                    startUrl("https://github.com/cgeo/cgeo/releases");
                }
            });
            return view;
        }

    }

    class SystemViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.system) protected TextView system;
        @BindView(R.id.copy) protected Button copy;
        @BindView(R.id.share) protected Button share;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_system_page, parentView, false);
            ButterKnife.bind(this, view);
            final String systemInfo = SystemInformation.getSystemInformation(AboutActivity.this);
            system.setText(systemInfo);
            system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            system.setTextIsSelectable(true);
            copy.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    ClipboardUtils.copyToClipboard(systemInfo);
                    showShortToast(getString(R.string.clipboard_copy_ok));
                }
            });
            share.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    ClipboardUtils.copyToClipboard(systemInfo);
                    final Intent share = new Intent(Intent.ACTION_SENDTO);
                    share.setType("message/rfc822");
                    share.setData(Uri.parse("mailto:"));
                    share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_system_info));
                    share.putExtra(Intent.EXTRA_TEXT, systemInfo);
                    startActivity(Intent.createChooser(share, getString(R.string.about_system_info_send_chooser)));
                }
            });
            return view;
        }
    }

    class HelpViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.support) protected TextView support;
        @BindView(R.id.website) protected TextView website;
        @BindView(R.id.facebook) protected TextView facebook;
        @BindView(R.id.twitter) protected TextView twitter;
        @BindView(R.id.market) protected TextView market;
        @BindView(R.id.faq) protected TextView faq;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_help_page, parentView, false);
            ButterKnife.bind(this, view);
            setClickListener(support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(AboutActivity.this)) +
                    "&body=" + Uri.encode(SystemInformation.getSystemInformation(AboutActivity.this)) + "\n");
            setClickListener(website, "http://www.cgeo.org/");
            setClickListener(facebook, "https://www.facebook.com/pages/cgeo/297269860090");
            setClickListener(twitter, "https://twitter.com/android_gc");
            setClickListener(faq, "http://faq.cgeo.org/");
            market.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View v) {
                    ProcessUtils.openMarket(AboutActivity.this, getPackageName());
                }
            });
            return view;
        }

    }

    class VersionViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @BindView(R.id.about_version_string) protected TextView version;
        @BindView(R.id.about_special_build) protected TextView specialBuild;
        @BindView(R.id.donate) protected TextView donateButton;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_version_page, parentView, false);
            ButterKnife.bind(this, view);
            version.setText(Version.getVersionName(AboutActivity.this));
            setClickListener(donateButton, "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AQBS7UP76CXW2");
            if (StringUtils.isNotEmpty(BuildConfig.SPECIAL_BUILD)) {
                specialBuild.setText(BuildConfig.SPECIAL_BUILD);
                specialBuild.setVisibility(View.VISIBLE);
            }
            return view;
        }
    }

    enum Page {
        VERSION(R.string.about_version),
        HELP(R.string.about_help),
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
        createViewPager(startPage, null);
        reinitializeViewPager();
    }

    public final void setClickListener(final View view, final String url) {
        view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                startUrl(url);
            }
        });
    }

    private void startUrl(final String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    protected final AbstractViewPagerActivity.PageViewCreator createViewCreator(final Page page) {
        switch (page) {
            case VERSION:
                return new VersionViewCreator();
            case HELP:
                return new HelpViewCreator();
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

}
