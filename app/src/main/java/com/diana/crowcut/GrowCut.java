package com.diana.crowcut;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;

class GrowCut {
    public static final int FOREGROUND_COLOUR = Color.RED;
    public static final int BACKGROUND_COLOUR = Color.BLUE;
    private static final int[] nighX = { 1, -1, 0, 0 };
    private static final int[] nighY = { 0, 0, 1, -1 };

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
        double maxCellNorm2 = 0;
        for (int cellX = 0; cellX < w; ++cellX) {
            for (int cellY = 0; cellY < h; ++cellY) {
                maxCellNorm2 = Math.max(maxCellNorm2, getNorm2(imageData.getPixel(cellX, cellY)));
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

        for (int countSteps = 0; countSteps < 5; ++countSteps) {
            step = 1 - step;
            Boolean wasModified = false;
            for (int cellX = 0; cellX < w; ++cellX) {
                for (int cellY = 0; cellY < h; ++cellY) {
                    labels[cellX][cellY][step] = labels[cellX][cellY][1 - step];
                    strengths[cellX][cellY][step] = strengths[cellX][cellY][1 - step];
                    for (int k = 0; k < nighX.length; ++k) {
                        int neighX = cellX + nighX[k];
                        int neighY = cellY + nighY[k];
                        if (neighX >= 0 && neighY >= 0 && neighX < w && neighY < h) {
                            double attackingStrength = strengths[neighX][neighY][1 - step];
                            attackingStrength *= 1 - getNorm2(imageData.getPixel(cellX, cellY),
                                    imageData.getPixel(neighX, neighY)) / maxCellNorm2;
                            if (attackingStrength > strengths[cellX][cellY][1 - step] + 1e-2) {
                                labels[cellX][cellY][step] = labels[neighX][neighX][1 - step];
                                strengths[cellX][cellY][step] = attackingStrength;
                                wasModified = true;
                            }
                        }
                    }
                }
            }
            if (!wasModified) {
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

    protected static double getNorm2(int colour) {
        return getNorm2(Color.red(colour), Color.green(colour), Color.blue(colour));
    }

    protected static double getNorm2(int colour1, int colour2) {
        return getNorm2(
                Color.red(colour1) - Color.red(colour2),
                Color.green(colour1) - Color.green(colour2),
                Color.blue(colour1) - Color.blue(colour2));
    }

    protected static double getNorm2(double r, double g, double b) {
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
