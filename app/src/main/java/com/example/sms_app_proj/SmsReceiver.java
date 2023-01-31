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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SmsReceiver extends BroadcastReceiver {


    private HashMap<String, State> transitions;
    private State currentState;
    private Map<String, String> dataset;
    private List<SMS> smsList = new ArrayList<>();
    private FSM fsm;

    // Define the initial and final states for the FSM
    enum State {
        BENIGN, DEFACEMENT, PHISHING, MALWARE
    }


    class FSM {
        private State currentState;
        private Map<String, State> transitions;

        public FSM() {

            // Set the initial state
            currentState = State.BENIGN;
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
                    if (currentState == State.DEFACEMENT) {
                        return currentState;
                    }else if (currentState == State.PHISHING){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }else if (currentState == State.MALWARE){
                        return currentState;
                    }
                }
            }
            if (msg.startsWith("http://") && msg.contains(".exe") || msg.startsWith("http://www.") && msg.contains(".exe") || msg.startsWith("www") && msg.contains(".exe")) {
                return State.MALWARE;
            }
            if (msg.startsWith("http://") && msg.contains("paypal") || msg.startsWith("http://www.") && msg.contains("paypal") || msg.startsWith("www") && msg.contains("paypal")) {
                return State.MALWARE;
            }

            // Check similarity with URLs in the dataset
            int threshold = (int) (0.9 * Math.min(msg.length(), 20));
            for (Map.Entry<String, String> entry : dataset.entrySet()) {
                String url = entry.getKey();
                int distance = levenshteinDistance(msg, url);
                if (distance <= threshold) {
                    currentState = State.valueOf(entry.getValue().toUpperCase());
                    return currentState;
                }
            }
            // If no transitions were triggered, return the current state
            return currentState;
        }


    }

    public int levenshteinDistance(String s, String t) {
        int m = s.length();
        int n = t.length();
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }
        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    d[i][j] = 1 + Math.min(d[i - 1][j], Math.min(d[i][j - 1], d[i - 1][j - 1]));
                }
            }
        }
        return d[m][n];
    }

    private String[] extractFeatures(String msg) {
        List<String> features = new ArrayList<>();
        String[] words = msg.split(" ");
        for (String word : words) {
            if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("defacement")) {
                features.add(word.toLowerCase());
            } else if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("phishing")) {
                features.add(word.toLowerCase());
            }else if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("malware")) {
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
            // Set the batch size
            int batchSize = 100;
            // Initialize the batch
            List<SMS> batch = new ArrayList<>();
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                // Use new line as separator
                String[] sms = line.split(",", 2);
                SMS smsObject = new SMS(sms[1], sms[0]);
                batch.add(smsObject);
                // add message to dataset
                String[] words = sms[1].split(" ");
                for (String word : words) {
                    if (!dataset.containsKey(word.toLowerCase())) {
                        dataset.put(word.toLowerCase(), sms[0]);
                    }
                }
                // If the batch size is reached, process the batch
                if (lineCount % batchSize == 0) {
                    smsList.addAll(batch);
                    batch.clear();
                }
            }
            // Add any remaining data in the batch to the smsList
            if (batch.size() > 0) {
                smsList.addAll(batch);
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
        private boolean isDefacement;
        private boolean isPhishing;
        private boolean isMalware;
        public SMS(String message, String label) {
            this.message = message;
            this.isDefacement = label.equals("defacement");
            this.isPhishing = label.equals("phishing");
            this.isMalware = label.equals("malware");
        }
        public boolean isDefacement() {
            return isDefacement;
        }
        public boolean isPhishing() {
            return isPhishing;
        }
        public boolean isMalware() {
            return isMalware;
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
        currentState = State.BENIGN;

        // Iterate through the dataset and train the FSM
        for (SMS sms : dataset) {
            String msg = sms.getMessage();
            boolean isDefacement = sms.isDefacement();
            boolean isPhishing = sms.isPhishing();
            boolean isMalware = sms.isMalware();

            // Extract the features from the SMS message (e.g. keywords, phrases, etc.)
            List<String> features = Arrays.asList(extractFeatures(msg));

            // For each feature, check if it triggers a transition to the "spam" state
            for (String feature : features) {
                if (isDefacementFeature(feature)) {
                    State nextState = State.DEFACEMENT;
                    transitions.put(feature, nextState);
                } else if (isPhishingFeature(feature)) {
                    State nextState = State.PHISHING;
                    transitions.put(feature, nextState);
                }   else if (isMalwareFeature(feature)) {
                    State nextState = State.MALWARE;
                    transitions.put(feature, nextState);
                }
            }
        }
    }


    private boolean isDefacementFeature(String feature) {
        return dataset.get(feature).equals("defacement");
    }
    private boolean isPhishingFeature(String feature) {
        return dataset.get(feature).equals("phishing");
    }
    private boolean isMalwareFeature(String feature) {
        return dataset.get(feature).equals("malware");
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
                            if (isDefacementFeature(feature)) {
                                fsm.addTransition(feature, State.DEFACEMENT);
                            } else if (isPhishingFeature(feature)) {
                                fsm.addTransition(feature, State.PHISHING);
                            } else if (isMalwareFeature(feature)) {
                                fsm.addTransition(feature, State.MALWARE);
                            }
                        }
                    }

                    // Classify the SMS message as spam or not
                    State finalState = fsm.process(msg);
                    if (finalState == State.DEFACEMENT) {
                        Toast.makeText(context, "From: " + phone + " CONTAINS DEFACEMENT LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.PHISHING){
                        Toast.makeText(context, "From: " + phone + " CONTAINS PHISHING LINKS", Toast.LENGTH_SHORT).show();
                    }else if(finalState == State.MALWARE){
                        Toast.makeText(context, "From: " + phone + " CONTAINS MALWARE LINKS", Toast.LENGTH_SHORT).show();
                    } else{
                        Toast.makeText(context, "From: " + phone + " IS BENIGN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("Error", "Failed to read SMS!");
        }
    }
}