package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Service client for GWZ to GWC compilation via Wherigo Foundation compiler
 */
public final class GwzCompileService {

    private static final String TAG = "GwzCompileService";
    
    // Patterns to extract ASP.NET ViewState fields
    private static final Pattern VIEWSTATE_PATTERN = Pattern.compile("<input type=\"hidden\" name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"([^\"]+)\"");
    private static final Pattern VIEWSTATE_GENERATOR_PATTERN = Pattern.compile("<input type=\"hidden\" name=\"__VIEWSTATEGENERATOR\" id=\"__VIEWSTATEGENERATOR\" value=\"([^\"]+)\"");
    private static final Pattern EVENT_VALIDATION_PATTERN = Pattern.compile("<input type=\"hidden\" name=\"__EVENTVALIDATION\" id=\"__EVENTVALIDATION\" value=\"([^\"]+)\"");

    private GwzCompileService() {
        // Utility class
    }

    /**
     * Compile a GWZ file using the Wherigo Foundation compiler
     *
     * @param gwzInputStream input stream of the GWZ file to compile
     * @param outputFile file where the compiled GWC should be saved
     * @return error message if failed, null if successful
     */
    @Nullable
    public static String compileGwzFile(final InputStream gwzInputStream, final File outputFile) {
        final String serviceUrl = getServiceUrl();
        Log.i(TAG + " Compiling GWZ file using service: " + serviceUrl);

        try {
            final byte[] gwzData = readInputStream(gwzInputStream);
            if (gwzData == null || gwzData.length == 0) {
                return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                    "Failed to read GWZ file");
            }

            final ViewStateFields viewState = fetchViewState(serviceUrl);
            if (viewState == null) {
                return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                    "Failed to extract ViewState from compiler page");
            }

            return uploadAndCompile(serviceUrl, gwzData, viewState, outputFile);

        } catch (final IOException e) {
            Log.e(TAG + " Error communicating with compile service", e);
            return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                e.getMessage());
        }
    }

    private static String getServiceUrl() {
        final String serviceUrl = Settings.getWherigoGwzCompileServiceUrl();
        Log.i(TAG + " Using compiler URL: " + serviceUrl);
        return serviceUrl;
    }

    @Nullable
    private static ViewStateFields fetchViewState(final String serviceUrl) throws IOException {
        final HttpRequest getRequest = new HttpRequest().uri(serviceUrl);
        try (HttpResponse getResponse = getRequest.request().blockingGet()) {
            if (!getResponse.isSuccessful()) {
                return null;
            }
            final String compilerPage = getResponse.getBodyString();
            
            final String viewState = extractPattern(compilerPage, VIEWSTATE_PATTERN);
            final String viewStateGenerator = extractPattern(compilerPage, VIEWSTATE_GENERATOR_PATTERN);
            final String eventValidation = extractPattern(compilerPage, EVENT_VALIDATION_PATTERN);
            
            if (viewState == null || viewStateGenerator == null) {
                return null;
            }
            
            return new ViewStateFields(viewState, viewStateGenerator, eventValidation);
        }
    }

    @Nullable
    private static String uploadAndCompile(final String serviceUrl, final byte[] gwzData, 
                                          final ViewStateFields viewState, final File outputFile) throws IOException {
        final File tempGwzFile = createTempFile(gwzData);
        try {
            final Parameters formParams = buildFormParameters(viewState);
            return postAndDownload(serviceUrl, formParams, tempGwzFile, outputFile);
        } finally {
            if (tempGwzFile.exists()) {
                tempGwzFile.delete();
            }
        }
    }

    private static File createTempFile(final byte[] gwzData) throws IOException {
        final File tempGwzFile = File.createTempFile("gwz_upload_", ".gwz");
        try (FileOutputStream fos = new FileOutputStream(tempGwzFile)) {
            fos.write(gwzData);
            fos.flush();
        }
        return tempGwzFile;
    }

    private static Parameters buildFormParameters(final ViewStateFields viewState) {
        final Parameters formParams = new Parameters()
            .put("__VIEWSTATE", viewState.viewState)
            .put("__VIEWSTATEGENERATOR", viewState.viewStateGenerator);
        
        if (viewState.eventValidation != null) {
            formParams.put("__EVENTVALIDATION", viewState.eventValidation);
        }
        
        formParams.put("ctl00$MainContent$rptr$ctl10$chkCompile", "on");
        formParams.put("ctl00$MainContent$rptr$ctl10$chkDownload", "on");
        formParams.put("ctl00$MainContent$hdnSelectedCompile", "WhereYouGo");
        formParams.put("ctl00$MainContent$hdnSelectedDownload", "WhereYouGo");
        formParams.put("ctl00$MainContent$btnCompile", "Test Compiling Cartridge");
        
        return formParams;
    }

    @Nullable
    private static String postAndDownload(final String serviceUrl, final Parameters formParams, 
                                         final File tempGwzFile, final File outputFile) throws IOException {
        final HttpRequest postRequest = new HttpRequest()
            .uri(serviceUrl)
            .method(HttpRequest.Method.POST);

        try (HttpResponse response = postRequest.bodyForm(formParams, "ctl00$MainContent$upFile", 
                                                          "application/zip", tempGwzFile).request().blockingGet()) {
            if (!response.isSuccessful()) {
                Log.e(TAG + " Compile service returned error: " + response.getStatusCode());
                return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                    "HTTP " + response.getStatusCode());
            }

            final String contentTypeError = validateContentType(response);
            if (contentTypeError != null) {
                return contentTypeError;
            }

            return saveCompiledFile(response, outputFile);
        }
    }

    @Nullable
    private static String validateContentType(final HttpResponse response) {
        final okhttp3.Response rawResponse = response.getResponse();
        final String contentType = rawResponse.header("Content-Type", "");
        
        if (!contentType.contains("application/octet-stream") && !contentType.contains("application/x-gwc")) {
            final String responseBody = response.getBodyString();
            if (responseBody.contains("error") || responseBody.contains("Error")) {
                return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                    "Compilation failed - check cartridge format");
            }
            return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                "Unexpected response type: " + contentType);
        }
        return null;
    }

    @Nullable
    private static String saveCompiledFile(final HttpResponse response, final File outputFile) throws IOException {
        final okhttp3.Response rawResponse = response.getResponse();
        if (rawResponse.body() == null) {
            return LocalizationUtils.getString(R.string.wherigo_gwz_compile_service_error, 
                "Empty response from service");
        }

        try (InputStream responseStream = new BufferedInputStream(rawResponse.body().byteStream());
             OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            
            final byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = responseStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        Log.i(TAG + " Successfully compiled GWZ file to: " + outputFile.getAbsolutePath());
        return null;
    }

    /**
     * Check if the compile service is configured and available
     * Tests connection to the configured compiler service
     *
     * @return true if service is reachable, false otherwise
     */
    public static boolean isServiceConfigured() {
        try {
            final String serviceUrl = Settings.getWherigoGwzCompileServiceUrl();
            if (StringUtils.isBlank(serviceUrl)) {
                return false;
            }
            
            // Test connection by fetching the compiler page
            final HttpRequest testRequest = new HttpRequest().uri(serviceUrl);
            try (HttpResponse testResponse = testRequest.request().blockingGet()) {
                return testResponse.isSuccessful();
            }
        } catch (final Exception e) {
            Log.w(TAG + " Service connection test failed: " + e.getMessage());
            return false;
        }
    }

    private static String extractPattern(final String html, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static byte[] readInputStream(final InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        int bytesRead;
        final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        
        return output.toByteArray();
    }

    private static class ViewStateFields {
        final String viewState;
        final String viewStateGenerator;
        final String eventValidation;

        ViewStateFields(final String viewState, final String viewStateGenerator, final String eventValidation) {
            this.viewState = viewState;
            this.viewStateGenerator = viewStateGenerator;
            this.eventValidation = eventValidation;
        }
    }
}

