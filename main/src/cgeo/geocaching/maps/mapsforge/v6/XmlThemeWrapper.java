package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider;

public class XmlThemeWrapper implements XmlRenderTheme {

    private final XmlRenderTheme wrapped;
    private boolean scaleOn = false;
    private float scaleSymbol = 1;
    private float scaleStrokeWidth = 1;
    private float scaleFontSize = 1;

    public XmlThemeWrapper(final XmlRenderTheme wrapped, final String renderthemeScale) {
        this.wrapped = wrapped;

        if (!StringUtils.isBlank(renderthemeScale)) {
            final String[] tokens = renderthemeScale.split("-");
            if (tokens.length >= 1) {
                scaleOn = true;
                scaleFontSize = NumberUtils.toFloat(tokens[0], 1);
            }
            if (tokens.length >= 2) {
                scaleStrokeWidth = NumberUtils.toFloat(tokens[1], 1);
            }
            if (tokens.length >= 3) {
                scaleSymbol = NumberUtils.toFloat(tokens[2], 1);
            }
        }

    }

    @Override
    public InputStream getRenderThemeAsStream() throws IOException {

        if (!scaleOn) {
            return this.wrapped.getRenderThemeAsStream();
        }

        String xml = CollectionStream.of(IOUtils.readLines(this.wrapped.getRenderThemeAsStream(), "UTF-8")).toJoinedString();

        xml = rescaleAttributes(xml, "symbol-height", true, scaleSymbol);
        xml = rescaleAttributes(xml, "symbol-width", true, scaleSymbol);
        xml = rescaleAttributes(xml, "stroke-width", false, scaleStrokeWidth);
        xml = rescaleAttributes(xml, "font-size", true, scaleFontSize);

        return new ByteArrayInputStream(xml.getBytes());
    }

    private static String rescaleAttributes(final String xml, final String attributeName, final boolean roundToInt, final float factor) {
        return TextUtils.replacePattern(xml, attributeName + "=\"([0-9.]+)\"", f -> {
            final float value = NumberUtils.toFloat(f);
            final String newValue = value == 0.0f ? f : (roundToInt ? "" + Math.round(factor * value) : "" + Math.round(factor * value * 100) / 100f);
            return attributeName + "=\"" + newValue + "\"";
        });
    }

    //delegated calls

    @Override
    public XmlRenderThemeMenuCallback getMenuCallback() {
        return this.wrapped.getMenuCallback();
    }

    @Override
    public String getRelativePathPrefix() {
        return this.wrapped.getRelativePathPrefix();
    }


    @Override
    public XmlThemeResourceProvider getResourceProvider() {
        return this.wrapped.getResourceProvider();
    }

    @Override
    public void setMenuCallback(final XmlRenderThemeMenuCallback menuCallback) {
        this.wrapped.setMenuCallback(menuCallback);
    }

    @Override
    public void setResourceProvider(final XmlThemeResourceProvider resourceProvider) {
        this.wrapped.setResourceProvider(resourceProvider);
    }
}
