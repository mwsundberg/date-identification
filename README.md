# date-identification
Android app to identify dates on flyers, posters, from a real time camera preview. Project for DandyHacks 2018

## Team Members
Claire MacCormick, Joon Sung Park, Matthew Sundberg, and Nathan Reed

## Info

This is an android application that is designed to recognize dates on flyers/posters/etc for events using a live camera preview. It allows the user to add these events to their calendar apps quickly and easily. 
It uses Google's Android Vision API for the OCR and (currently) the natty library (http://natty.joestelmach.com) for parsing dates out of natural language.
We apply a series of refinements to recognized text in order to increase the likelihood of getting usable dates out of them.

## Miscellaneous
- This is heavily based on a google sample app from https://codelabs.developers.google.com/codelabs/mobile-vision-ocr/#0 (used for the basic UI and also the OCR setup).
