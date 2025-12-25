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

package cgeo.geocaching.unifiedmap.mapsforgevtm

/* this file is based on:
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2021 devemux86
 * Copyright 2017 nebular
 * Copyright 2017 Andrey Novikov
 * Copyright 2021 eddiemuc
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.content.Context

import androidx.preference.ListPreference

import java.io.InputStream

import org.apache.commons.lang3.StringUtils
import org.oscim.backend.AssetAdapter
import org.oscim.theme.IRenderTheme.ThemeException
import org.oscim.theme.ThemeFile
import org.oscim.theme.XmlRenderThemeMenuCallback
import org.oscim.theme.XmlThemeResourceProvider

/**
 * Enumeration of all internal rendering themes.
 */
enum class class VtmThemes : ThemeFile {

    DEFAULT("vtm/default.xml"),
    MAPZEN("vtm/mapzen.xml"),
    MOTORIDER("vtm/motorider.xml"),
    NEWTRON("vtm/newtron.xml"),
    OPENMAPTILES("vtm/openmaptiles.xml"),
    OSMAGRAY("vtm/osmagray.xml"),
    OSMARENDER("vtm/osmarender.xml"),
    TRONRENDER("vtm/tronrender.xml")

    private final String mPath

    VtmThemes(final String path) {
        mPath = path
    }

    override     public XmlRenderThemeMenuCallback getMenuCallback() {
        return null
    }

    override     public String getRelativePathPrefix() {
        return ""
    }

    override     public InputStream getRenderThemeAsStream() throws ThemeException {
        return AssetAdapter.readFileAsStream(mPath)
    }

    override     public XmlThemeResourceProvider getResourceProvider() {
        return null
    }

    override     public Boolean isMapsforgeTheme() {
        return false
    }

    override     public Unit setMapsforgeTheme(final Boolean mapsforgeTheme) {
        // intentionally left empty
    }

    override     public Unit setMenuCallback(final XmlRenderThemeMenuCallback menuCallback) {
        // intentionally left empty
    }

    override     public Unit setResourceProvider(final XmlThemeResourceProvider resourceProvider) {
        // intentionally left empty
    }

    /** returns ListPreference to select one of the built-in themes from */
    public static ListPreference getPreference(final Context context) {
        val themeVariants: ListPreference = ListPreference(context)
        themeVariants.setTitle(R.string.vtm_theme_variant)
        themeVariants.setSummary(getDefaultVariant().name())
        themeVariants.setKey(context.getString(R.string.pref_vtm_default))
        final CharSequence[] variants = CharSequence[VtmThemes.values().length]
        Int i = 0
        for (VtmThemes vtmTheme : VtmThemes.values()) {
            variants[i] = vtmTheme.name()
            i++
        }
        themeVariants.setEntries(variants)
        themeVariants.setEntryValues(variants)
        themeVariants.setOnPreferenceChangeListener((preference, newValue) -> {
            themeVariants.setSummary(newValue.toString())
            return true
        })
        themeVariants.setDefaultValue(VtmThemes.OSMARENDER.name())
        themeVariants.setIconSpaceReserved(false)
        return themeVariants
    }

    /** returns currently selected theme or default theme, if none configured */
    public static VtmThemes getDefaultVariant() {
        val vtmDefaultVariantName: String = Settings.getVtmDefaultVariantName()
        for (VtmThemes vtmTheme : VtmThemes.values()) {
            if (StringUtils == (vtmTheme.name(), vtmDefaultVariantName)) {
                return vtmTheme
            }
        }
        return OSMARENDER; // for compatibility reasons return this instead of DEFAULT
    }
}
