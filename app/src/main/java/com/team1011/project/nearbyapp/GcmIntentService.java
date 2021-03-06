package com.team1011.project.nearbyapp;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.team1011.Database.Chat;
import com.team1011.Database.ChatDataSource;
import com.team1011.Database.Person;
import com.team1011.Database.PersonDataSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class waits for intents from GCM and processes them
 * Decides if a received message was is a control or a chat messages
 *
 * @author Alex Dellow, Thomas Tallentire
 * @author Filip Gutica, Melvin Loho (Implementation and Integration into project)
 */
public class GcmIntentService extends IntentService {



    public static ArrayList<Person> messages = new ArrayList<Person>();

    public PersonDataSource dataSource = new PersonDataSource(this);
    public ChatDataSource chatDataSource = new ChatDataSource(this);

    public Notifications notification;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    /**
     * Created and modified by Melvin and Filip
     * Processes received intents
     * Sends chats to the chat UI
     * sends matches to the notification UI
     * Adds chats and discovered people to the database
     * @param intent The received intent from GCM
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        dataSource.open();
        chatDataSource.open();
        messages = dataSource.getAllPeople();  //Notifications
        final Person person;


        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            String msg = extras.get("text").toString();
            try {
                JSONObject obj = new JSONObject(msg);

                //Add discovered person to notifications
                if (obj.get("TYPE").equals("control")) {

                    final String personUsrName;
                    final String personRegId;
                    personUsrName = obj.get("USER_NAME").toString();
                    personRegId = obj.get("REG_ID").toString();

                    if (!dataSource.exists(personUsrName)) {
                        notification = new Notifications(getApplicationContext());
                        Notifications.notify(getApplicationContext(), personUsrName, "New match", personUsrName, personRegId);
                        person = dataSource.createPerson(personUsrName, personRegId);

                        if (person != null)
                            messages.add(person);
                    }
                }
                else {
                    //Send the received chat to the chatFragment
                    final String chatUsrname;
                    final String chatmsg;
                    final String regid;

                    chatUsrname = obj.get("USER_NAME").toString();
                    chatmsg = obj.get("MESSAGE").toString();
                    regid = obj.get("REG_ID").toString();

                    Log.d("CHAT_RECEIVED", chatmsg);

                    Intent sendIntent = new Intent("chatBroadcastIntent");
                    Bundle chatBundle = new Bundle();
                    chatBundle.putString("USER_NAME", chatUsrname);
                    chatBundle.putString("TYPE", Chat.TYPE_FROM);
                    chatBundle.putString("MSG", chatmsg);

                    sendIntent.putExtras(chatBundle);

                    chatDataSource.createChat(chatUsrname, Chat.TYPE_FROM, chatmsg);

                    //Send intent to be received by the chatFragment
                    LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent);

                    if (!UI_Shell.runnnnnnnin) {
                        notification = new Notifications(getApplicationContext());
                        Notifications.notify(getApplicationContext(), chatUsrname + ": " + chatmsg, "New chat", chatUsrname, regid);
                    }

                    if (!dataSource.exists(chatUsrname)) {
                        person = dataSource.createPerson(chatUsrname, regid);
                        Notifications.notify(getApplicationContext(),chatUsrname, "New match", chatUsrname, regid);
                        if (person != null)
                            messages.add(person);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

}
