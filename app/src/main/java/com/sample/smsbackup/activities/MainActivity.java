package com.sample.smsbackup.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
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

public class MainActivity extends AppCompatActivity {

    //Constants
    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private static final int SIGN_IN_REQUEST_CODE = 2;
    private static final int ERASE_REQUEST_CODE = 3;
    private static final int BACKUP_REQUEST_CODE = 4;

    //Fields
    SMSInboxServices smsService;
    GoogleSignInClient googleSignInClient;
    GoogleSignInAccount googleSignInAccount = null;

    //Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleSignInClient = buildGoogleSignInClient();
        smsService = new SMSInboxServices(this);
    }

    @Override
    protected  void onStart(){
        super.onStart();

        checkPermissions(Manifest.permission.READ_SMS);
        checkPermissions(Manifest.permission.SEND_SMS);
        checkPermissions(Manifest.permission.GET_ACCOUNTS);
        checkPermissions(Manifest.permission.INTERNET);

        googleSingIn();
    }

    //Requests Callbacks
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == PERMISSIONS_REQUEST_CODE) {
            if (this.checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                        getApplicationContext(),
                        R.string.stringPermissionGranted,
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        R.string.stringPermissionDenied,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case ERASE_REQUEST_CODE:
                runService(ERASE_REQUEST_CODE);
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
            case R.id.btnErase:
                runService(ERASE_REQUEST_CODE);
                break;
            case R.id.btnBackup:
                runService(BACKUP_REQUEST_CODE);
                break;
            default:
                break;
        }
    }

    public void runService(int requestCode) {
        switch (requestCode) {
            case ERASE_REQUEST_CODE:
                if(Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName())) {
                    smsService.erase();
                    askForDefaultApp(0);
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "To process you has to choose SMSBackup",
                            Toast.LENGTH_LONG)
                            .show();
                    askForDefaultApp(ERASE_REQUEST_CODE);
                }
                break;
            case BACKUP_REQUEST_CODE:
                smsService.backup(googleSignInAccount);
            default:
                break;
        }
    }

    private void askForDefaultApp(int requestCode) {
        String intentExtraKey = Telephony.Sms.getDefaultSmsPackage(this) + Telephony.Sms.Intents.EXTRA_PACKAGE_NAME;
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(intentExtraKey, getPackageName());
        startActivityForResult(intent, requestCode);
    }

    private void checkPermissions(String permission) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{permission}, 0);
        }
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
            Log.w(this.getClass().getSimpleName(), "signInResult:failed code=" + e.getStatusCode());
        }
    }
}
