package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class Twitter {
    public static final int MAX_TWEET_SIZE = 140;

    public static void postTweet(cgeoapplication app, cgSettings settings, String status, final Geopoint coords) {
        if (app == null) {
            return;
        }
        if (settings == null || StringUtils.isBlank(settings.tokenPublic) || StringUtils.isBlank(settings.tokenSecret)) {
            return;
        }

        try {
            Parameters parameters = new Parameters();

            parameters.put("status", status);
            if (coords != null) {
                parameters.put("lat", String.format("%.6f", coords.getLatitude()));
                parameters.put("long", String.format("%.6f", coords.getLongitude()));
                parameters.put("display_coordinates", "true");
            }

            final String paramsDone = cgOAuth.signOAuth("api.twitter.com", "/1/statuses/update.json", "POST", false, parameters, settings.tokenPublic, settings.tokenSecret);

            HttpURLConnection connection = null;
            try {
                final StringBuffer buffer = new StringBuffer();
                final URL u = new URL("http://api.twitter.com/1/statuses/update.json");
                final URLConnection uc = u.openConnection();

                uc.setRequestProperty("Host", "api.twitter.com");

                connection = (HttpURLConnection) uc;
                connection.setReadTimeout(30000);
                connection.setRequestMethod("POST");
                HttpURLConnection.setFollowRedirects(true);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                final OutputStream out = connection.getOutputStream();
                final OutputStreamWriter wr = new OutputStreamWriter(out);
                wr.write(paramsDone);
                wr.flush();
                wr.close();

                Log.i(cgSettings.tag, "Twitter.com: " + connection.getResponseCode() + " " + connection.getResponseMessage());

                InputStream ins;
                final String encoding = connection.getContentEncoding();

                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    ins = new GZIPInputStream(connection.getInputStream());
                } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                    ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
                } else {
                    ins = connection.getInputStream();
                }

                final InputStreamReader inr = new InputStreamReader(ins);
                final BufferedReader br = new BufferedReader(inr);

                readIntoBuffer(br, buffer);

                br.close();
                ins.close();
                inr.close();
                connection.disconnect();
            } catch (IOException e) {
                Log.e(cgSettings.tag, "cgBase.postTweet.IO: " + connection.getResponseCode() + ": " + connection.getResponseMessage() + " ~ " + e.toString());

                final InputStream ins = connection.getErrorStream();
                final StringBuffer buffer = new StringBuffer();
                final InputStreamReader inr = new InputStreamReader(ins);
                final BufferedReader br = new BufferedReader(inr);

                readIntoBuffer(br, buffer);

                br.close();
                ins.close();
                inr.close();
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgBase.postTweet.inner: " + e.toString());
            }

            connection.disconnect();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.postTweet: " + e.toString());
        }
    }

    private static void readIntoBuffer(BufferedReader br, StringBuffer buffer) throws IOException {
        final int bufferSize = 1024 * 16;
        final char[] bytes = new char[bufferSize];
        int bytesRead;
        while ((bytesRead = br.read(bytes)) > 0) {
            buffer.append(bytes, 0, bytesRead);
        }
    }

    public static String appendHashTag(final String status, final String tag) {
        String result = status;
        if (result.length() + 2 + tag.length() <= 140) {
            result += " #" + tag;
        }
        return result;
    }
}
