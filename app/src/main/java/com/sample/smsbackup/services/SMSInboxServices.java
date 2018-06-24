package com.sample.smsbackup.services;

import android.app.Activity;
import android.net.Uri;
import android.provider.Telephony;
import android.widget.Toast;

import com.sample.smsbackup.R;

public class SMSInboxServices {

    //Constants
    private static final Uri INBOX_URI = Telephony.Sms.CONTENT_URI;

    //Fields
    private Activity context;

    //Methods
    public void erase() {
        int rowDeleted = context.getContentResolver().delete(INBOX_URI, "_id > 0", null);
        Toast.makeText(
                context.getApplicationContext(),
                rowDeleted + " messages deleted. Now choose your favorite SMS app",
                Toast.LENGTH_LONG)
                .show();
    }

    //Cnt
    public SMSInboxServices(Activity context) {
        this.context = context;
    }
}
