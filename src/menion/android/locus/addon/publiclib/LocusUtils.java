package menion.android.locus.addon.publiclib;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Locus Helper class
 * 
 * @author Menion Asamm
 * @author Arcao
 * 
 */
public class LocusUtils {

	/** Locus Free package name */
	public static final String LOCUS_FREE_PACKAGE_NAME = "menion.android.locus";
	/** Locus Pro package name */
	public static final String LOCUS_PRO_PACKAGE_NAME = "menion.android.locus.pro";
	/** All Locus package names */
	public static final String[] LOCUS_PACKAGE_NAMES = new String[] {
			LOCUS_PRO_PACKAGE_NAME, LOCUS_FREE_PACKAGE_NAME };

	/**
	 * Returns <code>true</code> if Locus Pro or Locus Free is installed.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return true or false
	 */
	public static boolean isLocusAvailable(Context context) {
		return getLocusPackageInfo(context) != null;
	}

	/**
	 * Returns {@link PackageInfo} with information about Locus or null if Locus
	 * is not installed.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return instance of PackageInfo object
	 */
	public static PackageInfo getLocusPackageInfo(Context context) {
		PackageInfo info = null;
		for (String p : LOCUS_PACKAGE_NAMES) {
			try {
				info = context.getPackageManager().getPackageInfo(p, 0);
				break;
			} catch (PackageManager.NameNotFoundException e) {
			}
		}

		return info;
	}

	/**
	 * Returns Locus version, e.g. <code>"1.9.5.1"</code>. If Locus is not
	 * installed returns <code>null</code>.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return version
	 */
	public static String getLocusVersion(Context context) {
		PackageInfo info = getLocusPackageInfo(context);

		if (info == null)
			return null;

		return info.versionName;
	}

	/**
	 * Returns Locus version code, e.g. <code>99</code>. If Locus is not
	 * installed returns <code>-1</code>.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return version code
	 */
	public static int getLocusVersionCode(Context context) {
		PackageInfo info = getLocusPackageInfo(context);

		if (info == null)
			return -1;

		return info.versionCode;
	}

	/**
	 * Returns <code>true</code> if Locus Pro is installed.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return true or false
	 */
	public static boolean isLocusProInstalled(Context context) {
		try {
			context.getPackageManager().getPackageInfo(LOCUS_PRO_PACKAGE_NAME,
					0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	/**
	 * Returns a package name of Locus (Pro). If Locus is not installed returns
	 * null.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return package name
	 */
	public static String getLocusPackageName(Context context) {
		PackageInfo info = getLocusPackageInfo(context);
		if (info == null)
			return null;
		return info.packageName;
	}

	/**
	 * Returns a package name like {@link #getLocusPackageName(Context)} but if
	 * Locus is not installed returns a default package name constant
	 * {@link #LOCUS_FREE_PACKAGE_NAME}.
	 * 
	 * @param context
	 *            actual {@link Context}
	 * @return package name
	 */
	public static String getLocusDefaultPackageName(Context context) {
		String name = getLocusPackageName(context);
		if (name == null)
			return LOCUS_FREE_PACKAGE_NAME;
		return name;
	}
}
