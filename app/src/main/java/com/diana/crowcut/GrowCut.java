package com.diana.crowcut;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;

class GrowCut {
    public static final int FOREGROUND_COLOUR = Color.RED;
    public static final int BACKGROUND_COLOUR = Color.BLUE;
    private static final int WINDOW_SIZE = 3;
    private static final int MAX_ITERATIONS = 30;
    private static final long MAX_EXEC_TIME_MS = 2 * 60 * 1000;

    static Bitmap run(Context context, Bitmap imageData, Bitmap guides, Boolean isParallel)
    {
        if (imageData.getWidth() != guides.getWidth() || imageData.getHeight() != guides.getHeight())
        {
            return imageData;
        }
        return isParallel
                ? GrowCutRenderScript(context, imageData, guides)
                : GrowCutIterative(imageData, guides);
    }

    private static Bitmap GrowCutIterative(Bitmap imageData, Bitmap guides)
    {
        int w = imageData.getWidth();
        int h = imageData.getHeight();
        int[][][] labels = new int[w][h][2];
        double[][][] strengths = new double[w][h][2];
        int step = 0;
        for (int cellX = 0; cellX < w; ++cellX) {
            for (int cellY = 0; cellY < h; ++cellY) {
                switch (guides.getPixel(cellX, cellY)) {
                    case FOREGROUND_COLOUR:
                        labels[cellX][cellY][step] = 2;
                        strengths[cellX][cellY][step] = 1;
                        break;
                    case BACKGROUND_COLOUR:
                        labels[cellX][cellY][step] = 1;
                        strengths[cellX][cellY][step] = 1;
                        break;
                    default:
                        labels[cellX][cellY][step] = 0;
                        strengths[cellX][cellY][step] = 0;
                        break;
                }
            }
        }

        final long startTime = System.currentTimeMillis();
        final int ws = (WINDOW_SIZE + 1) >> 1;
        for (int countSteps = 0; countSteps < MAX_ITERATIONS; ++countSteps) {
            step = 1 - step;
            Boolean wasModified = false;
            for (int cellX = 0; cellX < w; ++cellX) {
                for (int cellY = 0; cellY < h; ++cellY) {
                    labels[cellX][cellY][step] = labels[cellX][cellY][1 - step];
                    strengths[cellX][cellY][step] = strengths[cellX][cellY][1 - step];
                    neighbourLoop:
                    for (int neighX = Math.max(0, cellX - ws); neighX < Math.min(cellX + ws + 1, w); ++neighX) {
                        for (int neighY = Math.max(0, cellY - ws); neighY < Math.min(cellY + ws + 1, h); ++neighY) {
                            double attackingStrength = strengths[neighX][neighY][1 - step];
                            attackingStrength *= 1 - getNorm2(imageData.getPixel(cellX, cellY),
                                    imageData.getPixel(neighX, neighY)) / Math.sqrt(3.0);
                            if (attackingStrength > strengths[cellX][cellY][step] + 1e-2) {
                                labels[cellX][cellY][step] = labels[neighX][neighY][1 - step];
                                strengths[cellX][cellY][step] = attackingStrength;
                                wasModified = true;
                                break neighbourLoop;
                            }
                        }
                    }
                }
            }
            if (!wasModified || System.currentTimeMillis() - startTime > MAX_EXEC_TIME_MS) {
                break;
            }
        }

        Bitmap result = imageData.copy(imageData.getConfig(), true);
        for (int cellX = 0; cellX < w; ++cellX) {
            for (int cellY = 0; cellY < h; ++cellY) {
                if (labels[cellX][cellY][step] != 2) {
                    result.setPixel(cellX, cellY, Color.TRANSPARENT);
                }
            }
        }
        return result;
    }

    protected static double getNorm2(int colour1, int colour2) {
        return getNorm2(
                Color.red(colour1) - Color.red(colour2),
                Color.green(colour1) - Color.green(colour2),
                Color.blue(colour1) - Color.blue(colour2));
    }

    protected static double getNorm2(double r, double g, double b) {
        r /= 255; g /= 255; b /= 255;
        return Math.sqrt(r * r + g * g + b * b);
    }

    private static Bitmap GrowCutRenderScript(Context context, Bitmap imageData, Bitmap guides)
    {
        final RenderScript rs = RenderScript.create(context);
        final ScriptC_GrowCut script = new ScriptC_GrowCut(rs);
        final Allocation inputAllocation = Allocation.createFromBitmap(rs, imageData);
        final Bitmap outputBitmap = imageData.copy(imageData.getConfig(), imageData.isMutable());
        final Allocation outputAllocation = Allocation.createFromBitmap(rs, outputBitmap);
        script.invoke_process(inputAllocation, outputAllocation);
        rs.destroy();
        return outputBitmap;
    }
}
