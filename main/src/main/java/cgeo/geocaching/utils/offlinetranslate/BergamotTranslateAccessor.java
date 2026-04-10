package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.network.Network;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import dev.davidv.bergamot.DetectionResult;
import dev.davidv.bergamot.LangDetect;
import dev.davidv.bergamot.NativeLib;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Bergamot-based offline translation backend for cgeo.
 *
 * Uses Mozilla's firefox-translations-models (MPL-2.0) via the bergamot-translator
 * C++ library wrapped via JNI (libbergamot-sys.so).
 *
 * Model files (~30 MB per direction, gzip-compressed at rest) are downloaded on demand
 * from Mozilla's production model registry and cached in the app's private storage.
 * Each non-English language requires two pair downloads: lang→en and en→lang
 * (needed for pivot translation through English).
 */
public class BergamotTranslateAccessor implements ITranslateAccessor {

    private static final String TAG = "BergamotTranslateAccessor";

    // Mozilla's production model bucket (MPL-2.0 licensed models)
    private static final String MODEL_BUCKET_URL =
        "https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/";
    private static final String MODEL_REGISTRY_URL = MODEL_BUCKET_URL + "db/models.json";

    private static final String PIVOT_LANGUAGE = "en";

    // All language codes with Release-status models in both directions in Mozilla's registry.
    // Verified against https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/db/models.json
    // "zh-hant" maps to registry key "zh_hant" (Traditional Chinese); see toRegistryKey().
    private static final Set<String> SUPPORTED_LANGUAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "en",
        // European
        "bg", "ca", "cs", "da", "de", "el", "es", "et", "fi", "fr",
        "hu", "is", "it", "lt", "lv", "nb", "nl", "no", "pl", "pt",
        "ro", "ru", "sk", "sl", "sv", "uk",
        // Middle East / Central Asia
        "ar", "fa", "he",
        // South / Southeast / East Asia
        "bn", "gu", "hi", "id", "ja", "kn", "ko", "ml", "ms", "te", "th", "tr", "vi", "zh", "zh-hant"
    )));

    /** Resolved download URLs for one translation direction (gzip-compressed on server) */
    private static final class PairUrls {
        final String modelUrl;
        final String vocabSrcUrl;
        final String vocabTrgUrl;  // same as vocabSrcUrl when the model uses a shared vocabulary
        final String lexUrl;

        PairUrls(final String modelUrl, final String vocabSrcUrl, final String vocabTrgUrl, final String lexUrl) {
            this.modelUrl    = modelUrl;
            this.vocabSrcUrl = vocabSrcUrl;
            this.vocabTrgUrl = vocabTrgUrl;
            this.lexUrl      = lexUrl;
        }
    }

    private final NativeLib nativeLib;
    private final LangDetect langDetect;
    private Scheduler callbackScheduler;

    private final Set<String> availableLanguages = new HashSet<>();
    private final Object availableLock = new Object();

    // models.json cached for the session (fetched once, then reused)
    private String cachedRegistryJson = null;
    private final Object registryLock = new Object();

    // Resolved PairUrls cached by "from-to" key
    private final Map<String, PairUrls> pairUrlsCache = new HashMap<>();
    private final Object pairUrlsCacheLock = new Object();

    public BergamotTranslateAccessor() {
        this.nativeLib = new NativeLib();
        this.langDetect = new LangDetect();
        nativeLib.initializeService();
        // Run on IO thread — loading 30MB models per pair is too heavy for main thread
        Schedulers.io().createWorker().schedule(this::scanAvailableModels);
    }

    @Override
    public String getTranslatorName() {
        return "Bergamot";
    }

    @Override
    public void setCallbackScheduler(final Scheduler scheduler) {
        this.callbackScheduler = scheduler;
    }

    @Override
    public String fromLanguageTag(final String tag) {
        if (tag == null) {
            return null;
        }
        // Map Traditional Chinese variants (zh-Hant, zh-TW, zh-HK, zh-MO) to our "zh-hant" code
        final String lower = tag.toLowerCase();
        if (lower.startsWith("zh-")) {
            final String sub = lower.substring(3);
            if ("hant".equals(sub) || "tw".equals(sub) || "hk".equals(sub) || "mo".equals(sub)) {
                return "zh-hant";
            }
        }
        return tag.contains("-") ? tag.split("-")[0].toLowerCase() : lower;
    }

    /**
     * Converts our internal language code to the Mozilla registry key format.
     * Most codes are identical; "zh-hant" is stored as "zh_hant" in the registry.
     */
    private static String toRegistryKey(final String lang) {
        return lang.replace("-", "_");
    }

    @Override
    public Set<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public void getAvailableLanguages(final Consumer<Set<String>> onSuccess, final Consumer<Exception> onError) {
        final Set<String> copy;
        synchronized (availableLock) {
            copy = new HashSet<>(availableLanguages);
        }
        runCallback(() -> onSuccess.accept(copy));
    }

    @Override
    public void downloadLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            runCallback(() -> onError.accept(new IllegalArgumentException("Unsupported language: " + language)));
            return;
        }
        // English (pivot) needs no standalone models — included in every language pair.
        if (PIVOT_LANGUAGE.equals(language)) {
            synchronized (availableLock) {
                availableLanguages.add(PIVOT_LANGUAGE);
            }
            runCallback(onSuccess);
            return;
        }
        Schedulers.io().createWorker().schedule(() -> {
            try {
                downloadPairFiles(language, PIVOT_LANGUAGE);   // lang → en
                downloadPairFiles(PIVOT_LANGUAGE, language);   // en  → lang
                loadPairIntoNative(language);
                synchronized (availableLock) {
                    availableLanguages.add(language);
                    availableLanguages.add(PIVOT_LANGUAGE);
                }
                runCallback(onSuccess);
            } catch (final Exception e) {
                Log.e(TAG + ": Error downloading models for " + language, e);
                runCallback(() -> onError.accept(e));
            }
        });
    }

    @Override
    public void deleteLanguage(final String language, final Runnable onSuccess, final Consumer<Exception> onError) {
        Schedulers.io().createWorker().schedule(() -> {
            if (!PIVOT_LANGUAGE.equals(language)) {
                deleteDir(getPairDir(language, PIVOT_LANGUAGE));
                deleteDir(getPairDir(PIVOT_LANGUAGE, language));
            }
            synchronized (availableLock) {
                availableLanguages.remove(language);
                final boolean hasAny = availableLanguages.stream().anyMatch(l -> !PIVOT_LANGUAGE.equals(l));
                if (!hasAny) {
                    availableLanguages.remove(PIVOT_LANGUAGE);
                }
            }
            runCallback(onSuccess);
        });
    }

    @Override
    public void guessLanguage(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
        if (source == null || source.trim().isEmpty()) {
            runCallback(() -> onSuccess.accept(null));
            return;
        }
        Schedulers.computation().createWorker().schedule(() -> {
            try {
                final DetectionResult result = langDetect.detectLanguage(source, null);
                // Always return the top-ranked language, even if isReliable=false.
                // "un" means CLD2 could not determine any language at all (e.g. random chars).
                final String lang = (result != null && result.language != null
                        && !result.language.isEmpty() && !"un".equals(result.language))
                        ? result.language : null;
                runCallback(() -> onSuccess.accept(lang));
            } catch (final Exception e) {
                Log.e(TAG + ": Language detection failed", e);
                runCallback(() -> onError.accept(e));
            }
        });
    }

    @Override
    public ITranslatorImpl getTranslator(final String sourceLanguage, final String targetLanguage) {
        if (!PIVOT_LANGUAGE.equals(sourceLanguage)) {
            ensureModelLoaded(sourceLanguage);
        }
        if (!PIVOT_LANGUAGE.equals(targetLanguage)) {
            ensureModelLoaded(targetLanguage);
        }
        return createTranslatorImpl(sourceLanguage, targetLanguage);
    }

    @Override
    public void getTranslatorWithDownload(final String sourceLanguage, final String targetLanguage,
            final Consumer<ITranslatorImpl> onSuccess, final Consumer<Exception> onError) {
        Schedulers.io().createWorker().schedule(() -> {
            try {
                if (!PIVOT_LANGUAGE.equals(sourceLanguage)) {
                    ensureOrDownload(sourceLanguage);
                }
                if (!PIVOT_LANGUAGE.equals(targetLanguage)) {
                    ensureOrDownload(targetLanguage);
                }
                runCallback(() -> onSuccess.accept(createTranslatorImpl(sourceLanguage, targetLanguage)));
            } catch (final Exception e) {
                runCallback(() -> onError.accept(e));
            }
        });
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private ITranslatorImpl createTranslatorImpl(final String sourceLang, final String targetLang) {
        return new ITranslatorImpl() {

            @Override
            public String getSourceLanguage() {
                return sourceLang;
            }

            @Override
            public String getTargetLanguage() {
                return targetLang;
            }

            @Override
            public void translate(final String source, final Consumer<String> onSuccess, final Consumer<Exception> onError) {
                Schedulers.computation().createWorker().schedule(() -> {
                    try {
                        final String[] result;
                        if (PIVOT_LANGUAGE.equals(sourceLang)) {
                            // en → target: direct
                            result = nativeLib.translateMultiple(new String[]{source}, pairKey(PIVOT_LANGUAGE, targetLang));
                        } else if (PIVOT_LANGUAGE.equals(targetLang)) {
                            // source → en: direct
                            result = nativeLib.translateMultiple(new String[]{source}, pairKey(sourceLang, PIVOT_LANGUAGE));
                        } else {
                            // source → en → target: pivot via English
                            result = nativeLib.pivotMultiple(
                                pairKey(sourceLang, PIVOT_LANGUAGE),
                                pairKey(PIVOT_LANGUAGE, targetLang),
                                new String[]{source}
                            );
                        }
                        final String translated = (result != null && result.length > 0) ? result[0] : source;
                        runCallback(() -> onSuccess.accept(translated));
                    } catch (final Exception e) {
                        Log.e(TAG + ": Translation failed", e);
                        runCallback(() -> onError.accept(e));
                    }
                });
            }

            @Override
            public void dispose() {
                // Models remain in native memory until the process exits.
                // NativeLib.cleanup() exists but is not called: reloading all models
                // after a cleanup would require reinitializing the service, which is
                // not yet implemented. Acceptable since users typically have 1-2 pairs loaded.
            }
        };
    }

    private static String pairKey(final String from, final String to) {
        return from + "-" + to;
    }

    private static File getPairDir(final String from, final String to) {
        return new File(LocalStorage.getBergamotDirectory(), from + "-" + to);
    }

    /**
     * Local file names for a pair direction.
     * Separate src/trg vocab files accommodate models with either shared or split vocabularies.
     */
    private static List<String> pairFileNames(final String from, final String to) {
        final String lc = from + to;
        return Arrays.asList(
            "model." + lc + ".intgemm.alphas.bin",
            "vocab." + lc + ".src.spm",
            "vocab." + lc + ".trg.spm",
            "lex.50.50." + lc + ".s2t.bin"
        );
    }

    /** Scan previously downloaded models on startup and load them into the native service */
    private void scanAvailableModels() {
        for (final String lang : SUPPORTED_LANGUAGES) {
            if (PIVOT_LANGUAGE.equals(lang)) {
                continue;
            }
            if (isPairComplete(lang)) {
                try {
                    Log.i(TAG + ": Loading cached models for " + lang);
                    loadPairIntoNative(lang);
                    synchronized (availableLock) {
                        availableLanguages.add(lang);
                        availableLanguages.add(PIVOT_LANGUAGE);
                    }
                    Log.i(TAG + ": Models loaded for " + lang);
                } catch (final Exception e) {
                    Log.e(TAG + ": Could not reload cached models for " + lang, e);
                }
            }
        }
        synchronized (availableLock) {
            Log.i(TAG + ": Scan complete. Available: " + availableLanguages);
        }
    }

    private boolean isPairComplete(final String lang) {
        return isDirectionComplete(lang, PIVOT_LANGUAGE) && isDirectionComplete(PIVOT_LANGUAGE, lang);
    }

    private boolean isDirectionComplete(final String from, final String to) {
        final File dir = getPairDir(from, to);
        for (final String fileName : pairFileNames(from, to)) {
            if (!new File(dir, fileName).exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Download all model files for one translation direction.
     * URLs are resolved from Mozilla's model registry; files are served gzip-compressed.
     */
    private void downloadPairFiles(final String from, final String to) throws IOException {
        if (isDirectionComplete(from, to)) {
            return;
        }
        final File dir = getPairDir(from, to);
        dir.mkdirs();

        final PairUrls urls = resolveModelUrls(from, to);
        final String lc = from + to;

        downloadFileIfMissing(urls.modelUrl,    new File(dir, "model." + lc + ".intgemm.alphas.bin"));
        downloadFileIfMissing(urls.vocabSrcUrl, new File(dir, "vocab." + lc + ".src.spm"));
        downloadFileIfMissing(urls.vocabTrgUrl, new File(dir, "vocab." + lc + ".trg.spm"));
        downloadFileIfMissing(urls.lexUrl,      new File(dir, "lex.50.50." + lc + ".s2t.bin"));
    }

    /**
     * Look up the download URLs for a direction from Mozilla's model registry.
     * Prefers architecture=base-memory with releaseStatus=Release.
     * Results are cached for the session to avoid repeated network fetches.
     */
    private PairUrls resolveModelUrls(final String from, final String to) throws IOException {
        final String key = pairKey(from, to);
        synchronized (pairUrlsCacheLock) {
            if (pairUrlsCache.containsKey(key)) {
                return pairUrlsCache.get(key);
            }
        }

        // Registry uses underscore notation (e.g. "zh_hant-en"), our internal codes use hyphens
        final String registryKey = toRegistryKey(from) + "-" + toRegistryKey(to);
        final String json = fetchRegistryJson();
        PairUrls result = null;
        try {
            // Registry format: { "baseUrl": "...", "models": { "de-en": [...], ... } }
            final JSONObject root = new JSONObject(json);
            final JSONObject modelsObj = root.optJSONObject("models");
            if (modelsObj != null) {
                final JSONArray arr = modelsObj.optJSONArray(registryKey);
                if (arr != null) {
                    result = pickBestModel(arr);
                }
            } else {
                // Fallback: root might directly be the pair map, or a flat array
                final JSONArray arr = root.optJSONArray(registryKey);
                if (arr != null) {
                    result = pickBestModel(arr);
                }
            }
        } catch (final JSONException e) {
            try {
                // Last resort: flat JSON array of model objects
                result = searchInArray(new JSONArray(json), from, to);
            } catch (final JSONException e2) {
                throw new IOException("Failed to parse model registry JSON", e2);
            }
        }

        if (result == null) {
            throw new IOException("No model found in registry for pair: " + registryKey);
        }

        synchronized (pairUrlsCacheLock) {
            pairUrlsCache.put(key, result);
        }
        return result;
    }

    /** Search a flat array of model objects for the best match for from→to */
    private PairUrls searchInArray(final JSONArray models, final String from, final String to) throws JSONException {
        PairUrls fallback = null;
        for (int i = 0; i < models.length(); i++) {
            final JSONObject m = models.getJSONObject(i);
            if (!from.equals(m.optString("sourceLanguage")) || !to.equals(m.optString("targetLanguage"))) {
                continue;
            }
            final PairUrls candidate = extractUrls(m);
            if (candidate == null) {
                continue;
            }
            final boolean isBaseMemory = "base-memory".equals(m.optString("architecture"));
            final boolean isRelease    = "Release".equals(m.optString("releaseStatus"));
            if (isBaseMemory && isRelease) {
                return candidate; // ideal match
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    /** Pick the best model from an array already filtered to one language pair */
    private PairUrls pickBestModel(final JSONArray models) throws JSONException {
        PairUrls fallback = null;
        for (int i = 0; i < models.length(); i++) {
            final JSONObject m = models.getJSONObject(i);
            final PairUrls candidate = extractUrls(m);
            if (candidate == null) {
                continue;
            }
            final boolean isBaseMemory = "base-memory".equals(m.optString("architecture"));
            final boolean isRelease    = "Release".equals(m.optString("releaseStatus"));
            if (isBaseMemory && isRelease) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private PairUrls extractUrls(final JSONObject m) throws JSONException {
        final JSONObject files = m.optJSONObject("files");
        if (files == null) {
            return null;
        }
        final JSONObject modelObj = files.optJSONObject("model");
        final JSONObject lexObj   = files.optJSONObject("lexicalShortlist");
        if (modelObj == null || lexObj == null) {
            return null;
        }
        final String modelPath = modelObj.optString("path");
        final String lexPath   = lexObj.optString("path");
        if (modelPath.isEmpty() || lexPath.isEmpty()) {
            return null;
        }
        // Mozilla registry uses either a shared "vocab" or separate "srcVocab"/"trgVocab"
        final JSONObject vocabObj    = files.optJSONObject("vocab");
        final JSONObject srcVocabObj = files.optJSONObject("srcVocab");
        final JSONObject trgVocabObj = files.optJSONObject("trgVocab");
        final String vocabSrcPath;
        final String vocabTrgPath;
        if (vocabObj != null) {
            vocabSrcPath = vocabObj.optString("path");
            vocabTrgPath = vocabSrcPath; // shared vocabulary
        } else if (srcVocabObj != null && trgVocabObj != null) {
            vocabSrcPath = srcVocabObj.optString("path");
            vocabTrgPath = trgVocabObj.optString("path");
        } else {
            return null;
        }
        if (vocabSrcPath.isEmpty() || vocabTrgPath.isEmpty()) {
            return null;
        }
        return new PairUrls(
            MODEL_BUCKET_URL + modelPath,
            MODEL_BUCKET_URL + vocabSrcPath,
            MODEL_BUCKET_URL + vocabTrgPath,
            MODEL_BUCKET_URL + lexPath
        );
    }

    /** Fetch models.json, caching the raw JSON string for the lifetime of this session */
    private String fetchRegistryJson() throws IOException {
        synchronized (registryLock) {
            if (cachedRegistryJson != null) {
                return cachedRegistryJson;
            }
            Log.i(TAG + ": Fetching model registry");
            final InputStream stream = Network.getResponseStream(Network.getRequest(MODEL_REGISTRY_URL));
            if (stream == null) {
                throw new IOException("Failed to fetch model registry");
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            stream.close();
            cachedRegistryJson = baos.toString("UTF-8");
            return cachedRegistryJson;
        }
    }

    private void downloadFileIfMissing(final String url, final File dest) throws IOException {
        if (dest.exists()) {
            return;
        }
        Log.i(TAG + ": Downloading " + url + " -> " + dest.getName());
        downloadFile(url, dest);
    }

    /** Download a single file, automatically decompressing if the URL ends in .gz */
    private void downloadFile(final String fileUrl, final File dest) throws IOException {
        final InputStream raw = Network.getResponseStream(Network.getRequest(fileUrl));
        if (raw == null) {
            throw new IOException("Failed to download: " + dest.getName());
        }
        try (InputStream in = fileUrl.endsWith(".gz") ? new GZIPInputStream(raw) : raw;
             FileOutputStream out = new FileOutputStream(dest)) {
            final byte[] buf = new byte[65536];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }

    private void loadPairIntoNative(final String language) throws IOException {
        loadDirectionIntoNative(language, PIVOT_LANGUAGE);
        loadDirectionIntoNative(PIVOT_LANGUAGE, language);
    }

    private void loadDirectionIntoNative(final String from, final String to) throws IOException {
        final File dir = getPairDir(from, to);
        final String config = buildModelConfig(from, to, dir);
        nativeLib.loadModelIntoCache(config, pairKey(from, to));
    }

    private String buildModelConfig(final String from, final String to, final File dir) {
        final String lc = from + to;
        final String modelFile    = new File(dir, "model." + lc + ".intgemm.alphas.bin").getAbsolutePath();
        final String lexFile      = new File(dir, "lex.50.50." + lc + ".s2t.bin").getAbsolutePath();
        final String vocabSrcFile = new File(dir, "vocab." + lc + ".src.spm").getAbsolutePath();
        final String vocabTrgFile = new File(dir, "vocab." + lc + ".trg.spm").getAbsolutePath();
        return "beam-size: 1\n"
            + "normalize: 1.0\n"
            + "word-penalty: 0\n"
            + "max-length-break: 128\n"
            + "mini-batch-words: 1024\n"
            + "workspace: 128\n"
            + "max-length-factor: 2.0\n"
            + "skip-cost: false\n"
            + "cpu-threads: 0\n"
            + "quiet: true\n"
            + "quiet-translation: true\n"
            + "gemm-precision: int8shiftAlphaAll\n"
            + "models:\n"
            + "  - " + modelFile + "\n"
            + "vocabs:\n"
            + "  - " + vocabSrcFile + "\n"
            + "  - " + vocabTrgFile + "\n"
            + "shortlist:\n"
            + "  - " + lexFile + "\n"
            + "  - false\n";
    }

    private void ensureModelLoaded(final String language) {
        synchronized (availableLock) {
            if (!availableLanguages.contains(language)) {
                throw new IllegalStateException("Model not available for language: " + language);
            }
        }
    }

    private void ensureOrDownload(final String language) throws IOException {
        final boolean available;
        synchronized (availableLock) {
            available = availableLanguages.contains(language);
        }
        if (!available) {
            downloadPairFiles(language, PIVOT_LANGUAGE);
            downloadPairFiles(PIVOT_LANGUAGE, language);
            loadPairIntoNative(language);
            synchronized (availableLock) {
                availableLanguages.add(language);
                availableLanguages.add(PIVOT_LANGUAGE);
            }
        }
    }

    private static void deleteDir(final File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File f : files) {
                f.delete();
            }
        }
        dir.delete();
    }

    private void runCallback(final Runnable r) {
        final Scheduler s = callbackScheduler != null ? callbackScheduler : AndroidSchedulers.mainThread();
        s.createWorker().schedule(r);
    }
}
