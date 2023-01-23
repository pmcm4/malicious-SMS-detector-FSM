package com.example.sms_app_proj;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {


    private final String SERVER = "http://atifnaseem22.000webhostapp.com/save_sms0.php";

    private HashMap<String, State> transitions;
    private State currentState;
    private Map<String, String> dataset;
    private List<SMS> smsList = new ArrayList<>();
    private FSM fsm;

    // Define the initial and final states for the FSM
    enum State {
        NOT_SPAM, SPAM
    }

    class FSM {
        private State currentState;
        private Map<String, State> transitions;

        public FSM() {

            // Set the initial state
            currentState = State.NOT_SPAM;
            // Initialize the transitions map
            transitions = new HashMap<>();
        }

        public void addTransition(String feature, State nextState) {

            transitions.put(feature, nextState);
        }

        public State process(String msg) {

            // Extract features from the SMS message
            String[] features = extractFeatures(msg);
            // Iterate through each feature and check if it triggers a transition
            for (String feature : features) {
                if (transitions.containsKey(feature)) {
                    currentState = transitions.get(feature);
                    // If the final state is "spam", return it
                    if (currentState == State.SPAM) {
                        return currentState;
                    }
                }
            }
            // If no transitions were triggered, return the current state
            return currentState;

        }
    }

    private String[] extractFeatures(String msg) {
        List<String> features = new ArrayList<>();
        String[] words = msg.split(" ");
        for (String word : words) {
            if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("spam")) {
                features.add(word.toLowerCase());
            }
        }
        return features.toArray(new String[features.size()]);
    }

    private List<SMS> readDatasetFromCSV(Context context, String datasetPath) throws IOException {
        List<SMS> smsList = new ArrayList<>();
        dataset = new HashMap<>();
        // Read the CSV file
        InputStream is = context.getAssets().open(datasetPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                // Use new line as separator
                String[] sms = line.split(",", 2);
                SMS smsObject = new SMS(sms[1], sms[0]);
                smsList.add(smsObject);
                // add message to dataset
                String[] words = sms[1].split(" ");
                for (String word : words) {
                    if (!dataset.containsKey(word.toLowerCase())) {
                        dataset.put(word.toLowerCase(), sms[0]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return smsList;
    }



    public class SMS {
        private String message;
        private String label;
        private boolean isSpam;
        public SMS(String message, String label) {
            this.message = message;
            this.isSpam = label.equals("spam");
        }
        public boolean isSpam() {
            return isSpam;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    private void trainFSM(String datasetPath, Context context) throws IOException {

        // Read the dataset from the CSV file
        List<SMS> dataset = readDatasetFromCSV(context, datasetPath);

        // Initialize the transitions map and set the initial state to "not spam"
        transitions = new HashMap<>();
        currentState = State.NOT_SPAM;

        // Iterate through the dataset and train the FSM
        for (SMS sms : dataset) {
            String msg = sms.getMessage();
            boolean isSpam = sms.isSpam();

            // Extract the features from the SMS message (e.g. keywords, phrases, etc.)
            List<String> features = Arrays.asList(extractFeatures(msg));

            // For each feature, check if it triggers a transition to the "spam" state
            for (String feature : features) {
                if (isSpamFeature(feature)) {
                    State nextState = State.SPAM;
                    transitions.put(feature, nextState);
                }
            }
        }
    }

    private boolean isSpamFeature(String feature) {
        return dataset.get(feature).equals("spam");
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            Bundle bundle = intent.getExtras();
            if(bundle != null){
                Object[] pdus = (Object[]) bundle.get("pdus");
                for(int i=0; i<pdus.length; i++){
                    SmsMessage sms = android.telephony.SmsMessage.createFromPdu((byte[]) pdus[i]);

                    String phone = sms.getDisplayOriginatingAddress();
                    String msg = sms.getDisplayMessageBody();

                    // Train the FSM on the dataset
                    smsList = readDatasetFromCSV(context, "spam.csv");

                    FSM fsm = new FSM();
                    for (SMS smsObject : smsList) {
                        String[] features = extractFeatures(smsObject.getMessage());
                        for (String feature : features) {
                            if (isSpamFeature(feature)) {
                                fsm.addTransition(feature, State.SPAM);
                            }
                        }
                    }

                    // Classify the SMS message as spam or not
                    State finalState = fsm.process(msg);
                    if (finalState == State.SPAM) {
                        Toast.makeText(context, "From: " + phone + " is SPAM", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "From: " + phone + " is NOT SPAM", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }catch (Exception e){
            Log.e("Error", "Failed to read SMS!");
        }
    }
}