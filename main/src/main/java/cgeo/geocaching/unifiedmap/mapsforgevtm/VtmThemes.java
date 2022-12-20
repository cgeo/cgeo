package cgeo.geocaching.unifiedmap.mapsforgevtm;

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

import java.io.InputStream;

import org.oscim.backend.AssetAdapter;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlThemeResourceProvider;

/**
 * Enumeration of all internal rendering themes.
 */
public enum VtmThemes implements ThemeFile {

    DEFAULT("vtm/default.xml"),
    MAPZEN("vtm/mapzen.xml"),
    NEWTRON("vtm/newtron.xml"),
    OPENMAPTILES("vtm/openmaptiles.xml"),
    OSMAGRAY("vtm/osmagray.xml"),
    OSMARENDER("vtm/osmarender.xml"),
    TRONRENDER("vtm/tronrender.xml");

    private final String mPath;

    VtmThemes(final String path) {
        mPath = path;
    }

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return null;
    }

    @Override
    public String getRelativePathPrefix() {
        return "";
    }

    @Override
    public InputStream getRenderThemeAsStream() throws ThemeException {
        return AssetAdapter.readFileAsStream(mPath);
    }

    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return null;
    }

    @Override
    public boolean isMapsforgeTheme() {
        return false;
    }

    @Override
    public void setMapsforgeTheme(final boolean mapsforgeTheme) {
        // intentionally left empty
    }

    @Override
    public void setMenuCallback(final XmlRenderThemeMenuCallback menuCallback) {
        // intentionally left empty
    }

    @Override
    public void setResourceProvider(final XmlThemeResourceProvider resourceProvider) {
        // intentionally left empty
    }
}
