/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dandyhacks.datereader;

import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.dandyhacks.datereader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextBlock;
import com.joestelmach.natty.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
    private Parser parser = new Parser();

    private static final String TAG = "ProcessorDateParser";

    Queue<Date> oldDates = new PriorityQueue<>();

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private Activity context;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, Activity context) {
        mGraphicOverlay = ocrGraphicOverlay;
        this.context = context;
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        //LinkedList<Date> foundDateFragments = new LinkedList<>();
        //Fragment arrays
        ArrayList<Date> timeFragments = new ArrayList<>();
        ArrayList<Date> dateFragments = new ArrayList<>();
        ArrayList<Date> completeDates = new ArrayList<>();
        for (int i = 0; i < items.size(); ++i) {
            List<Line> lines = (List<Line>) items.valueAt(i).getComponents();
            for(Line item : lines) {
                if (item != null && item.getValue() != null){
                    Log.d(TAG, "Text Line detected! " + item.getValue());
                    String value = item.getValue();

                    // period to hyphen fix
                    value = value.replace('.','-');

                    //spaces around colons fix
                    for (int j = 1; j < value.length()-1; j ++){
                        if (value.charAt(j) == ':'){
                            int leftCount = 0;
                            for (int k = j-1; k >= 0; k --){
                                if (value.charAt(k) == ' '){
                                    leftCount ++;
                                } else {
                                    break;
                                }
                            }
                            int rightCount = 0;
                            for (int k = j + 1; k < value.length(); k++){
                                if (value.charAt(k) == ' '){
                                    rightCount ++;
                                } else {
                                    break;
                                }
                            }
                            String leftSide = value.substring(0,j-leftCount);
                            String rightSide = value.substring(j+rightCount+1, value.length());
                            value = leftSide + ":" + rightSide;
                        }
                    }

                    //capital letter I to 1
                    Pattern capI = Pattern.compile("(?=([I])([^a-zA-Z]|[IOo]|$)).");
                    Matcher Imatcher = capI.matcher(value);
                    value = Imatcher.replaceAll("1");

                    //letter "o" to 0
                    Pattern capO = Pattern.compile("(?=([Oo])([^a-zA-Z]|[IOo]|$)).");
                    Matcher Omatcher = capO.matcher(value);
                    value = Omatcher.replaceAll("0");

                    // Attempt to parse the Line as a date
                    //But only for strings longer than a little bit
                    if(value.length() >= 4) {

                        List<DateGroup> resultGroups = parser.parse(value);

                        if (resultGroups.size() > 0) {
                            //List<Date> foundDates = new LinkedList<>();
                            for (DateGroup dateGroup : resultGroups) {
                                if (dateGroup.isDateInferred()) {
                                    //Is time fragment
                                    Log.d(TAG, "TFRAG: " + dateGroup.getDates().get(0).toString());
                                    timeFragments.add(dateGroup.getDates().get(0));
                                } else if (dateGroup.isTimeInferred()) {
                                    //Is date fragment
                                    dateFragments.add(dateGroup.getDates().get(0));
                                    Log.d(TAG, "DFRAG: " + dateGroup.getDates().get(0).toString());
                                } else {
                                    //Is fully formed date
                                    completeDates.add(dateGroup.getDates().get(0));
                                    Log.d(TAG, "FULLDATE: " + dateGroup.getDates().get(0).toString());

                                }

                            }


                        }
                    }

                    OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item, value);
                    mGraphicOverlay.add(graphic);
                }
            }
        }
        //Now that we have identified all the dates in the detections this time around, let's see if any of them can be combined to make better dates
        if(timeFragments.size() > 1 || dateFragments.size() > 1) {
            Log.e(TAG, "Fragment classification: Either timefrags or datefrags has more than 1 item in it");
            Log.e(TAG, "Fragment classification: TimeFrags size: " + timeFragments.size());
            Log.e(TAG, "Fragment classification: DateFrags size: " + dateFragments.size());
        }

        Date finalDate = null;
        if(completeDates.size() > 0) {
            //We already have a complete date, we should do stuff with that
            finalDate = completeDates.get(0);
        } else if(timeFragments.size() > 0 && dateFragments.size() > 0) {
            //We have at least one date fragment and one time fragment, let's combine the first of each (There really should only be one of each anyway)
            Date datePart = dateFragments.get(0);
            Date timePart = timeFragments.get(0);
            timePart = purifyTimeFragment(timePart);
            datePart = purifyDateFragment(datePart);
            //In order to get just the date part of the date fragment, we set H,M,S, MS to 0

            //Now we have a blank date and a blank time, we can simply add them together
            long totalDate = datePart.getTime() + timePart.getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET);
            finalDate = new Date(totalDate);
        } else if(dateFragments.size() > 0) {
            finalDate = purifyDateFragment(dateFragments.get(0));
        }

        if(finalDate != null) {
            Log.e(TAG, "finalDate valid: " + finalDate.toString());
            //We have a date, show a toast of it
            //But only if it's not currently being displayed
            final Date actualFinalDate = new Date(finalDate.getTime());
            if(!oldDates.contains(actualFinalDate)) {
                context.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context, actualFinalDate.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                // Add the new recognized date to the queue, if there are more than five then delete the last one
                oldDates.add(actualFinalDate);
                if(oldDates.size() > 5) {
                    oldDates.remove();
                    Log.d(TAG, "Added the newly recognized date (" + actualFinalDate + ") and deleted the old queue item.");
                }
            }

        }
    }

    @Override
    public void release() {
        mGraphicOverlay.clear();
    }

    public Date purifyTimeFragment(Date input) {
        long wholeDays = (System.currentTimeMillis() / 3600000 / 24) - 1;
        long millisSinceEpoch = wholeDays * 24 * 3600000;
        input.setTime(input.getTime() - millisSinceEpoch);
        return input;
    }

    public Date purifyDateFragment(Date input) {
        Calendar c = Calendar.getInstance();
        c.setTime(input);
        c.set(Calendar.HOUR, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) - 1);
        input = c.getTime();
        return input;
    }
}
