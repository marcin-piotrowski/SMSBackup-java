package com.sample.smsbackup.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sample.smsbackup.activities.HomeActivity;
import com.sample.smsbackup.models.SMS;
import com.sample.smsbackup.utilities.SecretService;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SMSInboxServices {

    //Constants
    private static final String BACKUP_FILE_NAME = "backup";
    private static final Uri INBOX_URI = Telephony.Sms.CONTENT_URI;
    private static final String[] PROJECTION = new String[]
            {
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
            };

    //Fields
    private HomeActivity context;

    //Methods
    public void backup(GoogleSignInAccount googleSignInAccount) {
        downloadSMSFromCloudAndRun(googleSignInAccount, null,
                msgFromCloud -> {

                    List<SMS> smsInInbox = getAllSMSFromInbox();

                    if(msgFromCloud.size() > 0) {
                        Log.i(this.getClass().getSimpleName(), "Compering cloud content to device");
                        smsInInbox.removeAll(msgFromCloud);
                        Log.i(this.getClass().getSimpleName(), "Found " + smsInInbox.size() + " not stored yet messages");
                    }

                    if(smsInInbox.size() < 1){
                        Toast.makeText(
                                context.getApplicationContext(),
                                "No SMS to backup",
                                Toast.LENGTH_LONG)
                                .show();
                        return;
                    }

                    try {
                        msgFromCloud.addAll(smsInInbox);
                        String encryptedJSON = SecretService.encrypt(makeJSON(smsInInbox), googleSignInAccount);
                        uploadToCloud(encryptedJSON, googleSignInAccount);
                    } catch (Exception e) {
                        Toast.makeText(
                                context.getApplicationContext(),
                                "Error occurs while making a backup",
                                Toast.LENGTH_LONG)
                                .show();
                        Log.e(this.getClass().getName(), "Error occurs while making a backup. Exception message: " + e.getMessage());
                    }
                });
    }

    public void restore(GoogleSignInAccount googleSignInAccount) {
        downloadSMSFromCloudAndRun(googleSignInAccount, msgFromCloud -> {
            msgFromCloud.removeAll(getAllSMSFromInbox());
            storeSMS(msgFromCloud);
        }, null);
    }

    public void erase() {
        int rowDeleted = context.getContentResolver().delete(INBOX_URI, "_id > 0", null);
        context.askForDefaultApp(0);
        Toast.makeText(
                context.getApplicationContext(),
                rowDeleted + " messages deleted. Now choose your favorite SMS app",
                Toast.LENGTH_LONG)
                .show();
    }

    private String makeJSON(List<SMS> list) {
        return new Gson().toJson(list);
    }

    private ArrayList<SMS> getAllSMSFromInbox() {
        Cursor cursor = context.getContentResolver().query(INBOX_URI, PROJECTION, null, null, null);

        ArrayList<SMS> mailBox = new ArrayList<>(0);

        while (cursor.moveToNext()) {
            SMS sms = new SMS();
            sms.setAddress(cursor.getString(cursor.getColumnIndex(PROJECTION[0])));
            sms.setBody(cursor.getString(cursor.getColumnIndex(PROJECTION[1])));
            sms.setDate(cursor.getString(cursor.getColumnIndex(PROJECTION[2])));
            sms.setType(cursor.getString(cursor.getColumnIndex(PROJECTION[3])));

            mailBox.add(sms);
        }

        return mailBox;
    }

    private void storeSMS(List<SMS> list) {

        int count = 0;

        if(list.size() > 0) {
            for (SMS sms : list) {

                ContentValues smsRow = new ContentValues();
                smsRow.put(Telephony.Sms.ADDRESS, sms.getAddress());
                smsRow.put(Telephony.Sms.BODY, sms.getBody());
                smsRow.put(Telephony.Sms.DATE, sms.getDate());
                smsRow.put(Telephony.Sms.TYPE, sms.getType());

                context.getContentResolver().insert(INBOX_URI, smsRow);
                count++;
            }

            Toast.makeText(
                    context.getApplicationContext(),
                    count + " messages recover. No choose your favorite SMS app",
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(
                    context.getApplicationContext(),
                    "Nothing to recover",
                    Toast.LENGTH_LONG)
                    .show();
        }
        context.askForDefaultApp(0);
    }

    private void downloadSMSFromCloudAndRun(GoogleSignInAccount googleSignInAccount, Writer writer, Reader reader) {

        DriveResourceClient driveResourceClient =
                Drive.getDriveResourceClient(context.getApplicationContext(), googleSignInAccount);

        Task<DriveFolder> appFolderTask = driveResourceClient.getAppFolder();
        appFolderTask
                .continueWith(task -> {
                    DriveFolder appFolder = appFolderTask.getResult();
                    return appFolder;
                })
                .addOnSuccessListener(context, folder -> {
                    Task<MetadataBuffer> listTask = driveResourceClient.listChildren(folder);
                    listTask
                            .addOnSuccessListener(context, metadataBuffer -> {

                                DriveFile backupFile = null;
                                for (Metadata row : metadataBuffer) {
                                    if (row.getTitle().equals(BACKUP_FILE_NAME)) {
                                        backupFile = row.getDriveId().asDriveFile();
                                    }
                                }

                                Log.i(this.getClass().getSimpleName(), "Downloading file");
                                if (backupFile != null) {

                                    Log.i(this.getClass().getSimpleName(), "Backup file found");

                                    Task<DriveContents> openFileTask =
                                            driveResourceClient.openFile(backupFile, DriveFile.MODE_READ_ONLY);

                                    openFileTask
                                            .continueWithTask(task -> {
                                                DriveContents contents = task.getResult();
                                                InputStream stream = contents.getInputStream();
                                                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                                                String encryptedJSON = br.lines().collect(Collectors.joining("\n"));
                                                String decryptedJSON = SecretService.decrypt(encryptedJSON, googleSignInAccount);
                                                Log.i(this.getClass().getSimpleName(),"JSON from cloud -> "+decryptedJSON);
                                                List<SMS> SMSfromCluod = new Gson().fromJson(decryptedJSON, new TypeToken<List<SMS>>() {
                                                }.getType());

                                                if (reader == null) {
                                                    Log.i(this.getClass().getSimpleName(),
                                                            SMSfromCluod.size() + " sms downloaded. Going to compare and store on device.");
                                                    writer.store(SMSfromCluod);
                                                } else {
                                                    Log.i(this.getClass().getSimpleName(),
                                                            SMSfromCluod.size() + " sms downloaded. Going to compare and save in cloud.");
                                                    reader.save(SMSfromCluod);
                                                }

                                                Task<Void> discardTask = driveResourceClient.discardContents(contents);
                                                return discardTask;
                                            })
                                            .addOnFailureListener(e -> Log.e(this.getClass().getName(), e.getMessage()));

                                } else {

                                    Log.i(this.getClass().getSimpleName(), "Backup file not found. Nothing to compare.");

                                    if (reader == null) {
                                        writer.store(new ArrayList<>(0));
                                    } else {
                                        reader.save(new ArrayList<>(0));
                                    }

                                }
                            })
                            .addOnFailureListener(context, e -> Log.e(this.getClass().getName(), e.getMessage()));
                })
                .addOnFailureListener(context, e -> Log.e(this.getClass().getName(), e.getMessage()));
    }

    private void uploadToCloud(String json, GoogleSignInAccount googleSignInAccount) {

        DriveResourceClient driveResourceClient =
                Drive.getDriveResourceClient(context.getApplicationContext(), googleSignInAccount);

        Task<DriveFolder> appFolderTask = driveResourceClient.getAppFolder();
        appFolderTask
                .continueWith(task -> {
                    DriveFolder appFolder = appFolderTask.getResult();
                    return appFolder;
                })
                .addOnSuccessListener(context, folder -> {
                    Task<MetadataBuffer> listTask = driveResourceClient.listChildren(folder);
                    listTask
                            .addOnSuccessListener(context, metadataBuffer -> {

                                DriveFile backupFile = null;
                                for (Metadata row : metadataBuffer) {
                                    if (row.getTitle().equals(BACKUP_FILE_NAME)) {
                                        backupFile = row.getDriveId().asDriveFile();
                                    }
                                }

                                Log.i(this.getClass().getSimpleName(), "Uploading file");
                                if (backupFile != null) {

                                    Log.i(this.getClass().getSimpleName(), "Backup file found");

                                    Task<DriveContents> openFileTask =
                                            driveResourceClient.openFile(backupFile, DriveFile.MODE_READ_WRITE);

                                    openFileTask.
                                            continueWithTask(task -> {

                                                DriveContents driveContents = task.getResult();
                                                ParcelFileDescriptor pfd = driveContents.getParcelFileDescriptor();

                                                try (OutputStream out = new FileOutputStream(pfd.getFileDescriptor())) {
                                                    out.write(json.getBytes());
                                                }

                                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                        .setStarred(true)
                                                        .setLastViewedByMeDate(new Date())
                                                        .build();

                                                Task<Void> commitTask =
                                                        driveResourceClient.commitContents(driveContents, changeSet);

                                                return commitTask;
                                            })
                                            .addOnSuccessListener(context, aVoid -> {
                                                Toast.makeText(
                                                        context.getApplicationContext(),
                                                        "Backup success!",
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                                Log.i(this.getClass().getSimpleName(), "Backup file rewrite to " + json);
                                            })
                                            .addOnFailureListener(context, e -> {
                                                Toast.makeText(
                                                        context.getApplicationContext(),
                                                        "Error occurs during uploading! Operation aborted...",
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                                Log.e(this.getClass().getSimpleName(), "Upload to Drive failed! Exception message: " + e.getMessage());
                                            });
                                } else {
                                    Log.i(this.getClass().getSimpleName(), "Backup file not found");

                                    Task<DriveContents> createFileTask = driveResourceClient.createContents();

                                    createFileTask
                                            .continueWithTask(task -> {
                                                DriveContents contents = createFileTask.getResult();
                                                OutputStream outputStream = contents.getOutputStream();
                                                try (java.io.Writer writer = new OutputStreamWriter(outputStream)) {
                                                    writer.write(json);
                                                }

                                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                        .setTitle(BACKUP_FILE_NAME)
                                                        .setMimeType("application/json")
                                                        .setStarred(true)
                                                        .build();

                                                return driveResourceClient.createFile(folder, changeSet, contents);
                                            })
                                            .addOnSuccessListener(context, driveFile -> {
                                                Toast.makeText(
                                                        context.getApplicationContext(),
                                                        "Backup successes!",
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                                Log.i(this.getClass().getSimpleName(), "Backup file <" + BACKUP_FILE_NAME + "> created with content -> " + json);
                                            })
                                            .addOnFailureListener(context, e -> {
                                                Toast.makeText(
                                                        context.getApplicationContext(),
                                                        "Error occurs during uploading! Operation aborted...",
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                                Log.e(this.getClass().getSimpleName(), "Upload to Drive failed! Exception message: " + e.getMessage());
                                            });
                                }
                            });
                });
    }

    //Cnt
    public SMSInboxServices(HomeActivity context) {
        this.context = context;
    }

    private interface Writer {
        void store(List<SMS> list);
    }

    private interface Reader {
        void save(List<SMS> list);
    }
}
