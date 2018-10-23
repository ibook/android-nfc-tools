package cn.netkiller.nfc;


import android.nfc.FormatException;
import android.nfc.NdefRecord;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;

import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView status = (TextView) findViewById(R.id.status);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            System.out.println("**** NFC ERROR ****");
            status.setText("NFC is not available.");
            return;
        } else if (!nfcAdapter.isEnabled()) {
            status.setText("请开启系统NFC功能");
        }

        status.setText("Start...");

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


        Button button = (Button) findViewById(R.id.writeButton);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setContentView(R.layout.activity_write);
//                Intent intent = new Intent(MainActivity.this,WriteActivity.class);
//                startActivityForResult(intent,0);
            }
        });

        final Switch switchWrite = (Switch) findViewById(R.id.switchWrite);

        switchWrite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked) {
                    status.setText(switchWrite.getTextOn().toString());
                } else {
                    status.setText(switchWrite.getTextOff().toString());
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this,pendingIntent, null, null);
    }
    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }


    //当窗口的创建模式是singleTop或singleTask时调用，用于取代onCreate方法
    //当NFC标签靠近手机，建立连接后调用
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        TextView status = (TextView) findViewById(R.id.status);
        TextView type = (TextView) findViewById(R.id.type);
        TextView size = (TextView) findViewById(R.id.size);
        TextView uidTextView = (TextView) findViewById(R.id.uid);
        TextView ndefMessage = (TextView) findViewById(R.id.ndefMessage);
        TextView schema = (TextView) findViewById(R.id.schema);
        TextView charset = (TextView) findViewById(R.id.charset);
        TextView language = (TextView) findViewById(R.id.language);
        TextView ndefWrite = (TextView) findViewById(R.id.ndefWrite);
        Switch switchWrite = (Switch) findViewById(R.id.switchWrite);


        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(switchWrite.isChecked()){

            UUID uuid = UUID.randomUUID();
            try {
                write(uuid.toString(),tag);
                ndefWrite.setText(uuid.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }

        }

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            status.setText("Read NDEF Message...");

            byte[] uid = tag.getId();
            BigInteger n = new BigInteger(uid);
            String hex = n.toString(16);
            uidTextView.setText(hex);

            Ndef ndef = Ndef.get(tag);
            String log = ndef.getType() + "\n最大数据容量：" + ndef.getMaxSize() + " bytes\n\n";
            System.out.println(log);
            type.setText(ndef.getType());
            size.setText(ndef.getMaxSize() +" bytes");

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                byte[] payload = messages[0].getRecords()[0].getPayload();

                try {

                    String tagId = new String(messages[0].getRecords()[0].getType());
                    schema.setText(tagId);

                    String encoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                    charset.setText(encoding);

                    int languageCodeLength = payload[0] & 0x3f;
                    String languageCode = new String(payload, 1, languageCodeLength,"US-ASCII");


//                    String lang = new String(payload, 1, payload[0] & 0063, "US-ASCII");
                    language.setText(languageCode);

//                    String text = new String(messages[0].getRecords()[0].getPayload());
                    String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, encoding);
                    ndefMessage.setText(text);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }

        }else {
            status.setText("NOT NDEF Messages tag");
        }
    }


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
