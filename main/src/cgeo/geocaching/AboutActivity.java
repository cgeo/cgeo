package cgeo.geocaching;

import butterknife.ButterKnife;
import butterknife.InjectView;

import cgeo.calendar.CalendarAddon;
import cgeo.contacts.ContactsAddon;
import cgeo.geocaching.activity.AbstractViewPagerActivity;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.sensors.OrientationProvider;
import cgeo.geocaching.sensors.RotationProvider;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AbstractCachingPageViewCreator;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.Version;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class AboutActivity extends AbstractViewPagerActivity<AboutActivity.Page> {

    private static final String EXTRA_ABOUT_STARTPAGE = "cgeo.geocaching.extra.about.startpage";

    class LicenseViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.license) protected TextView licenseLink;
        @InjectView(R.id.license_text) protected TextView licenseText;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_license_page, parentView, false);
            ButterKnife.inject(this, view);
            setClickListener(licenseLink, "http://www.apache.org/licenses/LICENSE-2.0.html");
            licenseText.setText(getRawResourceString(R.raw.license));
            return view;
        }
    }

    class ContributorsViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.contributors) protected TextView contributors;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_contributors_page, parentView, false);
            ButterKnife.inject(this, view);
            contributors.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            return view;
        }

    }

    class ChangeLogViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.changelog_master) protected TextView changeLogMaster;
        @InjectView(R.id.changelog_release) protected TextView changeLogRelease;
        @InjectView(R.id.changelog_github) protected TextView changeLogLink;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_changes_page, parentView, false);
            ButterKnife.inject(this, view);
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

        @InjectView(R.id.system) protected TextView system;
        @InjectView(R.id.copy) protected Button copy;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_system_page, parentView, false);
            ButterKnife.inject(this, view);
            final String systemInfo = systemInformation(AboutActivity.this);
            system.setText(systemInfo);
            system.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
            Compatibility.setTextIsSelectable(system, true);
            copy.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    ClipboardUtils.copyToClipboard(systemInfo);
                    showShortToast(getString(R.string.clipboard_copy_ok));
                }
            });
            return view;
        }
    }

    class HelpViewCreator extends AbstractCachingPageViewCreator<ScrollView> {

        @InjectView(R.id.support) protected TextView support;
        @InjectView(R.id.website) protected TextView website;
        @InjectView(R.id.facebook) protected TextView facebook;
        @InjectView(R.id.twitter) protected TextView twitter;
        @InjectView(R.id.market) protected TextView market;
        @InjectView(R.id.faq) protected TextView faq;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_help_page, parentView, false);
            ButterKnife.inject(this, view);
            setClickListener(support, "mailto:support@cgeo.org?subject=" + Uri.encode("cgeo " + Version.getVersionName(AboutActivity.this)) +
                    "&body=" + Uri.encode(systemInformation(AboutActivity.this)) + "\n");
            setClickListener(website, "http://www.cgeo.org/");
            setClickListener(facebook, "http://www.facebook.com/pages/cgeo/297269860090");
            setClickListener(twitter, "http://twitter.com/android_gc");
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

        @InjectView(R.id.about_version_string) protected TextView version;
        @InjectView(R.id.donate) protected TextView donateButton;

        @Override
        public ScrollView getDispatchedView(final ViewGroup parentView) {
            final ScrollView view = (ScrollView) getLayoutInflater().inflate(R.layout.about_version_page, parentView, false);
            ButterKnife.inject(this, view);
            version.setText(Version.getVersionName(AboutActivity.this));
            setClickListener(donateButton, "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AQBS7UP76CXW2");
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

        private final int resourceId;

        Page(final int resourceId) {
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
    protected final cgeo.geocaching.activity.AbstractViewPagerActivity.PageViewCreator createViewCreator(final Page page) {
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

    private String getRawResourceString(final int resourceId) {
        String result;
        Scanner scanner = null;
        try {
            final InputStream ins = res.openRawResource(resourceId);
            scanner = new Scanner(ins, CharEncoding.UTF_8);
            result = scanner.useDelimiter("\\A").next();
            IOUtils.closeQuietly(ins);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return result;
    }

    public static void showChangeLog(final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, AboutActivity.class);
        intent.putExtra(EXTRA_ABOUT_STARTPAGE, Page.CHANGELOG.ordinal());
        fromActivity.startActivity(intent);
    }


    private static String presence(final Boolean present) {
        return present ? "present" : "absent";
    }

    private static String systemInformation(final Context context) {
        final boolean googlePlayServicesAvailable = CgeoApplication.getInstance().isGooglePlayServicesAvailable();
        final StringBuilder body = new StringBuilder("--- System information ---")
                .append("\nDevice: ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.BRAND).append(")")
                .append("\nAndroid version: ").append(VERSION.RELEASE)
                .append("\nAndroid build: ").append(Build.DISPLAY)
                .append("\nCgeo version: ").append(Version.getVersionName(context))
                .append("\nGoogle Play services: ").append(googlePlayServicesAvailable ? (Settings.useGooglePlayServices() ? "enabled" : "disabled") : "unavailable")
                .append("\nLow power mode: ").append(Settings.useLowPowerMode() ? "active" : "inactive")
                .append("\nCompass capabilities: ").append(Sensors.getInstance().hasCompassCapabilities() ? "yes" : "no")
                .append("\nRotation sensor: ").append(presence(RotationProvider.hasRotationSensor(context)))
                .append("\nGeomagnetic rotation sensor: ").append(presence(RotationProvider.hasGeomagneticRotationSensor(context)))
                .append("\nOrientation sensor: ").append(presence(OrientationProvider.hasOrientationSensor(context)))
                .append("\nHide own/found: ").append(Settings.isExcludeMyCaches())
                .append("\nMap strategy: ").append(Settings.getLiveMapStrategy().toString().toLowerCase(Locale.getDefault()));
        for (final ILogin connector : ConnectorFactory.getActiveLiveConnectors()) {
            body.append('\n').append(connector.getName()).append(": ").append(connector.isLoggedIn() ? "logged in" : "not logged in")
                    .append(" (").append(connector.getLoginStatusString()).append(')');
            if (connector.getName().equals("geocaching.com") && connector.isLoggedIn()) {
                body.append(" / ").append(Settings.getGCMemberStatus());
            }
        }
        body.append("\nSystem language: ").append(Locale.getDefault());
        if (Settings.isUseEnglish()) {
            body.append(" (cgeo forced to English)");
        }
        final boolean calendarAddonAvailable = CalendarAddon.isAvailable();
        final boolean contactsAddonAvailable = ContactsAddon.isAvailable();
        body.append("\nInstalled cgeo plugins:");
        if (calendarAddonAvailable || contactsAddonAvailable) {
            if (calendarAddonAvailable) {
                body.append(" calendar");
            }
            if (contactsAddonAvailable) {
                body.append(" contacts");
            }
        } else {
            body.append(" none");
        }
        body.append("\n--- End of system information ---\n");
        return body.toString();
    }

}
