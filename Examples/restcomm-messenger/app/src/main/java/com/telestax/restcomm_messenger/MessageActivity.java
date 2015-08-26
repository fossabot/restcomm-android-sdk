package com.telestax.restcomm_messenger;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.app.Activity;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.mobicents.restcomm.android.client.sdk.RCClient;
import org.mobicents.restcomm.android.client.sdk.RCDevice;

import java.util.HashMap;


public class MessageActivity extends Activity implements View.OnClickListener, OnAudioFocusChangeListener {

    private RCDevice device;
    HashMap<String, Object> params = new HashMap<String, Object>();
    private static final String TAG = "MessageActivity";

    Button btnSend;
    EditText txtMessage;
    TextView txtWall;
    MediaPlayer messagePlayer;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        btnSend = (Button)findViewById(R.id.button_send);
        btnSend.setOnClickListener(this);
        txtWall = (TextView) findViewById(R.id.text_wall);
        txtWall.setOnClickListener(this);
        txtWall.setMovementMethod(new ScrollingMovementMethod());
        txtMessage = (EditText)findViewById(R.id.text_message);
        txtMessage.setOnClickListener(this);

        // volume control should be by default 'music' which will control the ringing sounds and 'voice call' when within a call
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        messagePlayer = MediaPlayer.create(getApplicationContext(), R.raw.message);
        messagePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    // We 've set MessageActivity to be 'singleTop' on the manifest to be able to receive messages while already open, without instantiating
    // a new activity. When that happens we receive onNewIntent()
    // An activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method
    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        // Note that getIntent() still returns the original Intent. You can use setIntent(Intent) to update it to this new Intent.
        setIntent(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // retrieve the device
        device = RCClient.getInstance().listDevices().get(0);

        // Get Intent parameters.
        final Intent finalIntent = getIntent();
        if (finalIntent.getAction().equals(RCDevice.OPEN_MESSAGE_SCREEN)) {
            params.put("username", finalIntent.getStringExtra(RCDevice.EXTRA_DID));
        }
        if (finalIntent.getAction().equals(RCDevice.INCOMING_MESSAGE)) {
            String message = finalIntent.getStringExtra(RCDevice.INCOMING_MESSAGE_TEXT);
            HashMap<String, String> intentParams = (HashMap<String, String>) finalIntent.getSerializableExtra(RCDevice.INCOMING_MESSAGE_PARAMS);
            String username = intentParams.get("username");
            String shortname = username.replaceAll("^sip:", "").replaceAll("@.*$", "");
            params.put("username", username);

            txtWall.append(shortname + ": " + message + "\n\n");

            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                messagePlayer.start();
            }
        }
    }

    // UI Events
    public void onClick(View view) {
        if (view.getId() == R.id.button_send) {
            HashMap<String, String> sendParams = new HashMap<String, String>();
            sendParams.put("username", (String)params.get("username"));
            if (device.sendMessage(txtMessage.getText().toString(), sendParams)) {
                // also output the message in the wall
                txtWall.append("Me: " + txtMessage.getText().toString() + "\n\n");

                int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    messagePlayer.start();
                }
                txtMessage.setText("");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    public void handleIncomingMessage(RCDevice device, String message, HashMap<String, String> parameters)
    {
        Log.i(TAG, "Message arrived: " + message);

        // put new text on the bottom
        txtWall.append(parameters.get("username") + ": " + message + "\n");
    }
    */

    // Callbacks for audio focus change events
    public void onAudioFocusChange(int focusChange)
    {
        Log.i(TAG, "onAudioFocusChange: " + focusChange);
		/*
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
			// Pause playback
		}
		else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// Resume playback or raise it back to normal if we were ducked
		}
		else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
			//am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
			audio.abandonAudioFocus(this);
			// Stop playback
		}
		else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume
        }
		*/
    }
}
