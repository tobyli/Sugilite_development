package edu.cmu.hcii.sugilite.study;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by toby on 4/27/17.
 */

public class StudyDataScriptFileUploadTask extends AsyncTask<String, Void, String> {
    private Context context;
    public StudyDataScriptFileUploadTask(Context context){
        this.context = context;
    }

    private Exception exception;
    //strings[0] is the path of the file, strings[1] is the url to send the file to, strings[2] is the clientidentifier
    protected String doInBackground(String... strings) {
        try {
            URL url = new URL(strings[1]);
            FileInputStream fileInputStream = new FileInputStream(strings[0]);
            String timeStampString = strings[3];
            File file = new File(strings[0]);
            Calendar c = Calendar.getInstance();
            String fileName = strings[2] + "_" + timeStampString + "_" + file.getName();


            //this is ugly :(
            String isTrackingEvent = "False";
            if(file.getName().contains("TrackingEvent"))
                isTrackingEvent = "True";

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            // open a URL connection to the Servlet
            HttpURLConnection conn = null;
            DataOutputStream dos = null;

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                    + fileName + "\"" + lineEnd);

            dos.writeBytes(lineEnd);
            int bytesAvailable, bytesRead, bufferSize;
            int maxBufferSize = 15 * 1024 * 1024;
            int serverResponseCode = 0;
            byte[] buffer;
            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {

                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Log.i("uploadFile", "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            fileInputStream.close();
            dos.flush();
            dos.close();

            //get response from server
            InputStream serverInputStream = conn.getInputStream();
            int ch;

            StringBuffer inputBuffer =new StringBuffer();
            while( ( ch = serverInputStream.read() ) != -1 ){ inputBuffer.append((char) ch); }
            String response = inputBuffer.toString();
            serverInputStream.close();


            return isTrackingEvent + "_" + serverResponseMessage + ": " + serverResponseCode + "\n" + response;


        }
        catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
            return "connection failed";
        }


    }


    protected void onPostExecute(String result) {
        //do stuff
        System.out.println("thread finished");
        //System.out.println(result);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //TODO: potential int overflow after 21 years
        SharedPreferences.Editor editor = prefs.edit();
        //TODO: that's an ugly solution :( fix if have time
        if(result.startsWith("True") && result.endsWith("success")){
            editor.putInt("lastSuccessfulUpdate", (int) (Calendar.getInstance().getTimeInMillis() / 1000));
            editor.commit();
        }
        //Toast.makeText(context, result, Toast.LENGTH_SHORT).show();


    }
}
