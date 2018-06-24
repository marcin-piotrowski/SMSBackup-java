package com.sample.smsbackup.services;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sample.smsbackup.R;
import com.sample.smsbackup.models.SMS;

import org.json.JSONObject;

import java.util.ArrayList;

public class SMSInboxServices {

    //Constants
    private static final Uri INBOX_URI = Telephony.Sms.CONTENT_URI;
    private static final String[] PROJECTION = new String[]
            {
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
            };

    //Fields
    private Activity context;

    //Methods
    public void backup(){
        String json = makeJSON();
        
    }

    public void erase() {
        int rowDeleted = context.getContentResolver().delete(INBOX_URI, "_id > 0", null);
        Toast.makeText(
                context.getApplicationContext(),
                rowDeleted + " messages deleted. Now choose your favorite SMS app",
                Toast.LENGTH_LONG)
                .show();
    }

    private String makeJSON(){
        Cursor cursor = context.getContentResolver().query(INBOX_URI, PROJECTION, null, null, null);

        ArrayList<SMS> mailBox = new ArrayList<>(0);
        while (cursor.moveToNext())
        {
            SMS sms = new SMS();
            sms.setAddress(cursor.getString(cursor.getColumnIndex(PROJECTION[0])));
            sms.setBody(cursor.getString(cursor.getColumnIndex(PROJECTION[1])));
            sms.setDate(cursor.getString(cursor.getColumnIndex(PROJECTION[2])));
            sms.setType(cursor.getString(cursor.getColumnIndex(PROJECTION[3])));

            mailBox.add(sms);
        }

        return new Gson().toJson(mailBox);
    }

    //Cnt
    public SMSInboxServices(Activity context) {
        this.context = context;
    }
}
