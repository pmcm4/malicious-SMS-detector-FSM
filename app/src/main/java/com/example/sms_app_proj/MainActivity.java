package com.example.sms_app_proj;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.BlockedNumberContract;
import android.provider.BlockedNumberContract.BlockedNumbers;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText etPhone, etMsg;
    Button btnSendSMS, btnReadSMS;
    ListView lvSMS;
    static ArrayList<String> smsData = new ArrayList<String>();
    ArrayAdapter arrayAdapter;


    private List<String> blockedNumbers;
    private ArrayAdapter<String> adapter;
    private ListView listView;


    private final int REQ_CODE_PERMISSION_SEND_SMS = 121;
    private final int REQ_CODE_PERMISSION_READ_SMS = 122;
    private final int REQ_CODE_PERMISSION_RECEIVE_SMS = 123;

    private final String SERVER = "https://smsappproj.000webhostapp.com/save_sms0.php";

    private FirebaseAnalytics mFirebaseAnalytics;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //context = this;

        etPhone = findViewById(R.id.etPhoneNum);
        etMsg = findViewById(R.id.etMsg);

        btnReadSMS = findViewById(R.id.btnRead);
        btnSendSMS = findViewById(R.id.btnSend);

        lvSMS = findViewById(R.id.lvSMS);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, smsData);
        lvSMS.setAdapter(arrayAdapter);

        btnReadSMS.setEnabled(false);
        btnSendSMS.setEnabled(false);

        // Register the broadcast receiver
        SmsReceiver smsReceiver = new SmsReceiver(this);
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);


        Button button = findViewById(R.id.showBlocked);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BlockedNum.class);
                startActivity(intent);
            }
        });



        /*Cursor cursor = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cursor = context.getContentResolver().query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                context,
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] {BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                new int[] {android.R.id.text1},
                0
        );
        ListView listView = (ListView) findViewById(R.id.blocked);
        listView.setAdapter(adapter);
        */


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //permission check on SENDING SMS
        if (checkPermission(Manifest.permission.SEND_SMS)) {
            btnSendSMS.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQ_CODE_PERMISSION_SEND_SMS);
        }

        //permission check on READ SMS
        if (checkPermission(Manifest.permission.READ_SMS)) {
            btnReadSMS.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_SMS},
                    REQ_CODE_PERMISSION_READ_SMS);
        }

        //permission check on RECEIVE SMS
        if (!checkPermission(Manifest.permission.RECEIVE_SMS)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    REQ_CODE_PERMISSION_RECEIVE_SMS);
        }

        btnReadSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver cr = getContentResolver();
                Cursor c = cr.query(Uri.parse("content://sms/inbox"),
                        null, null, null, null);

                //column names get
                StringBuffer info = new StringBuffer();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    info.append("COLUMN_NAME: " + c.getColumnName(i) + "\n");
                }
                Toast.makeText(MainActivity.this, info.toString(), Toast.LENGTH_LONG).show();


                int indexBody = c.getColumnIndex("body");
                int indexPhone = c.getColumnIndex("address");

                int indexDate = c.getColumnIndex("date");
                int indexDateSent = c.getColumnIndex("date_sent");

                if (indexBody < 0 || !c.moveToFirst()) return;

                arrayAdapter.clear();
                do {
                    String phone = c.getString(indexPhone);
                    String msg = c.getString(indexBody);
                    String str = "SMS from: " + phone + "\n" + msg;

                    String ddate = c.getString(indexDate);
                    String dateSent = c.getString(indexDateSent);

                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date thisDate = new Date(Long.parseLong(ddate));
                    ddate = df.format(thisDate);
                    thisDate = new Date(Long.parseLong(dateSent));
                    dateSent = df.format(thisDate);

                    str += "\nDate: " + ddate;
                    str += "\nDate Sent: " + dateSent;

                    arrayAdapter.add(str);


                    //send sms
                    sendSMS(phone, msg, ddate, dateSent);


                } while (c.moveToNext());
                c.close();
            }
        });


        btnSendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNum = etPhone.getText().toString();
                String message = etMsg.getText().toString();

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNum, null, message, null, null);


                Toast.makeText(MainActivity.this, "SMS sent to: " + phoneNum, Toast.LENGTH_LONG).show();
                //record sms sent by app
            }
        });

        arrayAdapter = new SMSAdapter();
        lvSMS.setAdapter(arrayAdapter);


    }

    public class SmsReceiver extends BroadcastReceiver {

        private MainActivity mainActivity;

        public SmsReceiver(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            String phoneNumber = "";
            String message = "";
            long dateSent = 0;
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];

                for (int i = 0; i < msgs.length; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    phoneNumber = msgs[i].getOriginatingAddress();
                    message += msgs[i].getMessageBody();
                    dateSent = msgs[i].getTimestampMillis();
                }
                // Handle the incoming SMS message here
                MainActivity.smsData.add("From: " + phoneNumber + "\nMessage: " + message + "\nDate Sent: " + dateSent);
                mainActivity.arrayAdapter.notifyDataSetChanged();
            }
        }

    }


    public void displayMsg() {
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(Uri.parse("content://sms/inbox"),
                null, null, null, null);

        //column names get
        StringBuffer info = new StringBuffer();
        for (int i = 0; i < c.getColumnCount(); i++) {
            info.append("COLUMN_NAME: " + c.getColumnName(i) + "\n");
        }
        Toast.makeText(MainActivity.this, info.toString(), Toast.LENGTH_LONG).show();


        int indexBody = c.getColumnIndex("body");
        int indexPhone = c.getColumnIndex("address");

        int indexDate = c.getColumnIndex("date");
        int indexDateSent = c.getColumnIndex("date_sent");

        if (indexBody < 0 || !c.moveToFirst()) return;

        arrayAdapter.clear();
        do {
            String phone = c.getString(indexPhone);
            String msg = c.getString(indexBody);
            String str = "SMS from: " + phone + "\n" + msg;

            String ddate = c.getString(indexDate);
            String dateSent = c.getString(indexDateSent);

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date thisDate = new Date(Long.parseLong(ddate));
            ddate = df.format(thisDate);
            thisDate = new Date(Long.parseLong(dateSent));
            dateSent = df.format(thisDate);

            str += "\nDate: " + ddate;
            str += "\nDate Sent: " + dateSent;

            arrayAdapter.add(str);


        } while (c.moveToNext());
        c.close();
    }


    private boolean checkPermission(String permission) {
        int permissionCode = ContextCompat.checkSelfPermission(this, permission);
        return permissionCode == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_PERMISSION_READ_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    btnReadSMS.setEnabled(true);
                    btnSendSMS.setEnabled(true);
                }
                break;
        }
    }


    public void sendSMS(String phone, String msg, String ddate, String dateSent) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = SERVER + "?phone=" + phone;
        url += "&msg=" + Uri.encode(msg);
        url += "&date=" + Uri.encode(ddate);
        url += "&date_sent=" + Uri.encode(dateSent);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    class SMSAdapter extends ArrayAdapter<String> {
        SMSAdapter() {
            super(MainActivity.this, R.layout.list_item, smsData);
        }
        private int selectedSmsPosition;

        SMSAdapter(int selectedSmsPosition) {
            super(MainActivity.this, R.layout.list_item, smsData);
            this.selectedSmsPosition = selectedSmsPosition;
        }
        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }
            TextView textView = convertView.findViewById(R.id.text_sms);
            textView.setText(getItem(position));
            String phoneNumber = getNumberFromSms(getItem(position));
            Button blockButton = convertView.findViewById(R.id.block_button);
            blockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addNumberToBlockList(phoneNumber);
                }
            });

            return convertView;
        }


        private void addNumberToBlockList(String phoneNumber) {
            // Remove the item from the smsData list
            smsData.remove(selectedSmsPosition);

            // Add the number to the blocked numbers list
            ContentValues values = new ContentValues();
            values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber);
            ContentResolver contentResolver = getContentResolver();
            contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);

            // Notify the adapter that the data set has changed
            arrayAdapter.notifyDataSetChanged();
        }

        @SuppressLint("Range")
        private String getNumberFromSms(String sms) {
            String phoneNumber = null;
            if (sms != null) {
                Pattern pattern = Pattern.compile("\\+?[0-9]{5,13}");
                Matcher matcher = pattern.matcher(sms);
                if (matcher.find()) {
                    phoneNumber = matcher.group();
                }
            }
            return phoneNumber;
        }


    }
}

