package edu.cmu.hcii.sugilite.ui;
/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// package com.google.android.marvin.utils;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Handles drawing the screen reader cursor on-screen.
 */
public class HighlightBoundsView extends View {
    private final int[] SCREEN_LOCATION = new int[2];

    private final Rect mTemp = new Rect();
    private final Paint mPaint = new Paint();
    private final HashSet<AccessibilityNodeInfo> mNodes = new HashSet<AccessibilityNodeInfo>();
    private final Matrix mMatrix = new Matrix();

    private int mHighlightColor;

    /**
     * Constructs a new highlight bounds view using the specified attributes.
     *
     * @param context The parent context.
     * @param attrs The view attributes.
     */
    public HighlightBoundsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeJoin(Join.ROUND);
        mHighlightColor = Color.RED;
        setWillNotDraw(false);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        getLocationOnScreen(SCREEN_LOCATION);
    }

    @Override
    public void onDraw(Canvas c) {
        final int saveCount = c.save();
        c.translate(-SCREEN_LOCATION[0], -SCREEN_LOCATION[1]);
        c.setMatrix(mMatrix);

        mPaint.setColor(mHighlightColor);

        /*This for loop should not be required*/
        for (AccessibilityNodeInfo node : mNodes) {
            node.getBoundsInScreen(mTemp);
            mTemp.offset(this.SCREEN_LOCATION[0] * -1, this.SCREEN_LOCATION[1] * - 1);
            c.drawRect(mTemp, mPaint);
        }
        //added
        removeInvalidNodes();

        c.restoreToCount(saveCount);
    }

    /**
     * Sets the color of the highlighted bounds.
     *
     * @param color
     */
    public void setHighlightColor(int color) {
        mHighlightColor = color;
    }

    /**
     * Sets the width of the stroke width.
     *
     * @param width
     */
    public void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    public void clear() {
        for (AccessibilityNodeInfo node : mNodes) {
            node.recycle();
        }

        mNodes.clear();
    }

    /**
     * Sets the highlighted bounds to those of the specified node.
     *
     * @param node The node to highlight.
     */
    public void add(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        //final AccessibilityNodeInfo clone = AccessibilityNodeInfo.obtain(node);
        final AccessibilityNodeInfo clone = node;

        if(mNodes.contains(clone))
            mNodes.remove(clone);
        mNodes.add(clone);
        this.invalidate();
    }

    /**
     * Removes nodes that are no longer accessible.
     */
    public void removeInvalidNodes() {
        final Iterator<AccessibilityNodeInfo> iterator = mNodes.iterator();

        while (iterator.hasNext()) {
            final AccessibilityNodeInfo node = iterator.next();

            if (!isValidNode(node)) {
                iterator.remove();
                node.recycle();
            }
        }
    }

    private boolean isValidNode(AccessibilityNodeInfo node) {
        final AccessibilityNodeInfo parent = node.getParent();

        if (parent != null) {
            parent.recycle();
            return true;
        }

        final int childCount = node.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {
                child.recycle();
                return true;
            }
        }

        return false;
    }
}