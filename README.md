# malicious-SMS-detector-FSM

SMS application that receives and reads SMS using SMSRETRIEVER API, extracts the url links contained in the SMS, and compare the url from the SMS to the dataset from kraggle to determine it's label(benign, defacement, phishing, or malware link). The program uses a similarity distance checker Jaro-Winkler algorithm that has a threshold of 90%.
