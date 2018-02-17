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

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextBlock;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
        mGraphicOverlay = ocrGraphicOverlay;
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
                            String rightSide = value.substring(j+rightCount, value.length()-1);
                            value = leftSide + ":" + rightSide;


                        }
                    }

                    //capital letter I to 1
                    Pattern capI = Pattern.compile("(?=([I])([^a-zA-Z]|[IO]|$)).");
                    Matcher Imatcher = capI.matcher(value);
                    value = Imatcher.replaceAll("1");

                    //letter "o" to 0
                    Pattern capO = Pattern.compile("(?=([O])([^a-zA-Z]|[IO]|$)).");
                    Matcher Omatcher = capO.matcher(value);
                    value = Omatcher.replaceAll("0");

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
