package org.eztarget.realay.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by michel on 24/04/15.
 *
 */
public class URLConnectionHelper {

    private static final String TAG = URLConnectionHelper.class.getSimpleName();

    private static final boolean DEBUG_URL = false;

    public static final String RETRY = "retry_result_code";

    public static final int NUMBER_OF_RETRIES = 3;

    private static final String API_BASE_URL = "http://rldb.easy-target.org/";

    private static final String STATUS_TAG = "st";

    private static final int READ_TIMEOUT = 8 * 1000;

    private static final int CONNECT_TIMEOUT = 8 * 1000;

    private static final String MULTIPART_BOUNDARY = "0xKhTmLbOuNdArY";
    private static final String MULTIPART_START = "--" + MULTIPART_BOUNDARY + "\r\n";
    private static final String MULTIPART_BOUNDARY_RETURN = "\r\n--" + MULTIPART_BOUNDARY + "\r\n";
    private static final String MULTIPART_END = MULTIPART_BOUNDARY_RETURN + "--";
    private static final String MULTIPART_CONTENT_TYPE_JPG = "Content-Type: image/jpg\r\n\r\n";

    /**
     * @param context Context in which to build and verify the connection
     * @param apiCall Server script name
     * @param parameter HTTP parameters as keys and the respective values
     * @param successStatus Verifies the correct API call returns the expected result
     * @param arrayTag Object key of the requested Array
     *
     * @return Requested Array, if the right success code and the Array data was received;
     * An empty Array, if the right success code was received but the Array data was not;
     * Null, if a problem occurred.
     */
    public static JSONArray getJsonArray(
            final Context context,
            final String apiCall,
            final HashMap<String, String> parameter,
            final String successStatus,
            final String arrayTag
    ) {
        // Get the literal response from the server.
        // Workaround due to connection bug in some Android versions:
        // If performCall() returns RETRY code, reattempt the call several times.
        String jsonResult = performCall(context, apiCall, parameter);
        if (jsonResult == null || jsonResult.length() < 1) {
            return null;
        } else if (jsonResult.equals(RETRY)) {
            for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
//                Log.d(TAG, "Retry: " + i);
                jsonResult = performCall(context, apiCall, parameter);
                if (!RETRY.equals(jsonResult)) break;
            }
        }

