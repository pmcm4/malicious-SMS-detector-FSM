/*package com.example.sms_app_proj;

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
        BENIGN, PHISHING, MALICIOUS
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
            Map<String, Double> tfIdf = calculateTfIdf(msg);
            double malicious = 0;
            double phishing = 0;
            for (Map.Entry<String, Double> entry : tfIdf.entrySet()) {
                if (dataset.containsKey(entry.getKey()) && dataset.get(entry.getKey()).equals("defacement")) {
                    malicious += entry.getValue();
                }
                else if(dataset.containsKey(entry.getKey()) && dataset.get(entry.getKey()).equals("phishing")){
                    malicious += entry.getValue();
                }
            }
            if (malicious > 0.5) {
                currentState = State.MALICIOUS;
            }
            else if(phishing > 0.5){
                currentState = State.PHISHING;
            }
            else {
                currentState = State.BENIGN;
            }
            return currentState;
        }
    }

    private Map<String, Double> calculateTfIdf(String msg) {
        Map<String, Double> tfIdf = new HashMap<>();
        String[] words = msg.split(" ");
        double wordCount = words.length;
        // Calculate the term frequency (TF) of each word
        Map<String, Double> tf = new HashMap<>();
        for (String word : words) {
            if (tf.containsKey(word.toLowerCase())) {
                tf.put(word.toLowerCase(), tf.get(word.toLowerCase()) + 1);
            } else {
                tf.put(word.toLowerCase(), 1.0);
            }
        }
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            entry.setValue(entry.getValue() / wordCount);
        }
        // Calculate the inverse document frequency (IDF) of each word
        Map<String, Double> idf = new HashMap<>();
        for (String word : words) {
            if (idf.containsKey(word.toLowerCase())) {
                continue;
            }
            double docCount = 0;
            for (SMS sms : smsList) {
                if (sms.getMessage().toLowerCase().contains(word.toLowerCase())) {
                    docCount++;
                }
            }
            idf.put(word.toLowerCase(), Math.log(smsList.size() / docCount));
        }
        // Calculate the TF-IDF of each word
        for (String word : words) {
            tfIdf.put(word.toLowerCase(), tf.get(word.toLowerCase()) * idf.get(word.toLowerCase()));
        }
        return tfIdf;
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
        private boolean isMali;
        private boolean isPhish;
        public SMS(String message, String label) {
            this.message = message;
            this.isMali = label.equals("defacement");
            this.isPhish = label.equals("phishing");
        }
        public boolean isMali() {
            return isMali;
        }

        public boolean isPhish() {
            return isPhish;
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
        smsList = readDatasetFromCSV(context, datasetPath);

        // Initialize the transitions map and set the initial state to "not spam"
        transitions = new HashMap<>();

        // Iterate through the dataset and train the FSM
        for (SMS sms : smsList) {
            String msg = sms.getMessage();
            boolean isMali = sms.isMali();
            boolean isPhish = sms.isPhish();

            // Calculate the TF-IDF of each word in the SMS message
            Map<String, Double> tfIdf = calculateTfIdf(msg);

            // For each word, check if it is a spam word and if so, add it to the transitions map
            for (Map.Entry<String, Double> entry : tfIdf.entrySet()) {
                if (isMali && dataset.get(entry.getKey()).equals("defacement")) {
                    transitions.put(entry.getKey(), State.MALICIOUS);
                }
                else if (isPhish && dataset.get(entry.getKey()).equals("phishing")) {
                    transitions.put(entry.getKey(), State.PHISHING);
                }
            }
        }
    }


    private boolean isMaliFeature(String feature) {
        return dataset.get(feature).equals("defacement");
    }
    private boolean isPhishFeature(String feature) {
        return dataset.get(feature).equals("phishing");
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
                    smsList = readDatasetFromCSV(context, "malicious_phish.csv");

                    FSM fsm = new FSM();
                    for (SMS smsObject : smsList) {
                        String[] features = calculateTfIdf(smsObject.getMessage()).keySet().toArray(new String[0]);
                        for (String feature : features) {
                            if (isMaliFeature(feature)) {
                                fsm.addTransition(feature, State.MALICIOUS);
                            }
                            else if (isPhishFeature(feature)) {
                                fsm.addTransition(feature, State.PHISHING);
                            }
                        }
                    }

                    // Classify the SMS message as spam or not
                    State finalState = fsm.process(msg);
                    if (finalState == State.MALICIOUS) {
                        Toast.makeText(context, "From: " + phone + "CONTAINS MALICIOUS LINKS", Toast.LENGTH_SHORT).show();
                    } else if (finalState == State.MALICIOUS){
                        Toast.makeText(context, "From: " + phone + " CONTAINS PHISHING LINKS", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(context, "From: " + phone + " is BENIGN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }catch (Exception e){
            Log.e("Error", "Failed to read SMS!");
        }
    }
}
*/


/*
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

    private HashMap<String, State> transitions;
    private State currentState;
    private Map<String, String> dataset;
    private List<SMS> smsList = new ArrayList<>();
    private FSM fsm;

    // Define the initial and final states for the FSM
    enum State {
        BENIGN, PHISHING, MALICIOUS
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
                    // If the final state is "phishing" or "malicious", return it
                    if (currentState == State.PHISHING || currentState == State.MALICIOUS) {
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
            if (dataset.containsKey(word.toLowerCase())) {
                String state = dataset.get(word.toLowerCase());
                if (state.equals("phishing") || state.equals("defacement")) {
                    features.add(word.toLowerCase());
                }
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
                // Skip the first line (header)
                if (line.startsWith("type,")) {
                    continue;
                }
                // Use comma as separator
                String[] sms = line.split(",", 2);
                SMS smsObject = new SMS(sms[0], sms[1]);
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
        private String url;
        private String label;
        private boolean isMali;
        private boolean isPhish;
        public SMS(String url, String label) {
            this.url = url;
            this.isMali = label.equals("defacement");
            this.isPhish = label.equals("phishing");
        }
        public boolean isMali() {
            return isMali;
        }

        public boolean isPhish() {
            return isPhish;
        }

        public String getMessage() {
            return url;
        }

        public void setMessage(String url) {
            this.url = url;
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
            boolean isMali = sms.isMali();
            boolean isPhish = sms.isPhish();

            // Extract the features from the SMS message (e.g. keywords, phrases, etc.)
            List<String> features = Arrays.asList(extractFeatures(msg));

            // For each feature, check if it triggers a transition to the "spam" state
            for (String feature : features) {
                if (isMaliFeature(feature)) {
                    State nextState = State.MALICIOUS;
                    transitions.put(feature, nextState);
                }
                if (isPhishFeature(feature)) {
                    State nextState = State.PHISHING;
                    transitions.put(feature, nextState);
                }
            }
        }
    }

    private boolean isMaliFeature(String feature) {
        return dataset.get(feature).equals("defacement");
    }
    private boolean isPhishFeature(String feature) {
        return dataset.get(feature).equals("phishing");
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
                    smsList = readDatasetFromCSV(context, "malicious_phish.csv");
                    State state = fsm.process(msg);
                    String result = "";
                    if (state == State.BENIGN) {
                        result = "The SMS message is benign.";
                    } else if (state == State.PHISHING) {
                        result = "The SMS message is phishing.";
                    } else if (state == State.MALICIOUS) {
                        result = "The SMS message is malicious.";
                    }
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show();
                }
            }
        }catch (Exception e){
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }
}
*/