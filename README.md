# date-identification
Android app to identify dates on flyers, posters, etc from photos. Project for DandyHacks 2018

## Team Members
Claire MacCormick, Joon Sung Park, Matthew Sundberg, and Nathan Reed

## Info

This is an android application that is designed to recognize dates on flyers/posters/etc for events and allow the user to add them to their calendar quickly and easily. 
It uses Google's Android Vision API for the OCR and (currently) the natty library (http://natty.joestelmach.com) for parsing dates out of natural language.
We apply a series of refinements to recognized text in order to increase the likelihood of getting usable dates out of them.

## Miscellaneous
- This is heavily based on a google sample app from https://codelabs.developers.google.com/codelabs/mobile-vision-ocr/#0 (used for the basic UI and also the OCR setup).
- Still definitely a work in progress, we have more rules to add to the text refinement system and are working on a better system for recognizing all parts of a date on a poster
- Ability to actually add event to calendar is also coming soon
