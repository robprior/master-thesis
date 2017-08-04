package com.androidexperiments.MorphologyTests.example;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidexperiments.MorphologyTests.example.gl.ExampleRenderer;
import com.androidexperiments.MorphologyTests.fragments.CameraFragment;
import com.androidexperiments.MorphologyTests.gl.CameraRenderer;
import com.androidexperiments.MorphologyTests.utils.ShaderUtils;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Written by Anthony Tripaldi
 *
 * Very basic implemention of shader camera.
 */
public class MainActivity extends FragmentActivity implements CameraRenderer.OnRendererReadyListener, SeekBar.OnSeekBarChangeListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";

    private static final String VIDEO_FILE_NAME = "FingerDetection.mp4";
    /**
     * We inject our views from our layout xml here using {@link ButterKnife}
     */
    @InjectView(R.id.texture_view) TextureView mTextureView;

    /**
     * Custom fragment used for encapsulating all the {@link android.hardware.camera2} apis.
     */
    private CameraFragment mCameraFragment;

    /**
     * Our custom renderer for this example, which extends {@link CameraRenderer} and then adds custom
     * shaders, which turns shit green, which is easy.
     */
    private ExampleRenderer mRenderer;

    /**
     * boolean for triggering restart of camera after completed rendering
     */
    private boolean mRestartCamera = false;

    private final int CAMERA_PERMISSION = 1;
    private boolean mCameraPermissionGranted = false;
    private boolean mRendererReady = false;


    // -- Threshold Layout --
    @InjectView(R.id.threshSliderLayout) LinearLayout mThreshLayout;
    @InjectView(R.id.SeekBGGenBounds) SeekBar mSeekBGGenBounds;
    @InjectView(R.id.SeekBGGenInc) SeekBar mSeekBGGenInc;
    @InjectView(R.id.SeekBGSubMin) SeekBar mSeekBGSubMin;
    @InjectView(R.id.SeekBGSubMax) SeekBar mSeekBGSubMax;
    @InjectView(R.id.TextViewBGGen) TextView mTextBGGen;
    @InjectView(R.id.TextViewBGSubBounds) TextView mTextBGSubBounds;

    @InjectView(R.id.SeekSizeStructElem) SeekBar  mSeekStructElemSize;
    @InjectView(R.id.TextStructElemSize) TextView mTextStructElemSize;

    @InjectView(R.id.TextNumCodeElements) TextView mTextNumCodeElements;
    @InjectView(R.id.SeekNumCodeElements) SeekBar  mSeekNumCodeElements;

    @InjectView(R.id.seekArmWidthThresh) SeekBar mSeekArmWidth;
    @InjectView(R.id.textArmWidthThresh) TextView mTextArmWidth;

    @InjectView(R.id.SeekFillThresh) SeekBar  mSeekFIllThresh;
    @InjectView(R.id.textFillThresh) TextView mTextFillThresh;

    @InjectView(R.id.SeekCannyThresh) SeekBar mSeekCannyThresh;
    @InjectView(R.id.textCannyThresh) TextView mTextCannyThresh;

    @InjectView(R.id.SeekNeighThresh) SeekBar  mSeekFingerNeighbourNum;
    @InjectView(R.id.textNeighThresh) TextView mTextFingerNeighbourNum;

    @InjectView(R.id.SeekFingerAngleThreshMax) SeekBar  mSeekFingerAngleThreshMax;
    @InjectView(R.id.textFingerAngleThreshMax) TextView mTextFingerAngleThreshMax;
    @InjectView(R.id.SeekFingerAngleThreshMin) SeekBar  mSeekFingerAngleThreshMin;
    @InjectView(R.id.textFingerAngleThreshMin) TextView mTextFingerAngleThreshMin;

    @InjectView(R.id.SeekDistThreshMax) SeekBar  mSeekDistThreshMax;
    @InjectView(R.id.TextDistThreshMax) TextView mTextDistThreshMax;

    @InjectView(R.id.SeekDistThreshMin) SeekBar  mSeekDistThreshMin;
    @InjectView(R.id.TextDistThreshMin) TextView mTextDistThreshMin;

    @InjectView(R.id.SeekZoomLevel) SeekBar  mSeekZoomLevel;
    @InjectView(R.id.textZoomLevel) TextView mTextZoomLevel;

    @InjectView(R.id.SeekFingerAccumThresh) SeekBar  mSeekFingerAccumThresh;
    @InjectView(R.id.textFingerAccumThresh) TextView mTextFingerAccumThresh;

    //@InjectView(R.id.SeekFingerDistThresh) SeekBar  mSeekFingerDistThresh;
    //@InjectView(R.id.textFingerDistThresh) TextView mTextFingerDistThresh;

    @InjectView(R.id.SeekKmeansIters) SeekBar  mSeekKmeansIters;
    @InjectView(R.id.textKmeansIters) TextView mTextKmeansIters;

    @InjectView(R.id.SeekKmeansDistThresh) SeekBar  mSeekKmeansDistThresh;
    @InjectView(R.id.textKmeansDistThresh) TextView mTextKmeansDistThresh;

    @InjectView(R.id.SeekAccumConvolveKern) SeekBar mSeekAccumConvolveKern;
    @InjectView(R.id.textAccumConvolveKern) TextView mTextAccumConvolveKern;

    @InjectView(R.id.SeekReposThresh) SeekBar  mSeekReposThresh;
    @InjectView(R.id.TextReposThresh) TextView mTextReposThresh;

    @InjectView(R.id.btn_record) Button mRecordButton;

    @InjectView(R.id.textFPS) TextView mTextFPS;
    @InjectView(R.id.textGesture) TextView mTextGesture;

    // -- Codebook Layout --
    private final String sBoundsText       = "Bounds for Codebook Gen %f, Increment %f";
    private final String sModText          = "Bounds for Backgrond Sub Min %f, Max %f";
    private final String sNumCEText        = "Code Elements: %d";
    private final String sStructElem       = "Structuring Element Size: %d";
    private final String sMorphOPsText     = "Morphology Ops: %10s";
    private final String sArmWidthText     = "ArmWidthThresh: %d";
    private final String sFillText         = "Fill Threshold: %03d";
    private final String sCannyThresh      = "Canny Threshold: %f";
    private final String sFingNumThresh    = "Neighbourhood Size: %d";
    private final String sFingAngThreshMax = "AngleThresholdMax: %d";
    private final String sFingAngThreshMin = "AngleThresholdMin: %d";
    private final String sDistThreshMax    = "Distance Threshold Max: %d";
    private final String sDistThreshMin    = "Distance Threshold Min: %d";
    private final String sZoomLevel        = "Zoom: %f";
    private final String sFingAccumThresh  = "FingerAccumThresh %d";
    //private final String sFingerDistThresh = "FingerDistThresh %d";
    private final String sKmeansIters      = "KmeansIters %d";
    private final String sKmeansDistThresh = "KmeansDist %d";
    private final String sAccumConvolve    = "AccumConvolve %d";
    private final String sReposThresh      = "Respos: %d";
    private final String sFPS              = "%.3f FPS";

    public final static int MAX_MORPH_OPS = 10;
    @InjectView(R.id.textMorphOps) TextView mTextMorphOps;
    private String mMorphOpsString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        setupCameraFragment();
        setupInteraction();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)                 != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED    ) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION);
        } else {
            mCameraPermissionGranted = true;
        }

        mSeekBGGenBounds.setOnSeekBarChangeListener(this);
        mSeekBGGenInc.setOnSeekBarChangeListener(this);
        mSeekBGSubMin.setOnSeekBarChangeListener(this);
        mSeekBGSubMax.setOnSeekBarChangeListener(this);
        mSeekNumCodeElements.setOnSeekBarChangeListener(this);
        mSeekArmWidth.setOnSeekBarChangeListener(this);
        mSeekFIllThresh.setOnSeekBarChangeListener(this);
        mSeekCannyThresh.setOnSeekBarChangeListener(this);
        mSeekBGGenBounds.setMax(100);
        mSeekBGGenInc.setMax(100);
        mSeekBGSubMin.setMax(100);
        mSeekBGSubMax.setMax(100);
        mSeekBGGenBounds.setProgress(20);
        mSeekBGGenInc.setProgress(5);
        mSeekBGSubMin.setProgress(10);
        mSeekBGSubMax.setProgress(10);

        mSeekArmWidth.setMax(300);
        mSeekArmWidth.setProgress(100);
        mTextArmWidth.setText(String.format(sArmWidthText, 100));

        mSeekFIllThresh.setMax(100);
        mSeekFIllThresh.setProgress(5);
        mTextFillThresh.setText(String.format(sFillText, 5));

        mSeekCannyThresh.setProgress(100);
        mTextCannyThresh.setText(String.format(sCannyThresh, 0.1));

        mSeekBGSubMax.setProgress(100);

        mSeekNumCodeElements.setProgress(3);
        mTextNumCodeElements.setText(String.format(sNumCEText, 3));

        mSeekStructElemSize.setOnSeekBarChangeListener(this);
        mSeekStructElemSize.setProgress(5);
        mTextStructElemSize.setText(String.format(sStructElem, 5));

        mSeekFingerNeighbourNum.setOnSeekBarChangeListener(this);
        mSeekFingerNeighbourNum.setProgress(100);
        mTextFingerNeighbourNum.setText(String.format(sFingNumThresh, 100));

        mSeekFingerAngleThreshMax.setOnSeekBarChangeListener(this);
        mSeekFingerAngleThreshMax.setProgress(75);
        mTextFingerAngleThreshMax.setText(String.format(sFingAngThreshMax, 75));
        mSeekFingerAngleThreshMin.setOnSeekBarChangeListener(this);
        mSeekFingerAngleThreshMin.setProgress(10);
        mTextFingerAngleThreshMin.setText(String.format(sFingAngThreshMin, 10));

        mSeekDistThreshMax.setOnSeekBarChangeListener(this);
        mSeekDistThreshMax.setProgress(400);
        mTextDistThreshMax.setText(String.format(sDistThreshMax, 400));
        mSeekDistThreshMin.setOnSeekBarChangeListener(this);
        mSeekDistThreshMin.setProgress(185);
        mTextDistThreshMin.setText(String.format(sDistThreshMin, 185));

        /*final HorizontalScrollView hsv = (HorizontalScrollView)findViewById(R.id.RandomSliders);
        mSeekDistThreshMax.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    hsv.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });*/

        mSeekFingerAccumThresh.setOnSeekBarChangeListener(this);
        mSeekFingerAccumThresh.setProgress(1);
        mTextFingerAccumThresh.setText(String.format(sFingAccumThresh, 1));

        mSeekZoomLevel.setOnSeekBarChangeListener(this);
        mSeekZoomLevel.setProgress(1);
        mTextZoomLevel.setText(String.format(sZoomLevel, 1.0));

        //mSeekFingerDistThresh.setOnSeekBarChangeListener(this);
        //mSeekFingerDistThresh.setProgress(100);
        //mTextFingerDistThresh.setText(String.format(sFingerDistThresh, 100));

        mSeekKmeansIters.setOnSeekBarChangeListener(this);
        mSeekKmeansIters.setProgress(5);
        mTextKmeansIters.setText(String.format(sKmeansIters, 5));

        mSeekKmeansDistThresh.setOnSeekBarChangeListener(this);
        mSeekKmeansDistThresh.setProgress(60);
        mTextKmeansIters.setText(String.format(sKmeansDistThresh, 0));

        mSeekAccumConvolveKern.setOnSeekBarChangeListener(this);
        mSeekAccumConvolveKern.setProgress(3);
        mTextAccumConvolveKern.setText(String.format(sAccumConvolve, 3));

        mSeekReposThresh.setOnSeekBarChangeListener(this);
        mSeekReposThresh.setProgress(50);
        mTextReposThresh.setText(String.format(sReposThresh, 50));

        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.v(TAG, "maxMemory allowed to allocate before error: " + Long.toString(maxMemory) + " bytes");

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.v(TAG, "memoryClass memory should be below " + Integer.toString(memoryClass) + " MegaBytes");
    }

    public void onRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.switchShader(v.getId());
    }

    public void onMorphRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        onClickClear();
        mRenderer.swapMorphologyStructuringElement(v.getId());
    }

    public void onFillRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapFillMethod(v.getId());
    }

    public void onCannyRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapCanny(v.getId());
    }

    public void onRemoveContourBreakRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.switchRemoveContourBreaks(v.getId());
    }

    public void onDrawLineButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapDrawLines(v.getId());
    }

    public void onContourGenRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapContourAlg(v.getId());
    }

    public void onZoomRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapZoom(v.getId());
    }

    public void onContourThinRadioButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapContourThinniing(v.getId());
    }

    public void onKmeanResetCenterButtonClicked(View v) {
        boolean checked = ((RadioButton) v).isChecked();
        if (!checked) {
            return;
        }
        mRenderer.swapKmeansResetCenter(v.getId());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar bar, int progres, boolean fromUser) {
        if(mRenderer != null) {
            //TODO dunno how inefficient this is but should switch on id don't need to update all of them also this is p ugly
            //TODO should prob make a mpa or something too a particular seek id is always used with particular string / text/ get func
            mRenderer.setBounds(bar.getId(), progres);
            mTextBGGen.setText(String.format(sBoundsText, mRenderer.getBounds(), mRenderer.getiInc()));
            mTextBGSubBounds.setText(String.format(sModText, mRenderer.getMinMod(), mRenderer.getMaxMod()));
            mTextNumCodeElements.setText(String.format(sNumCEText, mRenderer.getNumCodeElements()));
            mTextStructElemSize.setText(String.format(sStructElem, mRenderer.getStructElemSize()));
            mTextArmWidth.setText(String.format(sArmWidthText, mRenderer.getArmWidthThresh()));
            mTextFillThresh.setText(String.format(sFillText, mRenderer.getFillThresh()));
            mTextCannyThresh.setText(String.format(sCannyThresh, mRenderer.getCannyThresh()));
            mTextFingerNeighbourNum.setText(String.format(sFingNumThresh, mRenderer.getFingerNeighbourhood()));
            mTextFingerAngleThreshMax.setText(String.format(sFingAngThreshMax, mRenderer.getFingerAngleThreshMax()));
            mTextFingerAngleThreshMin.setText(String.format(sFingAngThreshMin, mRenderer.getFingerAngleThreshMin()));
            mTextDistThreshMax.setText(String.format(sDistThreshMax, mRenderer.getDistThreshMax()));
            mTextDistThreshMin.setText(String.format(sDistThreshMin, mRenderer.getDistThreshMin()));
            mTextZoomLevel.setText(String.format(sZoomLevel, mRenderer.getZoomLevel()));
            mTextFingerAccumThresh.setText(String.format(sFingAccumThresh, mRenderer.getFingerAccumThresh()));
            //mTextFingerDistThresh.setText(String.format(sFingerDistThresh, mRenderer.getFingerDistThresh()));
            mTextKmeansIters.setText(String.format(sKmeansIters, mRenderer.getKmeansIterations()));
            mTextKmeansDistThresh.setText(String.format(sKmeansDistThresh, mRenderer.getKmeansDistThresh()));
            mTextAccumConvolveKern.setText(String.format(sAccumConvolve, mRenderer.getAccumConvolveSize()));
            mTextReposThresh.setText(String.format(sReposThresh, mRenderer.getReposTresh()));
        }
    }

    private void openCamera() {
        Log.d(TAG, "openCamera");
        if (!mRendererReady || !mCameraPermissionGranted) {
            return;
        }
        mCameraFragment.setPreviewTexture(mRenderer.getPreviewTexture());
        mCameraFragment.openCamera();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsReult");
        switch (requestCode) {
            case CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPermissionGranted = true;
                    openCamera();
                } else {
                    throw new IllegalStateException("Cannot aquire camera permissions");
                }
                return;
            }
        }
    }

    public void setCurrentFPSDisp(float fps) {
        mTextFPS.setText(String.format(sFPS, fps));
    }

    public Handler getFpsUpdateHandler() {
        return new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                //Handler expects FPS as an int of FPMS
                final long frameTime = (msg.arg1 << 32 & 0xFFFF0000) | (msg.arg2 &0x0000FFFF);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        float fps = 1000.0f / frameTime;
                        setCurrentFPSDisp(fps);
                    }
                });
                return true;
            }
        });
    }

    private enum Gestures {
        NONE,
        MOVE,
        SCALE,
        SCROLL,
        ROTATE,
    }
    private void setGestureDescription(int numFingersActive) {
        String text = "";
        switch (numFingersActive) {
            case 0:
            case 5:
                text = "None";
                break;
            case 1:
                text = "Move";
                break;
            case 2:
                text = "Scale";
                break;
            case 3:
                text = "Scroll";
                break;
            case 4:
                text = "Rotate";
                break;
        }
        mTextGesture.setText(text);
    }
    public Handler getGestureCallback() {
        return new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                //Handler expects num active finger
                final int numCenters = msg.arg1;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        setGestureDescription(numCenters);
                    }
                });
                return true;
            }
        });
    }

    /**
     * create the camera fragment responsible for handling camera state and add it to our activity
     */
    private void setupCameraFragment()
    {
        mCameraFragment = CameraFragment.getInstance();
        mCameraFragment.setCameraToUse(CameraFragment.CAMERA_PRIMARY); //pick which camera u want to use, we default to forward
        mCameraFragment.setTextureView(mTextureView);

        //add fragment to our setup and let it work its magic
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mCameraFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
    }

    /**
     * add a listener for touch on our surface view that will pass raw values to our renderer for
     * use in our shader to control color channels.
     */
    private void setupInteraction() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mRenderer.setTouchPoint(event.getRawX(), event.getRawY());
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        ShaderUtils.goFullscreen(this.getWindow());


        if(!mTextureView.isAvailable())
            mTextureView.setSurfaceTextureListener(mTextureListener); //set listener to handle when its ready
        else
            setReady(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    protected void onPause() {
        super.onPause();

        shutdownCamera(false);
        mTextureView.setSurfaceTextureListener(null);
    }

    @OnClick(R.id.btn_swap_camera)
    public void onClickSwapCamera()
    {
        mCameraFragment.swapCamera();
    }

    @OnClick(R.id.toggleCamLockButton)
    public void onClickCamLock() {
        mCameraFragment.toggleCameraLock();
    }

    @OnClick(R.id.buttonMoprhErode)
    public void onClickErode() {
        if (mMorphOpsString.length() >= MAX_MORPH_OPS) return;
        mMorphOpsString  += "E";
        mTextMorphOps.setText(String.format(sMorphOPsText, mMorphOpsString));
        mRenderer.addErodeOp();
    }

    @OnClick(R.id.buttonMorphDilate)
    public void onClickDilate() {
        if (mMorphOpsString.length() >= MAX_MORPH_OPS) return;
        mMorphOpsString  += "D";
        mTextMorphOps.setText(String.format(sMorphOPsText, mMorphOpsString));
        mRenderer.addDilateOp();
    }

    @OnClick(R.id.buttonMorphClear)
    public void onClickClear() {
        mMorphOpsString = "";
        mTextMorphOps.setText(String.format(sMorphOPsText, mMorphOpsString));
        mRenderer.clearOps();
    }

    private File getVideoFile()
    {
        return new File(Environment.getExternalStorageDirectory(), VIDEO_FILE_NAME);
    }
    @OnClick(R.id.btn_record)
    public void onClickRecordButton() {
        if(mRenderer.isRecording()) {
            mRenderer.stopRecording();
            mRecordButton.setText("Record");
            shutdownCamera(true);
            Toast.makeText(this, "File recording complete: " + getVideoFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            mRenderer.startRecording(getVideoFile());
            mRecordButton.setText("Stop");
        }
    }
    @OnClick(R.id.btn_SaveData)
    public void onClickSaveDataButton() {
        mRenderer.saveData();
    }

    /**
     * called whenever surface texture becomes initially available or whenever a camera restarts after
     * completed recording or resuming from onpause
     * @param surface {@link SurfaceTexture} that we'll be drawing into
     * @param width width of the surface texture
     * @param height height of the surface texture
     */
    private void setReady(SurfaceTexture surface, int width, int height) {
        mRenderer = new ExampleRenderer(this, surface, mCameraFragment, width, height);
        mRenderer.setOnRendererReadyListener(this);
        mRenderer.switchShader(R.id.radioCodeBookBGSub);
        mRenderer.start();

        mTextBGGen.setText(String.format(sBoundsText, mRenderer.getBounds(), mRenderer.getiInc()));
        mTextBGSubBounds.setText(String.format(sModText, mRenderer.getMinMod(), mRenderer.getMaxMod()));


        //initial config if needed
        mCameraFragment.configureTransform(width, height);
    }

    /**
     * kills the camera in camera fragment and shutsdown render thread
     * @param restart whether or not to restart the camera after shutdown is complete
     */
    private void shutdownCamera(boolean restart)
    {
        //check to make sure we've even created the cam and renderer yet
        if(mCameraFragment == null || mRenderer == null) return;

        mCameraFragment.closeCamera();

        mRestartCamera = restart;
        mRenderer.getRenderHandler().sendShutdown();
        mRenderer = null;
    }

    /**
     * Interface overrides from our {@link com.androidexperiments.MorphologyTests.gl.CameraRenderer.OnRendererReadyListener}
     * interface. Since these are being called from inside the CameraRenderer thread, we need to make sure
     * that we call our methods from the {@link #runOnUiThread(Runnable)} method, so that we don't
     * throw any exceptions about touching the UI from non-UI threads.
     *
     * Another way to handle this would be to create a Handler/Message system similar to how our
     * {@link com.androidexperiments.MorphologyTests.gl.CameraRenderer.RenderHandler} works.
     */
    @Override
    public void onRendererReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRendererReady = true;
                openCamera();
            }
        });
    }

    @Override
    public void onRendererFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRestartCamera) {
                    setReady(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
                    mRestartCamera = false;
                }
            }
        });
    }


    /**
     * {@link android.view.TextureView.SurfaceTextureListener} responsible for setting up the rest of the
     * rendering and recording elements once our TextureView is good to go.
     */
    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, final int width, final int height) {
                //convenience method since we're calling it from two places
                setReady(surface, width, height);
            }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            mCameraFragment.configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
    };



}
