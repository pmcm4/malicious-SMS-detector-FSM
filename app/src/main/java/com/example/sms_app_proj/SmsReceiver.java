package com.example.sms_app_proj;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.os.Build;
import android.provider.BlockedNumberContract;
import android.provider.BlockedNumberContract.BlockedNumbers;

import android.util.Log;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private Map<String, String> dataset;
    private List<SMS> smsList = new ArrayList<>();
    private FSM fsm;

    // Define the initial and final states for the FSM
    enum State {
        BENIGN, DEFACEMENT, PHISHING, MALWARE, DP, DM, PM, DPM
    }

    private State currentState = State.BENIGN;

    public class FSM {
        public State process(String msg) {
            List<String> urls = new ArrayList<>();

            String[] words = msg.split("[\\s\\n]+");

            for (String word : words) {
                if (word.contains("/") || (word.contains(".") && word.lastIndexOf(".") > 0)) {
                    urls.add(word);
                }
            }

            for (String url : urls) {
                double threshold = 0.9;
                System.out.println(urls);
                for (Map.Entry<String, String> entry : dataset.entrySet()){
                    String datasetUrl = entry.getKey();
                    double distance = similarityCheck(url, datasetUrl);

                    if (distance >= threshold) {
                        State nextState = State.valueOf(entry.getValue().toUpperCase());
                        System.out.println("url: " + url);
                        System.out.println("dataset url: "+datasetUrl);
                        System.out.println("distance: "+distance);
                        System.out.println("threshold: "+threshold);
                        System.out.println("Current State: "+currentState);
                        System.out.println("Next State: "+nextState);
                        switch (currentState) {
                            case BENIGN:
                                if (nextState == State.PHISHING) {
                                    currentState = State.PHISHING;
                                } else if (nextState == State.MALWARE) {
                                    currentState = State.MALWARE;
                                } else if (nextState == State.DEFACEMENT) {
                                    currentState = State.DEFACEMENT;
                                } else {
                                    currentState = State.BENIGN;
                                }
                                break;
                            case DEFACEMENT:
                                if (nextState == State.PHISHING) {
                                    currentState = State.DP;
                                } else if (nextState == State.MALWARE) {
                                    currentState = State.DM;
                                } else {
                                    currentState = State.DEFACEMENT;
                                }
                                break;
                            case PHISHING:
                                if (nextState == State.DEFACEMENT) {
                                    currentState = State.DP;
                                } else if (nextState == State.MALWARE) {
                                    currentState = State.PM;
                                } else {
                                    currentState = State.PHISHING;
                                }
                                break;
                            case MALWARE:
                                if (nextState == State.DEFACEMENT) {
                                    currentState = State.DM;
                                } else if (nextState == State.PHISHING) {
                                    currentState = State.PM;
                                } else {
                                    currentState = State.MALWARE;
                                }
                                break;
                            case DP:
                                if (nextState == State.MALWARE) {
                                    currentState = State.DPM;
                                } else {
                                    currentState = State.DP;
                                }break;
                            case PM:
                                if (nextState == State.DEFACEMENT) {
                                    currentState = State.DPM;
                                } else {
                                    currentState = State.PM;
                                }
                                break;
                            case DM:
                                if (nextState == State.PHISHING) {
                                    currentState = State.DPM;
                                } else {
                                    currentState = State.DM;
                                }
                                break;
                            case DPM:
                                currentState = State.DPM;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            return currentState;
        }
    }

    private double similarityCheck(String a, String b) {
        int[] mtp = matches(a, b);
        float m = mtp[0];
        if (m == 0) {
            return 0f;
        }
        float j = ((m / a.length() + m / b.length() + (m - mtp[1]) / m)) / 3;
        float jw = j < 0.7f ? j : j + Math.min(0.1f, 1f / mtp[3]) * mtp[2] * (1 - j);
        return jw;
    }

    private int[] matches(String a, String b) {
        String max, min;
        if (a.length() > b.length()) {
            max = a;
            min = b;
        } else {
            max = b;
            min = a;
        }
        int range = Math.max(max.length() / 2 - 1, 0);
        int[] matchIndexes = new int[min.length()];
        Arrays.fill(matchIndexes, -1);
        boolean[] matchFlags = new boolean[max.length()];
        int matches = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            char c1 = min.charAt(mi);
            for (int xi = Math.max(mi - range, 0), xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                    matchIndexes[mi] = xi;
                    matchFlags[xi] = true;
                    matches++;
                    break;
                }
            }
        }
        char[] ms1 = new char[matches];
        char[] ms2 = new char[matches];
        for (int i = 0, si = 0; i < min.length(); i++) {
            if (matchIndexes[i] != -1) {
                ms1[si] = min.charAt(i);
                si++;
            }
        }
        for (int i = 0, si = 0; i < max.length(); i++) {
            if (matchFlags[i]) {
                ms2[si] = max.charAt(i);
                si++;
            }
        }
        int transpositions = 0;
        for (int mi = 0; mi < ms1.length; mi++) {
            if (ms1[mi] != ms2[mi]) {
                transpositions++;
            }
        }
        int prefix = 0;
        for (int mi = 0; mi < min.length(); mi++) {
            if (a.charAt(mi) == b.charAt(mi)) {
                prefix++;
            } else {
                break;
            }
        }
        return new int[]{matches, transpositions / 2, prefix, max.length()};
    }

    private String[] extractFeatures(String msg) {
        List<String> features = new ArrayList<>();
        String[] words = msg.split(" ");
        for (String word : words) {
            if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("defacement")) {
                features.add(word.toLowerCase());
            } else if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("phishing")) {
                features.add(word.toLowerCase());
            } else if (dataset.containsKey(word.toLowerCase()) && dataset.get(word.toLowerCase()).equals("malware")) {
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
        public SMS(String message, String label) {
            this.message = message;
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

    /* private void trainFSM(String datasetPath, Context context) throws IOException {

        // Read the dataset from the CSV file
        List<SMS> dataset = readDatasetFromCSV(context, datasetPath);

        // Initialize the transitions map and set the initial state to "benign"
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
    }*/


    /* private boolean isDefacementFeature(String feature) {
        return dataset.get(feature).equals("defacement");
    }
    private boolean isPhishingFeature(String feature) {
        return dataset.get(feature).equals("phishing");
    }
    private boolean isMalwareFeature(String feature) {
        return dataset.get(feature).equals("malware");
    }*/



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
                    /*for (SMS smsObject : smsList) {
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
                    }*/
                    // Classify the SMS message as spam or not
                    State finalState = fsm.process(msg);
                    if (finalState == State.DEFACEMENT) {
                        Toast.makeText(context, "From: " + phone + " CONTAINS DEFACEMENT LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.PHISHING){
                        Toast.makeText(context, "From: " + phone + " CONTAINS PHISHING LINKS", Toast.LENGTH_SHORT).show();
                    }else if(finalState == State.MALWARE){
                        Toast.makeText(context, "From: " + phone + " CONTAINS MALWARE LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.DP){
                        Toast.makeText(context, "From: " + phone + " CONTAINS DEFACEMENT & PHISHING LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.DM){
                        Toast.makeText(context, "From: " + phone + " CONTAINS DEFACEMENT & MALWARE LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.PM){
                        Toast.makeText(context, "From: " + phone + " CONTAINS PHISHING & MALWARE LINKS", Toast.LENGTH_SHORT).show();
                    }else if(finalState == State.DPM){
                        Toast.makeText(context, "From: " + phone + " CONTAINS DEFACEMENT, PHISHING, AND MALWARE LINKS", Toast.LENGTH_SHORT).show();
                    } else if(finalState == State.BENIGN){
                        Toast.makeText(context, "From: " + phone + " BENIGN", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("Error", "Failed to read SMS!");
        }
    }

}