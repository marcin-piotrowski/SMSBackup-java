package com.sample.smsbackup.services;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.sample.smsbackup.models.SMS;
import com.sample.smsbackup.utilities.SecretService;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
    public void backup(GoogleSignInAccount googleSignInAccount){
        try {
            String json = makeJSON();
            String encrypted = SecretService.encrypt(json, googleSignInAccount);
            uploadToCloud(encrypted, googleSignInAccount);
        }catch (Exception e) {
            Toast.makeText(
                    context.getApplicationContext(),
                    "Error occurs during encrypting! Operation aborted...",
                    Toast.LENGTH_LONG)
                    .show();
        }
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

    private void uploadToCloud(final String json, GoogleSignInAccount googleSignInAccount){
        DriveResourceClient driveResourceClient =
                Drive.getDriveResourceClient(context.getApplicationContext(), googleSignInAccount);
        Task<DriveFolder> appFolderTask = driveResourceClient.getAppFolder();
        Task<DriveContents> createContentsTask = driveResourceClient.createContents();

        Tasks.whenAll(appFolderTask, createContentsTask)
                .continueWithTask(task -> {
                    DriveFolder parent = appFolderTask.getResult();
                    DriveContents contents = createContentsTask.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    try (Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write(json);
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(String.valueOf(System.currentTimeMillis()))
                            .setMimeType("application/json")
                            .setStarred(true)
                            .build();

                    return driveResourceClient.createFile(parent, changeSet, contents);
                })
                .addOnSuccessListener(context,
                        driveFile -> Toast.makeText(
                                context.getApplicationContext(),
                                "Backup successes!",
                                Toast.LENGTH_LONG)
                                .show())
                .addOnFailureListener(context,
                        e -> {Toast.makeText(
                        context.getApplicationContext(),
                        "Error occurs during uploading! Operation aborted...",
                        Toast.LENGTH_LONG)
                        .show();
                            Log.e(this.getClass().getSimpleName(), "Upload to Drive failed! Exception message: " + e.getMessage());
                        });
    }

    //Cnt
    public SMSInboxServices(Activity context) {
        this.context = context;
    }
}
