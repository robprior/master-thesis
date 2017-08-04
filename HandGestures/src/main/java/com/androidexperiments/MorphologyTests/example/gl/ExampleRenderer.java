package com.androidexperiments.MorphologyTests.example.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.androidexperiments.MorphologyTests.example.MainActivity;
import com.androidexperiments.MorphologyTests.example.R;
import com.androidexperiments.MorphologyTests.fragments.CameraFragment;
import com.androidexperiments.MorphologyTests.gl.CameraRenderer;
import com.androidexperiments.MorphologyTests.gl.GlUtil;

/**
 * Example renderer that changes colors and tones of camera feed
 * based on touch position.
 */
public class ExampleRenderer extends CameraRenderer
{
    private float offsetX = 0.5f;
    private float offsetY = 0.5f;

    private float iInc   = 0.01f;
    private float bounds = 0.2f;
    private float minMod = 0.1f;
    private float maxMod = 0.1f;

    private int structElemSize = 5;

    private boolean useZoomScrolling = false;

    /**
     * By not modifying anything, our default shaders will be used in the assets folder of shadercam.
     *
     * Base all shaders off those, since there are some default uniforms/textures that will
     * be passed every time for the camera coordinates and texture coordinates
     */
    public ExampleRenderer(Context context, SurfaceTexture previewSurface, CameraFragment cameraFragment, int width, int height)
    {
        super(context, previewSurface, cameraFragment, width, height, "touchcolor.frag.glsl", "touchcolor.vert.glsl");

        //other setup if need be done here
        lastTime = SystemClock.elapsedRealtime();
        mFPSCallback        = ((MainActivity)context).getFpsUpdateHandler();
        mNumCentersCallback = ((MainActivity)context).getGestureCallback();
    }

