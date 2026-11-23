package com.example.brady_jack_s2327712.data;

import android.content.Context;
import android.util.Log;

import com.example.brady_jack_s2327712.R;
import com.example.brady_jack_s2327712.model.Situation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.brady_jack_s2327712.BuildConfig;

/**
 * Traffic Repository reads the data from either the example files or the
 * live data stream and passes it to TrafficParser to be parsed into the model
 * <p>
 * Each successful download is saved to app storage so the previous data
 * can be used if the next download fails
 * <p>
 * load() is blocking therefore must be called from a worker thread
 */
public class TrafficRepository {

    private static final String TAG = "TrafficRepository";

    // false = download the live feeds, true = use the example XML files in res/raw
    public static final boolean USE_EXAMPLE_DATA = false;

    private static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // File names of saved copies of last good data in app's private storage if connection to live data is lost
    private static final String UNPLANNED_CACHE = "unplanned_cache.xml";
    private static final String ROADWORKS_CACHE = "roadworks_cache.xml";
    // Reads from local.properties on build so never written in java file
    private static final String CLIENT_ID = BuildConfig.TS_CLIENT_ID;
    private static final String CLIENT_KEY = BuildConfig.TS_CLIENT_KEY;
    private final Context context;
    private final TrafficParser parser = new TrafficParser();

    public TrafficRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    // A load produces two things (situations and where they came from),
    // but a method can only return one, so they are bundled together here
    public static class LoadResult {
        private final List<Situation> situations;
        private final String sourceDescription;


        LoadResult(List<Situation> situations, String sourceDescription) {
            this.situations = situations;
            this.sourceDescription = sourceDescription;
        }

        public List<Situation> getSituations() {
            return situations;
        }

        public String getSourceDescription() {
            return sourceDescription;
        }
    }


    // Loads incidents and roadworks
    public LoadResult load() throws Exception {
        if (USE_EXAMPLE_DATA) {
            List<Situation> all = parseBoth(
                    readRaw(R.raw.unplanned_example_2),
                    readRaw(R.raw.roadworks_example_2));
            return new LoadResult(all, "Example data");
        }

        // Parse before saving so a bad download doesn't replace last good copy
        try {
            String unplannedXml = download(UNPLANNED_URL);
            String roadworksXml = download(ROADWORKS_URL);
            List<Situation> all = parseBoth(unplannedXml, roadworksXml);
            writeFile(UNPLANNED_CACHE, unplannedXml);
            writeFile(ROADWORKS_CACHE, roadworksXml);
            return new LoadResult(all, "Live data, updated " + now());

        } catch (Exception liveError) {
            Log.w(TAG, "Live download failed, trying saved copy: " + liveError);
            File savedFile = new File(context.getFilesDir(), UNPLANNED_CACHE);
            if (!savedFile.exists()) {
                throw new IOException("No internet connection and no saved data yet");
            }
            List<Situation> all = parseBoth(readFile(UNPLANNED_CACHE), readFile(ROADWORKS_CACHE));
            String savedAt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.UK)
                    .format(new Date(savedFile.lastModified()));
            return new LoadResult(all, "Offline - showing data saved " + savedAt);
        }
    }

    // Parsing one example file on its own used for testing
    public LoadResult loadExampleFile(int resId, boolean roadworksFeed, String fileName) throws Exception {
        List<Situation> list = parser.parse(readRaw(resId), roadworksFeed);
        return new LoadResult(list, fileName);
    }

    private List<Situation> parseBoth(String unplannedXml, String roadworksXml) throws Exception {
        List<Situation> all = new ArrayList<>();
        all.addAll(parser.parse(unplannedXml, false));
        all.addAll(parser.parse(roadworksXml, true));
        return all;
    }

    private String download(String address) throws IOException {
        if (CLIENT_ID.isEmpty() || CLIENT_KEY.isEmpty()) {
            throw new IOException("No Traffic Scotland credentials in local.properties");
        }
        HttpURLConnection connection =
                (HttpURLConnection) URI.create(address).toURL().openConnection();
        connection.setConnectTimeout(15000);   // give up after 15 s instead of hanging
        connection.setReadTimeout(30000);
        String credentials = CLIENT_ID + ":" + CLIENT_KEY;
        connection.setRequestProperty("Authorization",
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + code);
            }
            return readStream(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private String readRaw(int resId) throws IOException {
        return readStream(context.getResources().openRawResource(resId));
    }

    // Saved copies stored in getFilesDir()
    // app-private storage so no permission required
    private String readFile(String name) throws IOException {
        return readStream(new FileInputStream(new File(context.getFilesDir(), name)));
    }

    private void writeFile(String name, String text) throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), name))) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    // Reads any stream into a String
    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String now() {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.UK).format(new Date());
    }
}