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
import android.content.Context;
import android.text.Layout;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.dandyhacks.datereader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextBlock;
import com.joestelmach.natty.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
    private Parser parser = new Parser();

    private Date oldDate = new Date(1999,4,11);

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
        for (int i = 0; i < items.size(); ++i) {
            List<Line> lines = (List<Line>) items.valueAt(i).getComponents();
            for(Line item : lines) {
                if (item != null && item.getValue() != null){
                    Log.d("Processor", "Text Line detected! " + item.getValue());
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
                    List<DateGroup> resultGroups = parser.parse(value);
                    if(resultGroups.size() > 0) {
                        List<Date> foundDates = new LinkedList<>();
                        for (DateGroup dateGroup : resultGroups) {
                            List<Date> dates = dateGroup.getDates();
                            for (Date date : dates) {
                                Log.d("ProcessorDateParser", date.toString());
                                foundDates.add(date);
                            }
                        }
                        final Date dateToPrint = foundDates.get(0);
                        if(!dateToPrint.equals(oldDate)) {
                            context.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(context, dateToPrint.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                            oldDate = dateToPrint;
                        }
                    }

                    OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item, value);
                    mGraphicOverlay.add(graphic);
                }
            }
        }
    }

    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}