    /**
     * we override {@link #setUniformsAndAttribs()} and make sure to call the super so we can add
     * our own uniforms to our shaders here. CameraRenderer handles the rest for us automatically
     */
    @Override
    protected void setUniformsAndAttribs()
    {
        super.setUniformsAndAttribs();


        GLES31.glUseProgram(mZoomProg);
        float scaledOffX = useZoomScrolling ? (offsetX - 1280.0f) / 1280.f : 0.0f;
        float scaledOffY = useZoomScrolling ? (offsetY -  720.0f) /  720.f : 0.0f;
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mZoomProg, "offsetX"), scaledOffX);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mZoomProg, "offsetY"), scaledOffY);
    }

    @Override
    protected void setupMorphology(){
        super.setupMorphology();

        GLES31.glUseProgram(mMorphCurDilateCompProg);
        GlUtil.checkGlError("Use morph dilate");
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mMorphCurDilateCompProg, "structuringElementSize"), structElemSize);
        GlUtil.checkGlError("set structuring size");
        GLES31.glUseProgram(mMorphCurErodeCompProg);
        GlUtil.checkGlError("Use morph erode");
        GLES31.glUniform1i(GLES31.glGetUniformLocation(mMorphCurErodeCompProg, "structuringElementSize"), structElemSize);
        GlUtil.checkGlError("set structuring size");

    }

    private int morphSteps[] = new int[MainActivity.MAX_MORPH_OPS];
    @Override
    protected void performMorphology(int inputMaskTexIdx, int scratchInputMaskTexIdx) {
        setupMorphology();
        int a = inputMaskTexIdx;
        int b = scratchInputMaskTexIdx;
        for (int i = 0; i<mMorphNumSteps; ++i) {
            GLES31.glUseProgram(morphSteps[i]);
            GlUtil.checkGlError("Use morph program in loop idx " + i + " progId " + morphSteps[i]);
            //morphLaunch(inputMaskTexIdx, scratchInputMaskTexIdx);
            morphLaunch(a, b);
            // swap
            //TODO maybe trying to be too fancy needs guaranteed LtoR b=a+b-(a=b)
            //scratchInputMaskTexIdx = (inputMaskTexIdx + scratchInputMaskTexIdx) - (inputMaskTexIdx=scratchInputMaskTexIdx);
            b=a+b-(a=b);
        }
    }

    public void swapMorphologyStructuringElement(int id) {
        clearOps();
        switch (id) {
            case R.id.radioMorphBox:
                mMorphCurDilateCompProg = mMorphBoxDilateCompProg;
                mMorphCurErodeCompProg  = mMorphBoxErodeCompProg;
                break;
            case R.id.radioMorphCross:
                mMorphCurDilateCompProg = mMorphCrossDilateCompProg;
                mMorphCurErodeCompProg  = mMorphCrossErodeCompProg;
                break;
            case R.id.radioMorphDiamond:
                mMorphCurDilateCompProg = mMorphDiamondDilateCompProg;
                mMorphCurErodeCompProg  = mMorphDiamondErodeCompProg;
                break;
        }
    }

    public void swapFillMethod(int id) {
        switch (id) {
            case R.id.radioFillNone:
                mFillMethod = FillMethod.NO_FILL;
                break;
            case R.id.radioFillNeighbour:
                mFillMethod = FillMethod.FILL_NEIGHBOUR;
                break;
            case R.id.radioFillDistance:
                mFillMethod = FillMethod.FILL_DISTANCE;
                break;
        }
    }

    public void swapZoom(int id) {
        switch (id) {
            case R.id.radioZoomOff:
                mUseZoom = false;
                break;
            case R.id.radioZoomOn:
                mUseZoom = true;
                useZoomScrolling = false;
                break;
            case R.id.radioZoomScrolling:
                mUseZoom = true;
                useZoomScrolling = true;
                break;
        }
    }

    public void addErodeOp() {
        morphSteps[mMorphNumSteps] = mMorphCurErodeCompProg;
        mMorphNumSteps++;
    }
    public void addDilateOp() {
        morphSteps[mMorphNumSteps] = mMorphCurDilateCompProg;
        mMorphNumSteps++;
    }
    public void clearOps() {
        mMorphNumSteps = 0;
    }

    private Handler mFPSCallback = null;
    private long lastTime;
    private static final int FRAME_TIME_SAMPLES = 50;
    private int mFrameTimeIndex = 0;
    private long[] mFrameTimeSampleBuffer = new long[FRAME_TIME_SAMPLES];
    private static final long updateRate = 250; // 1/4 second in ms
    private long frameTimeSum = 0;
    private boolean haveFilledBuffer = false;

    private void updateFPS() {
        long curTime = SystemClock.elapsedRealtime();
        long timeDif = curTime - lastTime;
        frameTimeSum += timeDif;
        long timeRemovedFromBuff = mFrameTimeSampleBuffer[mFrameTimeIndex];
        mFrameTimeSampleBuffer[mFrameTimeIndex++] = timeDif;
        if(mFrameTimeIndex == FRAME_TIME_SAMPLES) {
            haveFilledBuffer = true;
            mFrameTimeIndex = 0;
        }
        if (haveFilledBuffer) {
            frameTimeSum -= timeRemovedFromBuff;
        }
        //don't really care about decimal but might decrease accuracy
        long average = frameTimeSum / FRAME_TIME_SAMPLES;
        Message m = Message.obtain(
                mFPSCallback,
                0,
                (int) (average >> 32 & 0xFFFF),
                (int) (average & 0xFFFF));
        mFPSCallback.dispatchMessage(m);
        lastTime = curTime;
    }

    private Handler mNumCentersCallback = null;
    private void updateNumCenters() {
        Message m = Message.obtain(mNumCentersCallback, 0, lastNumActiveCenters);
        mNumCentersCallback.dispatchMessage(m);
    }

    @Override
    protected void setUniformsCompProg() {
        super.setUniformsCompProg();

        //TODO bit of a hack should put in a pre / post draw or something rather than this, just need a func called 1 per draw
        updateFPS();
        updateNumCenters();

        GLES31.glUniform1ui(GLES31.glGetUniformLocation(mCompProg, "numCE"), mNumCodeElements);
        GlUtil.checkGlError("");

        if (mCompProg == mGenCodebookSqrSqrtCompProg ||
            mCompProg == mGenCodebookAdditiveCompProg) {
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "BOUNDS"), bounds);
            GlUtil.checkGlError("");
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "I_INC"), iInc);
            GlUtil.checkGlError("");
        }
        if (mCompProg == mCodebookBGSubYCbCrSqrSqrtCompProg ||
            mCompProg == mCodebookBGSubYCbCrAdditiveCompProg ||
            mCompProg == mCodebookBGSubRGBSqrSqrtCompProg ||
            mCompProg == mCodebookBGSubRGBAdditiveCompProg ||
            mCompProg == mCodebookDisplay ||
            mCompProg == mCodebookMergeCompProg)
        {
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "MAX_MOD"), maxMod);
            GlUtil.checkGlError("");
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "MIN_MOD"), minMod);
            GlUtil.checkGlError("");
        }

        if (mCompProg == mCodebookDisplay) {
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "offsetX"), offsetX);
            GlUtil.checkGlError("");
            GLES20.glUniform1f(GLES20.glGetUniformLocation(mCompProg, "offsetY"), offsetY);
            GlUtil.checkGlError("");
        }
    }

    /**
     * take touch points on that textureview and turn them into multipliers for the color channels
     * of our shader, simple, yet effective way to illustrate how easy it is to integrate app
     * interaction into our glsl shaders
     * @param rawX raw x on screen
     * @param rawY raw y on screen
     */
    public void setTouchPoint(float rawX, float rawY)
    {
        offsetX = rawX;
        offsetY = rawY;
    }

    //TODO these are hacky way too much sharing with this and MainActivity
    public void setBounds(int id, int val) {
        float valNorm = (float)val / 100.0f;
        switch (id) {
            case R.id.SeekBGSubMin:
                minMod = valNorm;
                break;
            case R.id.SeekBGSubMax:
                maxMod = valNorm;
                break;
            case R.id.SeekBGGenBounds:
                bounds = valNorm;
                break;
            case R.id.SeekBGGenInc:
                iInc = valNorm;
                break;
            case R.id.SeekNumCodeElements:
                mNumCodeElements = val;
                break;
            case R.id.SeekSizeStructElem:
                structElemSize = (val % 2 == 0) ? val+1 : val;
                break;
            case R.id.seekArmWidthThresh:
                mArmWidthThreshold = (float)val;
                break;
            case R.id.SeekFillThresh:
                mFillInHolesThresh = (float)val;
                break;
            case R.id.SeekCannyThresh:
                mCannyThreshold = valNorm;
                break;
            case R.id.SeekNeighThresh:
                mFingerNeighbourhoodSize = val;
                break;
            case R.id.SeekFingerAngleThreshMax:
                mFingerAngleThreshMax = (float)val;
                break;
            case R.id.SeekFingerAngleThreshMin:
                mFingerAngleThreshMin = (float)val;
                break;
            case R.id.SeekDistThreshMax:
                mDistThreshMax = (float)val;
                break;
            case R.id.SeekDistThreshMin:
                mDistThreshMin = (float) val;
                break;
            case R.id.SeekZoomLevel:
                mZoomLevel = (float)val;
                break;
            case R.id.SeekFingerAccumThresh:
                mFingerAccumThresh = val;
                break;
            //case R.id.SeekFingerDistThresh:
            //    mFingPosDistThresh = val;
            //    break;
            case R.id.SeekKmeansIters:
                mKmeansIterations = val;
                break;
            case R.id.SeekKmeansDistThresh:
                mKmeansDistThresh = (float) val;
                break;
            case R.id.SeekAccumConvolveKern:
                mFingPosConvolveKernSize = val;
            case R.id.SeekReposThresh:
                mKmeansCenterReposThresh = (float)val;
                break;
        }
    }
    public float getBounds() {
        return bounds;
    }
    // some ints are returned as floats here because they will only ever be integers but need to be
    // compared to floating point values in GPU kernels would rather cast here rather than there
    public float getiInc()   {
        return iInc;
    }
    public float getMaxMod() {
        return maxMod;
    }
    public float getMinMod() {
        return minMod;
    }
    public int   getNumCodeElements() {
        return mNumCodeElements;
    }
    public int   getStructElemSize() { return structElemSize; }
    public int   getArmWidthThresh() { return (int)mArmWidthThreshold;}
    public int   getFillThresh()     { return (int)mFillInHolesThresh;}
    public float   getCannyThresh()    { return mCannyThreshold; }
    public int getFingerNeighbourhood() { return mFingerNeighbourhoodSize; }
    public int getFingerAngleThreshMax() { return (int)mFingerAngleThreshMax;}
    public int getFingerAngleThreshMin() { return (int)mFingerAngleThreshMin;}
    public int getDistThreshMax()        { return (int) mDistThreshMax;}
    public int getDistThreshMin()        { return (int) mDistThreshMin;}
    public int   getFingerAccumThresh() { return mFingerAccumThresh; }
    public float getZoomLevel() {return mZoomLevel;}
    //public int getFingerDistThresh() { return (int)mFingPosDistThresh;}
    public int getKmeansIterations() { return mKmeansIterations;}
    public int getKmeansDistThresh() { return (int)mKmeansDistThresh;}
    public int getAccumConvolveSize() { return mFingPosConvolveKernSize; }
    public int getReposTresh() { return  (int)mKmeansCenterReposThresh; }

    public void swapCanny(int id) {
        if (id == R.id.radioCannyOn) {
            mFingerDetectionState = FingerEdgeDetectionStep.CANNY;
        } else if (id == R.id.radioCannyOff) {
            mFingerDetectionState = FingerEdgeDetectionStep.OFF;
        } else if (id == R.id.radioSimpleEdge) {
            mFingerDetectionState = FingerEdgeDetectionStep.SIMPLE_EDGE;
        } else if (id == R.id.radioSimpleEdgeThin) {
            mFingerDetectionState = FingerEdgeDetectionStep.SIMPLE_EDGE_THIN;
        } else if (id == R.id.radioSimpleStepEdge) {
            mFingerDetectionState = FingerEdgeDetectionStep.SIMPLE_EDGE_STEP;
        }  else if (id == R.id.radioFingerDetectLine) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_LINE;
        } else if (id == R.id.radioFingerDetectBox) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_BOX;
        } else if (id == R.id.radioSimpleStepEdgeDebug) {
            mFingerDetectionState = FingerEdgeDetectionStep.SIMPLE_EDGE_DEBUG;
        } else if (id == R.id.radioFingerDetectAccum) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_DISPLAY;
        } else if (id == R.id.radioFingerDetectAccumSmooth) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_CONVOLVE;
        } else if (id == R.id.radioFingerDetectAccumNonMaxSupp) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_NONMAXSUPP;
        } else if (id == R.id.radioFingerDetectAccumAreaAdd) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_AREA_ADD;
        } else if (id == R.id.radioFingerDetectMidpoint) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_MIDPOINT;
        } else if (id == R.id.radioFingerDetectKMeans) {
            mFingerDetectionState = FingerEdgeDetectionStep.FINGER_DETECT_KMEANS;
        }
    }

    public void saveData() {
        mSaveData = true;
    }

    public void swapDrawLines(int id) {
        mDrawShortLine = 0;
        mDrawLongLine  = 0;
        if (id == R.id.radioDrawBothLines) {
            mDrawShortLine = 1;
            mDrawLongLine  = 1;
        } else if (id == R.id.radioDrawShorterLine) {
            mDrawShortLine = 1;
        } else if (id == R.id.radioDrawLongerLine) {
            mDrawLongLine  = 1;
        }
    }

    public void swapContourThinniing(int id) {
        if (id == R.id.radioContourThinOff) {
            mContourThinningAlg = ContourThinningAlg.OFF;
        } else if (id == R.id.radioContourThinKWK) {
            mContourThinningAlg = ContourThinningAlg.KWK;
        } else if (id == R.id.radioContourThinKWKThirdOnly) {
            mContourThinningAlg = ContourThinningAlg.KWK_THIRD_PASS_ONLY;
        } else if (id == R.id.radioContourThinZS) {
            mContourThinningAlg = ContourThinningAlg.ZS;
        }
    }

    public void swapContourAlg(int id) {
        if (id == R.id.radioSimple) {
            mContourGenerationAlg = ContourGenerationAlg.SIMPLE;
        } else if (id == R.id.radioSimpleOrth) {
            mContourGenerationAlg = ContourGenerationAlg.SIMPLE_ORTH;
        } else if (id == R.id.radioContourLeite) {
            mContourGenerationAlg = ContourGenerationAlg.LEITE;
        }
    }

    public void switchRemoveContourBreaks(int id) {
        if (id == R.id.radioContourBreakOff) {
            mRemoveContourBreak = false;
        } else if (id == R.id.radioContourBreakOn) {
            mRemoveContourBreak = true;
        }
    }

    public void swapKmeansResetCenter(int id) {
        mKmeanHaveResetCenterOnce = false;
        if (id == R.id.radioKmeanResetCenterOn) {
            mKmeanResetCenter = true;
        } else if (id == R.id.radioKmeanResetCenterOff) {
            mKmeanResetCenter = false;
        }
    }

    //TODO hack
    private int lastCodebookGenProg;
    public void switchShader(final int id) {
        mReInitializeCodebookBuffer = true;

        switch(id) {
            case R.id.radioCBGenYCbCrSqrSqrt:
                mCameraShaderProgram = mYCbCrProg;
                mCompProg = mGenCodebookSqrSqrtCompProg;
                lastCodebookGenProg = mCompProg;
                break;
            case R.id.radioCBGenYCbCrAdditive:
                mCameraShaderProgram = mYCbCrProg;
                mCompProg = mGenCodebookAdditiveCompProg;
                lastCodebookGenProg = mCompProg;
                break;
            case R.id.radioCBGenRGBSqrSqrt:
                mCameraShaderProgram = mBaseShaderProg;
                mCompProg = mGenCodebookSqrSqrtCompProg;
                lastCodebookGenProg = mCompProg;
                break;
            case R.id.radioCBGenRGBAdditive:
                mCameraShaderProgram = mBaseShaderProg;
                mCompProg = mGenCodebookAdditiveCompProg;
                lastCodebookGenProg = mCompProg;
                break;
            case R.id.radioDispCodebook:
                mCompProg = mCodebookDisplay;
                mCameraShaderProgram = mDrawCamTexProg;
                mReInitializeCodebookBuffer = false;
                break;
            case R.id.radioCodeBookBGSub:
                if (mCameraShaderProgram == mYCbCrProg) {
                    if (mCompProg == mGenCodebookSqrSqrtCompProg) {
                        mCompProg = mCodebookBGSubYCbCrSqrSqrtCompProg;
                    } else { // additive
                        mCompProg = mCodebookBGSubYCbCrAdditiveCompProg;
                    }
                } else { // RGB
                    if (mCompProg == mGenCodebookSqrSqrtCompProg) {
                        mCompProg = mCodebookBGSubRGBSqrSqrtCompProg;
                    } else { // additive
                        mCompProg = mCodebookBGSubRGBAdditiveCompProg;
                    }
                }

                if(mCompProg == mCodebookDisplay) {
                    mCompProg = lastCodebookGenProg;
                }

                mCameraShaderProgram = mDrawCamTexProg;
                mReInitializeCodebookBuffer = false;
                break;
            case R.id.radioCodeBookWhiteThresh:
                mCameraShaderProgram = mSimpleThresholdProg;
                mCompProg = -1;
                mReInitializeCodebookBuffer = false;
                break;
            case R.id.radioCodeBookGeneralThresh:
                mCameraShaderProgram = mSimpleThresholdAllProg;
                mCompProg = -1;
                mReInitializeCodebookBuffer = false;
                break;
            case R.id.radioRawCamera:
                mCompProg = -1;
                mReInitializeCodebookBuffer = false;
        }
    }
}
