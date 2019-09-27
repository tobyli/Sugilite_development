package edu.cmu.hcii.sugilite.study;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

import static edu.cmu.hcii.sugilite.R.id.textView;

/**
 * Created by toby on 4/27/17.
 */

public class StudyDataUploadManager {
    private Context context;
    private String clientIdentifier;
    private String uploadURL;
    private BluetoothAdapter mBluetoothAdapter;
    private SugiliteData sugiliteData;

    public StudyDataUploadManager(Context context, SugiliteData sugiliteData){
        this.context = context;
        String deviceModel = Build.MODEL;
        this.sugiliteData = sugiliteData;
        this.clientIdentifier = deviceModel + getLocalBluetoothName() + "_" + getOwnerName();
        uploadURL = StudyConst.STUDY_UPLOAD_URL;
    }

    public void uploadScript(String filePath, long timeStamp) throws IOException, FileNotFoundException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        Date time = new Date(timeStamp);
        String timeStampString = dateFormat.format(time);
        (new StudyDataScriptFileUploadTask(context)).execute(filePath, uploadURL, clientIdentifier, timeStampString);
    }

    public void uploadScriptJSON(SugiliteStartingBlock script) throws IOException{
        SugiliteBlockJSONProcessor jsonProcessor = new SugiliteBlockJSONProcessor(context);
        String directoryPath = context.getFilesDir().getPath().toString();
        String filePath = directoryPath + "/" + script.getScriptName() + ".json";
        PrintWriter out = new PrintWriter(new FileOutputStream(filePath, false));
        out.print(jsonProcessor.scriptToJson(script));
        out.flush();
        out.close();
        uploadScript(filePath, script.getCreatedTime());
    }
    private String getLocalBluetoothName(){
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(mBluetoothAdapter == null){
                return "NULL";
            }
        }
        String name = mBluetoothAdapter.getName();
        if(name == null){
            System.out.println("Name is null!");
            name = mBluetoothAdapter.getAddress();
        }
        return name;
    }

    private String getOwnerName(){
        try {
            Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            c.moveToFirst();
            String name = c.getString(c.getColumnIndex("display_name"));
            c.close();
            if (name != null)
                return name;
            else
                return "NULL";
        }
        catch (Exception e){
            e.printStackTrace();
            return "NULL";
        }
    }

    /**
     * used for uploading the study data
     */
    private class StudyDataScriptFileUploadTask extends AsyncTask<String, Void, String> {
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


        }
    }


}
