package edu.njit.cs656.nfcapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity  {

    private EditText edittext;
    private Uri[] mFileUris = new Uri[10];
    private NfcAdapter mNfcAdapter;
    private FileUriCallback mFileUriCallback;;

    private int PICK_IMAGE_REQUEST = 1;
    private String realPath = "";

    private EditText contactPath;
    private int PICK_CONTACT = 2;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PackageManager pm = this.getPackageManager();
        // Check whether NFC is available on device
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            // NFC is not available on the device.
            Toast.makeText(this, "The device does not has NFC hardware.",
                    Toast.LENGTH_SHORT).show();
        }
        // Check whether device is running Android 4.1 or higher
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // Android Beam feature is not supported.
            Toast.makeText(this, "Android Beam is not supported.",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            // NFC and Android Beam file transfer is supported.
            Toast.makeText(this, "Android Beam is supported on your device.",
                    Toast.LENGTH_SHORT).show();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edittext = findViewById(R.id.editText);
        edittext.setEnabled(false);

        contactPath = findViewById(R.id.contactPath);
        contactPath.setEnabled(false);
    }

    public void sendFile(View view) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(!mNfcAdapter.isEnabled()){
            // NFC is disabled, show the settings UI
            // to enable NFC
            Toast.makeText(this, "Please enable NFC.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
        // Check whether Android Beam feature is enabled on device
        else if(!mNfcAdapter.isNdefPushEnabled()) {
            // Android Beam is disabled, show the settings UI
            // to enable Android Beam
            Toast.makeText(this, "Please enable Android Beam.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        }
        else {
            mFileUriCallback = new FileUriCallback();
            mNfcAdapter.setBeamPushUrisCallback(mFileUriCallback, this);
        }
    }


    public void sendContact(View view) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(!mNfcAdapter.isEnabled()){
            // NFC is disabled, show the settings UI
            // to enable NFC
            Toast.makeText(this, "Please enable NFC.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
        // Check whether Android Beam feature is enabled on device
        else if(!mNfcAdapter.isNdefPushEnabled()) {
            // Android Beam is disabled, show the settings UI
            // to enable Android Beam
            Toast.makeText(this, "Please enable Android Beam.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        }
        else {
            // Register callback to set NDEF message
            mNfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
                    return new NdefMessage(new NdefRecord[] { getContactRecord() });
                }
            }, this);

            // Register callback to listen for message-sent success
            //mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }

    private NdefRecord getContactRecord()
    {
            byte[] uriField = realPath.getBytes(Charset.forName("US-ASCII"));
            byte[] payload = new byte[uriField.length + 1];  // Add 1 for the URI Prefix.
            System.arraycopy(uriField, 0, payload, 1, uriField.length);  // Append URI to payload.
            NdefRecord nfcRecord = new NdefRecord(
                    NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);
            Log.i("Main Activity", "Returning nfcRecord: "+nfcRecord.toString());
            return nfcRecord;

    }

    private class FileUriCallback implements
            NfcAdapter.CreateBeamUrisCallback {
        public FileUriCallback() {

            if(realPath != "") {
                File requestFile = new File(realPath);
                requestFile.setReadable(true, false);
                Uri fileUri = Uri.fromFile(requestFile);
                Log.i("FileUri: ", ""+fileUri);
                if (fileUri != null) {
                    mFileUris[0] = fileUri;
                    Log.i("Main Activity", "File URI available for transfer.");
                } else {
                    Log.e("Main Activity", "No File URI available for file.");
                }
            }
            else{
                Log.e("Main Activity", "No File selected for transfer.");
            }
            // Get a URI for the File and add it to the list of URIs
        }
        @Override
        public Uri[] createBeamUris(NfcEvent event) {
            return mFileUris;
        }
    }

    public void browsePhotos(View view){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            realPath = RealPathUtil.getRealPathFromURI(this, uri);
            Log.i("Main Activity", "Real Path: "+realPath);

            // Capture the layout's TextView and set the string as its text
            edittext.setText(realPath);

        }

        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();
            realPath = RealPathUtil.getRealPathFromContactURI(this, uri);
            Log.i("Main Activity", "Real Path: "+realPath);

            contactPath.setText(realPath);

        }

    }

    public void browseContacts(View view){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        Intent intent= new Intent(Intent.ACTION_PICK,  ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Contact"), PICK_CONTACT);
    }

}
