package com.junkiesoup.care4clean;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by makn on 19-09-2016.
 */
public class Communication {
    Context context;
    RequestQueue queue;
    public final static String URL_BASE = "https://185.93.195.194/";
    public final static String URL_CREATEUSER = "https://185.93.195.194/createuser.php";
    public final static String URL_UPLOADIMAGE = "https://185.93.195.194/uploadpic.php";

    public Communication(Context context)
    {
        this.context = context;
    }

    public class CustomHostnameVerifier implements HostnameVerifier
    {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


    public void setupSSLCertificate()
    {
        String ur = URL_BASE;
        System.out.println("setting up ssl ca");
        URL url = null;
        Certificate ca = null;
        HttpsURLConnection.setDefaultHostnameVerifier(new CustomHostnameVerifier());

        HttpsURLConnection urlConnection= null;

        try {
            url = new URL(ur);
        }
        catch (MalformedURLException e)
        {
            Log.d("MalformedURL",e.toString());
        }

        // Load CAs from an InputStream
// (could be from a resource or ByteArrayInputStream or ...)
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = context.getResources().openRawResource(
                    context.getResources().getIdentifier("cacert",
                            "raw", context.getPackageName()));

           // InputStream caInput = new BufferedInputStream(new FileInputStream(R.raw.cacert));
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }
        } catch (Exception e)
        {
            Log.d("CertificationError",e.toString());
        }
        System.out.println("factory done!");


// Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

        }
        catch (Exception e)
        {
            Log.d("KeyStoreException",e.toString());
        }
        System.out.println("KeyStore done!");

// Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager

            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, tmf.getTrustManagers(), null);

// Tell the URLConnection to use a SocketFactory from our SSLContext
         //   URL url = new URL("https://certs.cac.washington.edu/CAtest/");
            urlConnection =
                    (HttpsURLConnection) url.openConnection();
            urlConnection.setSSLSocketFactory(sslcontext.getSocketFactory());
        }
        catch (Exception e)
        {
            Log.d("TrustManagerException",e.toString());
        }
        System.out.println("Trustmanager done!");
        if ("https".equals(url.getProtocol())) {
           // ((HttpsURLConnection)urlConnection).setSSLSocketFactory(mSslSocketFactory);
            urlConnection.setHostnameVerifier(new CustomHostnameVerifier());
        }

        queue = Volley.newRequestQueue(context, new HurlStack(null, urlConnection.getSSLSocketFactory()));

    }

    public void uploadPicture(final Bitmap bitmap, final String token,
                              final int caseId, final String description,
                              final String imageName)
    {
        ((MainActivity)context).enableUpload(false);
        ((MainActivity)context).showProgressBar();
        String url = URL_UPLOADIMAGE;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        String text;
                        text = response.replaceAll("\\n",""); // Prevent empty lines in toast
                        text = text.substring(0,1).toUpperCase() + text.substring(1); // Convert first letter of string to uppercase (prettier)
                        Toast.makeText(context,text, Toast.LENGTH_SHORT).show();
                        Drawable ph = context.getResources().getDrawable(com.junkiesoup.care4clean.R.drawable.image_placeholder);
                        ((MainActivity)context).clearApp(ph);
                        ((MainActivity)context).enableUpload(true);
                        ((MainActivity)context).showProgressBar(false);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ErrorResponseServer",error.toString());
                if (error.getMessage()!=null) {
                    Log.d("ErrorResponseServer", error.getMessage().toString());
                    String text;
                    text = error.getMessage().toString().replaceAll("\\n",""); // Prevent empty lines in toast
                    text = text.substring(0,1).toUpperCase() + text.substring(1);
                    Toast.makeText(context,text, Toast.LENGTH_SHORT).show();
                    ((MainActivity)context).enableUpload(true);
                    ((MainActivity)context).showProgressBar(false);
                }
                NetworkResponse response = error.networkResponse;
                if(response != null && response.data != null){
                    switch(response.statusCode){
                        case 400:
                            String errorMessage = new String(response.data);
                            if(errorMessage != null) {
                                Log.d("ErrorServerUpload", errorMessage);
                                String text;
                                text = errorMessage.replaceAll("\\n",""); // Prevent empty lines in toast
                                text = text.substring(0,1).toUpperCase() + text.substring(1);
                                Toast.makeText(context,text,Toast.LENGTH_LONG).show();
                                ((MainActivity)context).invalidCaseId();
                                ((MainActivity)context).enableUpload(true);
                                ((MainActivity)context).showProgressBar(false);
                            }
                            break;
                    }
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                JSONObject json = new JSONObject();

                // Encode bitmap
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream .toByteArray();
                final String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

                // Get token
                SharedPreferences prefs = context.getSharedPreferences("myPrefs", MODE_PRIVATE);

                try {
                    json.put("base64", encoded);
                    json.put("token",prefs.getString("token",""));
                    json.put("case_id",caseId);
                    json.put("description",description);
                    json.put("name",imageName);
                }
                catch (JSONException e)
                {
                    Log.d("JsonException",e.getMessage().toString());
                }
                map.put("json", json.toString());
                return map;
            }
        };

        int socketTimeout = 20000;//20 seconds
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        // Add the request to the RequestQueue.

        queue.add(stringRequest);
    }

    public void CreateUser(final String userName, final String token) {
        ((MainActivity)context).enableUpload(false);
        ((MainActivity)context).showProgressBar(true,(context.getString(R.string.button_status_creating)));
        String url = URL_CREATEUSER;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        SharedPreferences prefs = context.getSharedPreferences("myPrefs", MODE_PRIVATE);
                        //save the preferences.
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("token",token);
                        editor.commit();
                        Toast.makeText(context,response, Toast.LENGTH_SHORT).show();
                        Log.d("Token creation", (prefs.contains("token")) ? "Token created: "+token : "Token not created");
                        ((MainActivity)context).enableUpload(true);
                        ((MainActivity)context).showProgressBar(false);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ErrorResponseServer",error.toString());
                if (error.getMessage()!=null) {
                    Log.d("ErrorResponseServer", error.getMessage().toString());
                    Toast.makeText(context,error.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    ((MainActivity)context).enableUpload(true);
                    ((MainActivity)context).showProgressBar(false);
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                JSONObject json = new JSONObject();

                try {
                    json.put("token", token);
                    json.put("username",userName);
                }
                catch (JSONException e)
                {
                    Log.d("JsonException",e.getMessage().toString());
                }
                map.put("json", json.toString());
                System.out.println("JSON:"+json.toString());
                return map;
            }
        };

        int socketTimeout = 20000;//20 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        // Add the request to the RequestQueue.

        queue.add(stringRequest);

    }
}
