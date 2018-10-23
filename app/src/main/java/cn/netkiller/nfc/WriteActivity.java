package cn.netkiller.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.UUID;

import static android.nfc.NdefRecord.createMime;

public class WriteActivity  extends Activity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        TextView uuidTextView = (TextView) findViewById(R.id.uuid);

//        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
//
//        if (nfcAdapter == null) {
//            System.out.println("**** NFC ERROR ****");
//            uuid.setText("NFC is not available.");
//            return;
//        } else if (!nfcAdapter.isEnabled()) {
//            uuid.setText("请开启系统NFC功能");
//            return;
//        }

//        nfcAdapter.setNdefPushMessageCallback(this, this);

//        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        uuidTextView.setText("Start...");

        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

        try {
            UUID uuid = UUID.randomUUID();
            write(uuid.toString(),tag);
            uuidTextView.setText(uuid.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        nfcAdapter.enableForegroundDispatch(this,pendingIntent, null, null);
//
//    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        nfcAdapter.disableForegroundDispatch(this);
//    }


    //当窗口的创建模式是singleTop或singleTask时调用，用于取代onCreate方法
    //当NFC标签靠近手机，建立连接后调用
//    @Override
//    public void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//
//        TextView uuidTextView = (TextView) findViewById(R.id.uuid);
//
//        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//
//        UUID uuid = UUID.randomUUID();
//        try {
//            write(uuid.toString(),tag);
//
//            uuidTextView.setText(uuid.toString());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (FormatException e) {
//            e.printStackTrace();
//        }
//    }
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }

}
