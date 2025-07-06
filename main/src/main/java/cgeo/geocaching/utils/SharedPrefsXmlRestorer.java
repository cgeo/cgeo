package cgeo.geocaching.utils;

import android.content.Context;

import android.content.SharedPreferences;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

public class SharedPrefsXmlRestorer {

    public static void restoreFromXmlAsset(Context context, String assetFileName) {
        Log.iForce("Starting SharedPreferences XML restoration from asset: " + assetFileName);

        try {
            InputStream is = context.getAssets().open(assetFileName);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            SharedPreferences prefs = context.getSharedPreferences("cgeo.geocaching.developer_preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            int count = 0;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    String name = parser.getAttributeValue(null, "name");

                    if (name == null) {
                        eventType = parser.next();
                        continue;
                    }

                    switch (tag) {
                        case "boolean":
                            boolean boolValue = Boolean.parseBoolean(parser.getAttributeValue(null, "value"));
                            editor.putBoolean(name, boolValue);
                            count++;
                            break;
                        case "string":
                            String strValue = "";
                            if (parser.next() == XmlPullParser.TEXT) {
                                strValue = parser.getText();
                                parser.nextTag(); // skip to end tag
                            }
                            editor.putString(name, strValue);
                            count++;
                            break;
                        case "int":
                            int intValue = Integer.parseInt(parser.getAttributeValue(null, "value"));
                            editor.putInt(name, intValue);
                            count++;
                            break;
                        case "long":
                            long longValue = Long.parseLong(parser.getAttributeValue(null, "value"));
                            editor.putLong(name, longValue);
                            count++;
                            break;
                        case "float":
                            float floatValue = Float.parseFloat(parser.getAttributeValue(null, "value"));
                            editor.putFloat(name, floatValue);
                            count++;
                            break;
                        default:
                            Log.w("Unknown tag <" + tag + "> with name " + name + ", skipping.");
                    }
                }

                eventType = parser.next();
            }

            editor.apply();
            Log.iForce("Restored " + count + " preferences from " + assetFileName + " into cgeo.developer_preferences");

        } catch (Exception e) {
            Log.e("Failed to restore SharedPreferences from XML asset", e);
        }
    }
}
