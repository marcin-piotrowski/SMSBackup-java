package com.sample.smsbackup.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.Task;
import com.sample.smsbackup.R;
import com.sample.smsbackup.services.SMSInboxServices;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    //Constants
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final int SIGN_IN_REQUEST_CODE = 2;
    private static final int ERASE_REQUEST_CODE = 3;
    private static final int BACKUP_REQUEST_CODE = 4;
    private static final int RESTORE_REQUEST_CODE = 5;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.INTERNET
    };

    //Fields
    SMSInboxServices smsService;
    GoogleSignInClient googleSignInClient;
    GoogleSignInAccount googleSignInAccount = null;
    View mainLayout;

    //Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mainLayout = findViewById(R.id.mainLayout);
        googleSignInClient = buildGoogleSignInClient();
        smsService = new SMSInboxServices(this);
    }

    @Override
    protected  void onStart(){
        super.onStart();

        if (!checkPermissions()) askPermissions();
        googleSingIn();
    }

    //Requests Callbacks
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == PERMISSIONS_REQUEST_CODE && !checkPermissions()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Toast.makeText(this, R.string.permissionsDenied, Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case ERASE_REQUEST_CODE:
                runService(ERASE_REQUEST_CODE);
                break;
            case RESTORE_REQUEST_CODE:
                runService(RESTORE_REQUEST_CODE);
                break;
            case SIGN_IN_REQUEST_CODE:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;
            default:
                break;
        }
    }

    //Methods
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnBackup:
                runService(BACKUP_REQUEST_CODE);
                break;
            case R.id.btnRestore:
                runService(RESTORE_REQUEST_CODE);
                break;
            case R.id.btnErase:
                Snackbar
                        .make(mainLayout, R.string.areYouSure, Snackbar.LENGTH_LONG)
                        .setAction(R.string.iDo, v -> runService(ERASE_REQUEST_CODE))
                        .show();
                break;
            default:
                break;
        }
    }

    public void runService(int requestCode) {
        switch (requestCode) {
            case BACKUP_REQUEST_CODE: {
                smsService.backup(googleSignInAccount);
                break;
            }
            case RESTORE_REQUEST_CODE: {
                if(Telephony.Sms.getDefaultSmsPackage(this).equals(this.getPackageName()))
                    smsService.restore(googleSignInAccount);
                else
                    askForDefaultApp(RESTORE_REQUEST_CODE);
                break;
            }
            case ERASE_REQUEST_CODE:{
                if(Telephony.Sms.getDefaultSmsPackage(this).equals(this.getPackageName()))
                    smsService.erase();
                else
                    askForDefaultApp(ERASE_REQUEST_CODE);
                break;
            }
            default: {break;}
        }
    }

    public void askForDefaultApp(int requestCode) {
        String intentExtraKey = Telephony.Sms.getDefaultSmsPackage(this) + Telephony.Sms.Intents.EXTRA_PACKAGE_NAME;
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(intentExtraKey, getPackageName());
        startActivityForResult(intent, requestCode);
    }

    private boolean checkPermissions(){
        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    private void askPermissions() {

        ArrayList<String> missingPermission = new ArrayList<>(0);

        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                missingPermission.add(permission);
            }
        }

        if(missingPermission.size() > 0)
            requestPermissions(missingPermission.toArray(new String[missingPermission.size()]), 0);
    }

    private void googleSingIn(){

        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        if(googleSignInAccount == null){
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, SIGN_IN_REQUEST_CODE);
        }
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            googleSignInAccount = completedTask.getResult(ApiException.class);
        } catch (ApiException e) {
            Log.w(this.getClass().getSimpleName(), "signInResult:failed code=" + e.getMessage());
        }
    }
}