        // Look for the Status object first.
        final String jsonStatus;
        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonResult);
            jsonStatus = jsonObject.getString(STATUS_TAG);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        // Verify the right Status code was received before trying to find the requested Object.
        if (jsonStatus == null) {
            Log.e(TAG, "Could not find JSON STATUS object.");
            return null;
        } else if (!jsonStatus.equals(successStatus)) {
            Log.e(TAG, "JSON status: " + jsonStatus);
            return null;
        } else if (jsonObject.has(arrayTag)){
            final JSONArray resultArray;
            try {
                resultArray = jsonObject.getJSONArray(arrayTag);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            return resultArray;
        } else {
            return new JSONArray();
        }
    }

    public static long getLong(
            Context context,
            final String apiCall,
            final HashMap<String, String> parameter,
            final String successStatus,
            final String valueTag
    ) {
        // Get the literal response from the server.
        // Workaround due to connection bug in some Android versions:
        // If performCall() returns RETRY code, reattempt the call several times.
        String jsonResult = performCall(context, apiCall, parameter);
        if (TextUtils.isEmpty(jsonResult)) {
            return -900L;
        } else if (jsonResult.equals(RETRY)) {
            for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
//                Log.d(TAG, "Retry: " + i);
                jsonResult = performCall(context, apiCall, parameter);
                if (!RETRY.equals(jsonResult)) break;
            }
        }

        final String jsonStatus;
        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonResult);
            jsonStatus = jsonObject.getString(STATUS_TAG);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return -1000L;
        }

        if (jsonStatus == null) {
            Log.e(TAG, "Could not find JSON STATUS object.");
            return -1100L;
        }

        if (!jsonStatus.equals(successStatus)) {
            Log.e(TAG, "JSON status: " + jsonStatus);
            return -1200L;
        } else {
            final long result;
            try {
                result = jsonObject.getLong(valueTag);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return -1900L;
            }
            return result;
        }
    }

    public static JSONObject getJsonObject(
            Context context,
            final String apiCall,
            final HashMap<String, String> parameter,
            final String jsonStatusOk,
            final String jsonObjectTag
    ) {
        // Get the literal response from the server.
        // Workaround due to connection bug in some Android versions:
        // If performCall() returns RETRY code, reattempt the call several times.
        String jsonResult = performCall(context, apiCall, parameter);
        if (TextUtils.isEmpty(jsonResult)) {
            return null;
        } else if (jsonResult.equals(RETRY)) {
            for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
//                Log.d(TAG, "Retry: " + i);
                jsonResult = performCall(context, apiCall, parameter);
                if (!RETRY.equals(jsonResult)) break;
            }
        }

        // Look for the Status object first.
        final String jsonStatus;
        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonResult);
            jsonStatus = jsonObject.getString(STATUS_TAG);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        // Verify the right Status code was received before trying to find the requested Object.
        if (jsonStatus == null) {
            Log.e(TAG, "Could not find JSON STATUS object.");
            return null;
        } else if (!jsonStatus.equals(jsonStatusOk)) {
            Log.e(TAG, "JSON status: " + jsonStatus);
            return null;
        } else {
            final JSONObject resultObject;
            try {
                resultObject = jsonObject.getJSONObject(jsonObjectTag);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            return resultObject;
        }
    }

    public static boolean performCall(
            final Context context,
            final String apiCall,
            final HashMap<String, String> parameter,
            final String successStatus
    ) {
        String jsonResult = performCall(context, apiCall, parameter);

        if (TextUtils.isEmpty(jsonResult)) {
            return false;
        } else if (jsonResult.equals(RETRY)) {
            for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
//                Log.d(TAG, "Retry: " + i);
                jsonResult = performCall(context, apiCall, parameter);
                if (!RETRY.equals(jsonResult)) break;
            }
        }

        final String jsonStatus;
        final JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonResult);
            jsonStatus = jsonObject.getString(STATUS_TAG);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return false;
        }

        if (jsonStatus == null) {
            Log.e(TAG, "Could not find JSON STATUS object.");
            return false;
        }

        if (jsonStatus.equals(successStatus)) {
            return true;
        } else {
            Log.e(TAG, "JSON status: " + jsonStatus);
            return false;
        }
    }

    public static HttpURLConnection getConnection(
            final Context context,
            final String apiCall,
            final HashMap<String, String> parameter
    ) {
        if (TextUtils.isEmpty(apiCall)) return null;

        if (!DeviceStatusHelper.isConnected(context)) return null;

        final URL url;
        try {
            url = new URL(API_BASE_URL + apiCall);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        final HttpURLConnection httpConnection;
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        httpConnection.setReadTimeout(READ_TIMEOUT);
        httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);

        try {
            httpConnection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        httpConnection.setRequestProperty("Accept-Encoding", "");
        httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        final StringBuilder bodyBuilder;
        if (parameter != null && parameter.size() > 0) {
            bodyBuilder = new StringBuilder();
            final String[] keys = new String[parameter.size()];
            parameter.keySet().toArray(keys);
            boolean isFirstParam = true;

            for (final String key : keys) {
                if (isFirstParam) isFirstParam = false;
                else bodyBuilder.append("&");
                if (key != null) bodyBuilder.append(key).append("=").append(parameter.get(key));
            }

            final String parameters = bodyBuilder.toString();
            if (DEBUG_URL) Log.d(TAG, url.toString() + "?" + parameters);

            httpConnection.setFixedLengthStreamingMode(parameters.getBytes().length);
            final PrintWriter out;
            try {
                out = new PrintWriter(httpConnection.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return null;
            }
            out.print(parameters);
            out.close();
        }

        return httpConnection;
    }

    private static String performCall(
            final Context context,
            final String apiCall,
            final HashMap<String, String> parameter
    ) {
        final HttpURLConnection httpConnection = getConnection(context, apiCall, parameter);
        if (httpConnection == null) return null;
        else return getResponse(httpConnection);
    }

    public static String putImage(
            final String targetURL,
            final String hiResParam,
            final File hiResImage,
            final String loResParam,
            final File loResImage,
            final String additionalParts
    ) {
        final String hiResFileHeader =
                "Content-Disposition: form-data; name=\""
                        + hiResParam
                        + "\"; filename=\"upimg.jpg\"\r\n"
                        + MULTIPART_CONTENT_TYPE_JPG;

        final String loResFileHeader =
                "Content-Disposition: form-data; name=\""
                        + loResParam
                        + "\"; filename=\"upimgs.jpg\"\r\n"
                        + MULTIPART_CONTENT_TYPE_JPG;

        final URL url;
        try {
            url = new URL(targetURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        final HttpURLConnection httpConnection;
        try {
            httpConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        httpConnection.setUseCaches(false);
        try {
            httpConnection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
            return null;
        }
        httpConnection.setRequestProperty(
                "Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY
        );

        FileInputStream hiResStream = null;
        FileInputStream loResStream = null;
        DataOutputStream outputStream = null;

        try {
            outputStream = new DataOutputStream(httpConnection.getOutputStream());

            outputStream.writeBytes(MULTIPART_START);
            outputStream.writeBytes(hiResFileHeader);
            hiResStream = new FileInputStream(hiResImage);
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = hiResStream.read(buf)) != -1; ) {
                outputStream.write(buf, 0, readNum);
            }
            hiResStream.close();
            outputStream.writeBytes(MULTIPART_BOUNDARY_RETURN);

            outputStream.writeBytes(loResFileHeader);
            loResStream = new FileInputStream(loResImage);
            for (int readNum; (readNum = loResStream.read(buf)) != -1; ) {
                outputStream.write(buf, 0, readNum);
            }
            loResStream.close();
            outputStream.writeBytes(MULTIPART_BOUNDARY_RETURN);

            outputStream.writeBytes(additionalParts);
            outputStream.writeBytes(MULTIPART_END);

        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (hiResStream != null) hiResStream.close();
                if (loResStream != null) loResStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return getResponse(httpConnection);
    }

    private static String getResponse(final HttpURLConnection httpConnection) {
        final int responseCode;
        final InputStream responseStream;
        try {
            responseCode = httpConnection.getResponseCode();
            responseStream = httpConnection.getInputStream();
        } catch (EOFException e) {
            return RETRY;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            httpConnection.disconnect();
            return null;
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.e(TAG, "Response code: " + responseCode);
            httpConnection.disconnect();
            return null;
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
        final StringBuilder responseBuilder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpConnection.disconnect();
            try {
                reader.close();
                responseStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseBuilder.toString();
    }

    public static String buildEntityPart(final String param, final String value) {
        return "Content-Disposition: form-data; name=\""
                + param
                + "\"\r\n\r\n"
                + value
                + MULTIPART_BOUNDARY_RETURN;
    }
}
