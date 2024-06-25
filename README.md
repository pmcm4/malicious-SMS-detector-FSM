# M-SMS

## Overview
M-SMS is an Android application developed in Java using Android Studio. It integrates the SMSRETRIEVER API to receive and read SMS messages. The application extracts URL links from incoming SMS messages and classifies them using a dataset from Kaggle, categorizing URLs as benign, defacement, phishing, or malware links. Classification is performed using a Jaro-Winkler similarity distance checker algorithm with a threshold of 90%. The app employs a Finite State Machine (FSM) approach to handle SMS messages containing multiple URL links.

## Features
- **SMS Retrieval:** Utilizes SMSRETRIEVER API to receive and read SMS messages.
- **URL Extraction:** Extracts URL links from SMS messages.
- **Classification:** Classifies URLs based on a Kaggle dataset.
- **Jaro-Winkler Algorithm:** Uses Jaro-Winkler similarity for URL classification.
- **Finite State Machine:** Manages SMS messages with multiple URL links.

## Tech Stack
- **Language:** Java
- **Development Environment:** Android Studio
- **API:** SMSRETRIEVER API
- **Algorithm:** Jaro-Winkler similarity
- **Dataset:** Kaggle dataset
- **Deployment:** Deployed on Android devices
