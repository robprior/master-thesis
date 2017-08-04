package com.androidexperiments.MorphologyTests.gl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaRecorder;
import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.androidexperiments.MorphologyTests.fragments.CameraFragment;
import com.androidexperiments.MorphologyTests.utils.ShaderUtils;
import com.google.labs.androidexperiments.shadercamera.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/** *
 * Base camera rendering class. Responsible for rendering to proper window contexts, as well as
 * recording video with built-in media recorder.
 *
 * Subclass this and add any kind of fun stuff u want, new shaders, textures, uniforms - go to town!
 *
 * TODO: add methods for users to create their own mediarecorders/change basic settings of default mr
 */

public class CameraRenderer extends Thread implements SurfaceTexture.OnFrameAvailableListener
{
    private static final String TAG = CameraRenderer.class.getSimpleName();
    private static final String THREAD_NAME = "CameraRendererThread";

    /**
     * if you create new files, just override these defaults in your subclass and
     * don't edit the {@link #vertexShaderCode} and {@link #fragmentShaderCode} variables
     */
    protected String DEFAULT_FRAGMENT_SHADER = "camera.frag.glsl";

    protected String DEFAULT_VERTEX_SHADER = "camera.vert.glsl";

    /**
     * Current context for use with utility methods
     */
    protected Context mContext;

    protected int mSurfaceWidth, mSurfaceHeight;

    protected float mSurfaceAspectRatio;

    /**
     * main texture for display, based on TextureView that is created in activity or fragment
     * and passed in after onSurfaceTextureAvailable is called, guaranteeing its existence.
     */
    private SurfaceTexture mSurfaceTexture;

    /**
     * EGLCore used for creating {@link WindowSurface}s for preview and recording
     */
    private EglCore mEglCore;

    /**
     * Primary {@link WindowSurface} for rendering to screen
     */
    private WindowSurface mWindowSurface;

    /**
     * primary {@link WindowSurface} for use with mediarecorder
     */
    private WindowSurface mRecordSurface;

    /**
     * Texture created for GLES rendering of camera data
     */
    private SurfaceTexture mPreviewTexture;

    /**
     * if you override these in ctor of subclass, loader will ignore the files listed above
     */
    protected String vertexShaderCode;

    protected String fragmentShaderCode;



    /**
     * Basic mesh rendering code
     */
    private static float squareSize = 1.00f;

    private static float squareCoords[] = {
            -squareSize, squareSize, // 0.0f,     // top left
            squareSize, squareSize, // 0.0f,   // top right
            -squareSize, -squareSize, // 0.0f,   // bottom left
            squareSize, -squareSize, // 0.0f,   // bottom right
    };
    private static float leftHalfSquareCoords[] = {
            -squareSize, squareSize, // 0.0f,     // top left
            0.0f, squareSize, // 0.0f,   // top right
            -squareSize, -squareSize, // 0.0f,   // bottom left
            0.0f, -squareSize, // 0.0f,   // bottom right
    };
    private static float rightHalfSquareCoords[] = {
            0.0f, squareSize, // 0.0f,     // top left
            1.0f, squareSize, // 0.0f,   // top right
            0.0f, -squareSize, // 0.0f,   // bottom left
            1.0f, -squareSize, // 0.0f,   // bottom right
    };
    private static short drawOrder[] = {0, 1, 2, 1, 3, 2};


    private static float arrowRecTopY  = 0.5f;
    private static float arrowRecWidth = 0.5f;
    private static float arrowTopWidth = 0.75f;
    private static float arrowCoords[] = {
        -arrowRecWidth, -1.0f, // rect bottom left
         arrowRecWidth, -1.0f, // rect bottom right
        -arrowRecWidth, arrowRecTopY, // rect top left
         arrowRecWidth, arrowRecTopY, // rect top right
        -arrowTopWidth, arrowRecTopY, // left head
                  0.0f,  1.0f, // top head
        arrowTopWidth, arrowRecTopY, // right head
    };
    private static short arrowDrawOrder[] = {0, 1, 2, 1, 3, 2, 4, 5, 6};


    private FloatBuffer textureBuffer;
    private FloatBuffer mapTextureBuffer;

    private float textureCoords[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private float mapTextureCoords[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

    protected int mCameraShaderProgram;

    private FloatBuffer vertexBuffer;
    private FloatBuffer leftHalfVertexBuffer;
    private FloatBuffer rightHalfVertexBuffer;
    private FloatBuffer arrowVertexBuffer;

    private ShortBuffer arrowDrawListBuffer;
    private ShortBuffer drawListBuffer;

    private int textureCoordinateHandle;

    private int positionHandle;

    /**
     * "arbitrary" maximum number of textures. seems that most phones dont like more than 16
     */
    public static final int MAX_TEXTURES = 16;

    /**
     * for storing all texture ids from genTextures, and used when binding
     * after genTextures, id[0] is reserved for camera texture
     */
    private int[] mTexturesIds = new int[MAX_TEXTURES];

    /**
     * array of proper constants for use in creation,
     * updating, and drawing. most phones max out at 16
     * same number as {@link #MAX_TEXTURES}
     *
     * Used in our implementation of {@link #addTexture(Bitmap, String)}
     */
    private int[] mTextureConsts = {
            GLES31.GL_TEXTURE1,
            GLES31.GL_TEXTURE2,
            GLES31.GL_TEXTURE3,
            GLES31.GL_TEXTURE4,
            GLES31.GL_TEXTURE5,
            GLES31.GL_TEXTURE6,
            GLES31.GL_TEXTURE7,
            GLES31.GL_TEXTURE8,
            GLES31.GL_TEXTURE9,
            GLES31.GL_TEXTURE10,
            GLES31.GL_TEXTURE11,
            GLES31.GL_TEXTURE12,
            GLES31.GL_TEXTURE13,
            GLES31.GL_TEXTURE14,
            GLES31.GL_TEXTURE15,
            GLES31.GL_TEXTURE16,
    };

    /**
     * array of {@link Texture} objects used for looping through
     * during the render pass. created in {@link #addTexture(int, Bitmap, String, boolean)}
     * and looped in {@link #setExtraTextures()}
     */
    private ArrayList<Texture> mTextureArray;


    /**
     * matrix for transforming our camera texture, available immediately after {@link #mPreviewTexture}s
     * {@code updateTexImage()} is called in our main {@link #draw()} loop.
     */
    private float[] mCameraTransformMatrix = new float[16];

    /**
     * Handler for communcation with the UI thread. Implementation below at
     * {@link com.androidexperiments.MorphologyTests.gl.CameraRenderer.RenderHandler RenderHandler}
     */
    private RenderHandler mHandler;

    /**
     * Interface listener for some callbacks to the UI thread when rendering is setup and finished.
     */
    private OnRendererReadyListener mOnRendererReadyListener;

    /**
     * Width and height storage of our viewport size, so we can properly accomodate any size View
     * used to display our preview on screen.
     */
    private int mViewportWidth, mViewportHeight;

    /**
     * boolean for recording so we cans wap the recording buffer into place
     */
    private boolean mIsRecording = false;

    /**
     * Reference to our users CameraFragment to ease setting viewport size. Thought about decoupling but wasn't
     * worth the listener/callback hastle
     */
    private CameraFragment mCameraFragment;

    /**
     * Default {@link MediaRecorder} instance so we can record all the cool shit we make. You can override this,
     * but make sure you handle the deletion of temp files yourself.
     */
    private MediaRecorder mMediaRecorder;

    /**
     * Bitrate of our recorded video passed to our default {@link MediaRecorder}
     */
    private static final int VIDEO_BIT_RATE = 10000000;

    private static final int VIDEO_WIDTH = 1920;

    /**
     * Height of our recorded video - notice that if we use {@link com.androidexperiments.MorphologyTests.view.SquareTextureView} that
     * we can pss in the same value as the width here to make sure we render out a square movie. Otherwise, it will stretch the square
     * textureview preview into a fullsize video. Play with the numbers here and the size of the TextureView you use to see the different types
     * of output depending on scale values
     */
    private static final int VIDEO_HEIGHT = 1080;

    //private static final int NUM_CODE_ELEMENTS = 3;
    protected int mNumCodeElements = 3;
    private static final int SIZE_OF_CODE_ELEMENTS = 6*4; //4 = sizeof(float)

    /**
     * Array of ints for use with screen orientation hint in our MediaRecorder.
     * See {@link #setupMediaRecorder()} for more info on its usage.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * temp file we write to for recording, then copy to where user wants to save video file
     */
    private File mTempOutputFile;

    /**
     * file passed by user where to save the video upon completion of recording
     */
    private File mOutputFile = null;


    private int mFBO[];

    /**
     * Simple ctor to use default shaders
     */
    public CameraRenderer(Context context, SurfaceTexture texture, CameraFragment cameraFragment, int width, int height)
    {
        init(context, texture, cameraFragment, width, height, DEFAULT_FRAGMENT_SHADER, DEFAULT_VERTEX_SHADER);
    }

    /**
     * Main constructor for passing in shaders to override the default shader.
     * Context, texture, width, and height are passed in automatically by CameraTextureListener
     * @param fragPath the file name of your fragment shader, ex: "lip_service.frag" if it is top-level /assets/ folder. Add subdirectories if needed
     * @param vertPath the file name of your vertex shader, ex: "lip_service.vert" if it is top-level /assets/ folder. Add subdirectories if needed
     */

    private int screenW, screenH;
    public CameraRenderer(Context context, SurfaceTexture texture, CameraFragment cameraFragment, int width, int height, String fragPath, String vertPath)
    {
        init(context, texture, cameraFragment, width, height, fragPath, vertPath);
        Display disp = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point s = new Point();
        disp.getSize(s);
        screenW = s.x;
        screenH = s.y;
    }

    private void init(Context context, SurfaceTexture texture, CameraFragment cameraFragment, int width, int height, String fragPath, String vertPath)
    {
        this.setName(THREAD_NAME);

        this.mContext = context;
        this.mSurfaceTexture = texture;
        this.mCameraFragment = cameraFragment;

        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        this.mSurfaceAspectRatio = (float)width / height;

        mTextureArray = new ArrayList<>();

        setupCameraFragment();
        setupMediaRecorder();
        setViewport(width, height);

        if(fragmentShaderCode == null || vertexShaderCode == null) {
            loadFromShadersFromAssets(fragPath, vertPath);
        }
    }

    private void setupCameraFragment() {
        mCameraFragment.setOnViewportSizeUpdatedListener(new CameraFragment.OnViewportSizeUpdatedListener() {
            @Override
            public void onViewportSizeUpdated(int viewportWidth, int viewportHeight) {
                mViewportWidth = viewportWidth;
                mViewportHeight = viewportHeight;
            }
        });
    }

    private void loadFromShadersFromAssets(String pathToFragment, String pathToVertex)
    {
        try {
            fragmentShaderCode = ShaderUtils.getStringFromFileInAssets(mContext, pathToFragment);
            vertexShaderCode   = ShaderUtils.getStringFromFileInAssets(mContext, pathToVertex);
        }
        catch (IOException e) {
            Log.e(TAG, "loadFromShadersFromAssets() failed. Check paths to assets.\n" + e.getMessage());
        }
    }

    /**
     * In order to properly make use of our awesome camera fragment and its renderers, we want
     * to record the cool shit we do - so lets use the stock {@link MediaRecorder} class to do that.
     * Because, i mean, why would I want to waste a month writing and implementing my own version
     * when this should do it all on its own, right? ...right? :(
     */
    private void setupMediaRecorder() {
        File outputDir = mContext.getCacheDir();
        try {
            mTempOutputFile = File.createTempFile("temp_mov", "mp4", outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Temp file could not be created. Message: " + e.getMessage());
        }

        mMediaRecorder = new MediaRecorder();

        //set the sources
        /**
         * {@link MediaRecorder.AudioSource.CAMCORDER} is nice because on some fancier
         * phones microphones will be aligned towards whatever camera is being used, giving us better
         * directional audio. And if it doesn't have that, it will fallback to the default Microphone.
         */
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

        /**
         * Using {@link MediaRecorder.VideoSource.SURFACE} creates a {@link Surface}
         * for us to use behind the scenes. We then pass this service to our {@link ExampleRenderer}
         * later on for creation of our EGL contexts to render to.
         *
         * {@link MediaRecorder.VideoSource.SURFACE} is also the default for rendering
         * out Camera2 api data without any shader manipulation at all.
         */
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        //set output
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        /**
         * This would eventually be worth making a parameter at each call to {@link #setupMediaRecorder()}
         * so that you can pass in a timestamp or unique file name each time to setup up.
         */
        mMediaRecorder.setOutputFile(mTempOutputFile.getPath());

        /**
         * Media Recorder can be finicky with certain video sizes, so lets make sure we pass it
         * something 'normal' - ie 720p or 1080p. this will create a surface of the same size,
         * which will be used by our renderer for drawing once recording is enabled
         */
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_BIT_RATE);
        mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mMediaRecorder.setVideoFrameRate(30);

        //setup audio
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
       // mMediaRecorder.setAudioEncodingBitRate(44800);

        /**
         * we can determine the rotation and orientation of our screen here for dynamic usage
         * but since we know our app will be portrait only, setting the media recorder to
         * 720x1280 rather than 1280x720 and letting orientation be 0 will keep everything looking normal
         */
        int rotation = ((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation); Log.d(TAG, "orientation: " + orientation);
        mMediaRecorder.setOrientationHint(0);

        try {
            /**
             * There are what seems like an infinite number of ways to fuck up the previous steps,
             * so prepare() will throw an exception if you fail, and hope that stackoverflow can help.
             */
            mMediaRecorder.prepare();
        }
        catch (IOException e) {
            Toast.makeText(mContext, "MediaRecorder failed on prepare()", Toast.LENGTH_LONG).show();
            Log.e(TAG, "MediaRecorder failed on prepare() " + e.getMessage());
        }

        Log.d(TAG, "MediaRecorder surface: " + mMediaRecorder.getSurface() + " isValid: " + mMediaRecorder.getSurface().isValid());
    }

    /**
     * Initialize all necessary components for GLES rendering, creating window surfaces for drawing
     * the preview as well as the surface that will be used by MediaRecorder for recording
     */
    public void initGL() {
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        //create preview surface
        mWindowSurface = new WindowSurface(mEglCore, mSurfaceTexture);
        mWindowSurface.makeCurrent();

        //create recording surface
        mRecordSurface = new WindowSurface(mEglCore, mMediaRecorder.getSurface(), false);

        initGLComponents();
    }

    protected int mCodebookSSBO[] ;
    protected int mMomentSSBO[];
    private final int MOMENT_SSBO_SIZE = 7 * 4; // floats
    protected int mArmWidthSSBO[];
    private final int ARM_WIDTH_SSBO_SIZE = 3000 * 4; // experimentally determined sized * size of float
    protected int mWristCandidateSSBO[];
    private final int WRIST_CAND_SSBO_SIZE = 3000 * 4; // TODO hard to tell from docs if can compress this to 1 bit per bool value
    protected int mFingerCandidateSSBO[];
    private final int FING_POS_SSBO_SIZE = (4 * 4 * 5); //4 32bit vals per potential finger
    protected int mFingerPosSSBO[];
    private final int KMEANS_CENTERS_SSBO_SIZE = (12 * 4 * 9); // 12 things each 4 bytes with 9 centers TODO make this 5?
    protected int mKmeansCentersSSBO[];
    protected int mKmeansLabelsSSBO[];
    protected int mKmeansWeightsSSBO[];
    private final int ONE_VAL_PER_PIX_SSBO_SIZE = (4 * VIDEO_WIDTH * VIDEO_HEIGHT);
    protected int mAngleDebugSSBO[];
    private final int KMEAN_CENTER_COG_SSBO_SIZE = (7 * 4); // floats
    protected int mKmeansGestureCogSSBO[];
    private boolean mHaveReadAngleDebugSSBO = false;

    protected int mMapTexId;

    protected void initGLComponents() {
        onPreSetupGLComponents();

        //setupQuerries();
        setupVertexBuffer();
        setupTextures();
        setupCameraTexture();
        setupShaders();

        initAlternateShaders();
        setupFBO();
        mIntermediateTex = createImmuteTextures(5);

        //setup the SSBO

        mCodebookSSBO = new int[1];
        GLES31.glGenBuffers(1, mCodebookSSBO, 0);
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mCodebookSSBO[0]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, VIDEO_WIDTH *
                        VIDEO_HEIGHT *
                        mNumCodeElements *
                        SIZE_OF_CODE_ELEMENTS,
                null, GLES31.GL_DYNAMIC_DRAW);
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mCodebookSSBO[0]);

        // for moments m01, m10, m00
        mMomentSSBO = createSSBO(MOMENT_SSBO_SIZE);

        // for storing buffer to count non 0 pixels
        mArmWidthSSBO =  createSSBO(ARM_WIDTH_SSBO_SIZE);

        // for outputing where likely wrist positions are
        mWristCandidateSSBO = createSSBO(WRIST_CAND_SSBO_SIZE);

        // for storing accumulation of potential finger point
        mFingerCandidateSSBO = createSSBO(ONE_VAL_PER_PIX_SSBO_SIZE);

        mFingerPosSSBO = createSSBO(FING_POS_SSBO_SIZE);

        mKmeansCentersSSBO = createSSBO(KMEANS_CENTERS_SSBO_SIZE);

        mKmeansLabelsSSBO = createSSBO(ONE_VAL_PER_PIX_SSBO_SIZE);

        mKmeansWeightsSSBO = createSSBO(ONE_VAL_PER_PIX_SSBO_SIZE);

        mKmeansGestureCogSSBO = createSSBO(KMEAN_CENTER_COG_SSBO_SIZE);

        //
        // this is substantialy larger than it needs to but lower bound is tough to estimate
        mAngleDebugSSBO = createSSBO(ONE_VAL_PER_PIX_SSBO_SIZE);

        mMapTexId = GlUtil.loadImageResourceIntoTexture(mContext, R.drawable.uvicmap);

        onSetupComplete();
    }

    protected int[] createSSBO(int ssboSize) {
        int ssboHandle[] = new int[1];
        GLES31.glGenBuffers(1, ssboHandle, 0);
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, ssboHandle[0]);
        GlUtil.checkGlError("bind ssbo");
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, ssboSize, null, GLES31.GL_DYNAMIC_DRAW);
        GlUtil.checkGlError("create ssbo");
        return ssboHandle;
    }

    protected void setupFBO() {
        mFBO = new int[1];
        GLES31.glGenFramebuffers(1, mFBO, 0);
        checkGlError("Check gen FBO");
    }

    private int mIntermediateTex[];

    public int[] createImmuteTextures(int numText) {
        int[] tex = new int[numText];

        GLES31.glGenTextures(numText, tex, 0);
        checkGlError("gen tex");
        for (int i = 0; i < numText; i++) {
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, tex[i]);
            checkGlError("Bind tex");
            GLES31.glTexStorage2D(GLES31.GL_TEXTURE_2D, 1, GLES31.GL_RGBA32F, VIDEO_WIDTH, VIDEO_HEIGHT);
            checkGlError("tex image 2d");
            GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
            checkGlError("tex params");
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0);
            checkGlError("tex unbind");
        }

        return tex;
    }

    public enum FillMethod {
        NO_FILL,
        FILL_NEIGHBOUR,
        FILL_DISTANCE
    }
    protected FillMethod mFillMethod = FillMethod.NO_FILL;


    protected boolean mSaveData = false;

    protected int mThreshProg;
    protected int mYCbCrProg;
    protected int mBaseShaderProg;
    protected int mDoNothingProg;
    protected int mInitCodebookProg;

    protected int mSimpleThresholdProg;
    protected int mSimpleThresholdAllProg;

    protected int mGenCodebookSqrSqrtCompProg;
    protected int mGenCodebookAdditiveCompProg;

    protected int mMomentAccumPorg;
    // TODO there has to be a better way to do this
    protected int mDrawCOGProg;
    protected int mMomentInitProg;
    protected int mMomentCalcProg;

    protected int mCodebookBGSubYCbCrSqrSqrtCompProg;
    protected int mCodebookBGSubYCbCrAdditiveCompProg;
    protected int mCodebookBGSubRGBSqrSqrtCompProg;
    protected int mCodebookBGSubRGBAdditiveCompProg;

    protected int mCodebookDisplay;

    protected int mCodebookMergeCompProg;

    protected int mMaskCompProg;

    protected int mMorphCrossErodeCompProg;
    protected int mMorphCrossDilateCompProg;
    protected int mMorphBoxErodeCompProg;
    protected int mMorphBoxDilateCompProg;
    protected int mMorphDiamondErodeCompProg;
    protected int mMorphDiamondDilateCompProg;

    protected float mArmWidthThreshold = 100.0f;
    protected int mArmWidthInitCompProg;
    protected int mArmWidthAccumCompProg;
    protected int mArmWidthFindMininumCompProg;

    protected float mFillInHolesThresh = 5.0f;
    protected int mFillInHolesNeighbourCompProg;
    protected int mFillInHolesDistanceCompProg;

    protected int mCompProg;
    protected int mDrawCamTexProg;
    protected boolean mReInitializeCodebookBuffer = false;
    protected boolean mMergeCodebook = false;

    protected float mCannyThreshold = 0.1f;
    protected int mCannyConvToGrayCompProg;
    protected int mCannyGaussBlurHorizontalCompProg;
    protected int mCannyGaussBlurVerticalCompProg;
    protected int mCannySobelCompProg;
    protected int mCannyNonMaxSupCompProg;

    protected int mSimpleEdgeCompProg;
    protected int mSimpleEdgeOrthCompProg;
    protected int mContourLeiteCompProg;

    protected int mSimpleEdgeThinningZSCompProg;
    protected int mSimpleEdgeThinningKWKPass1CompProg;
    protected int mSimpleEdgeThinningKWKPass2CompProg;
    protected int mSimpleEdgeThinningAggressiveCompProg;

    protected int mCopyTex1To3CompProg;

    // TODO naming is bad
    protected int mFingertipPreProcCompProg;
    protected int mFingertipPreProcDebugCompProg;
    protected int mFingertipDetectLineCompProg;
    protected int mFingertipDetectBoxCompProg;
    protected int mFingertipDetectMidpointCompProg;
    protected int mFingerNeighbourhoodSize = 100;
    protected float mFingerAngleThreshMax = 75.0f;
    protected float mFingerAngleThreshMin = 10.0f;
    protected int mDrawShortLine = 1;
    protected int mDrawLongLine  = 1;


    protected int mComplimentCompProg;

    protected float mDistThreshMax = 400.0f;
    protected float mDistThreshMin = 175.0f;
    protected int   mDistFilterCompProg;

    protected float mZoomLevel = 1.0f;
    protected boolean mUseZoom = false;
    protected int mZoomProg;

    protected int mFingerCandidateInitCompProg;
    protected int mFingerCandidateAccumCompProg;
    protected int mFingerCandidateThreshCompProg;
    protected int mFingerAccumThresh;
    // TODO naming oh dear god
    //protected float mFingPosDistThresh = 100.0f;
    protected int mFingPosInitCompProg;
    protected int mFingPosFindCompProg;
    protected int mFingPosDispCompProg;
    protected int mFingPosConvolveKernSize = 3;
    protected int mFingPosConvolveSmoothCompProg;
    protected int mFingPosConvolveNonMaxSuppCompProg;
    protected int mFingPosConvolveAreaAddCompProg;

    protected int mKmeansIterations = 5;
    protected float mKmeansDistThresh = 60.0f;
    protected int mNumKCenters = 9;
    protected float mKmeansCenterReposThresh = 50.0f;

    protected int mKmeansCentersInitCompProg;
    protected int mKmeansCentersResetCompProg;
    protected int mKmeansCentersUpdateCompProg;
    protected int mKmeansCentersUpdateFinalizeCompProg;
    protected int mKmeansLabelsInitCompProg;
    protected int mKmeansLabelsUpdateCompProg;
    protected int mKmeansLabelsDisplayCompProg;

    protected int mKmeansCenterUpdateLostMoveMeanCompProg;
    protected int mKmeansCenterUpdateLostCompProg;
    protected int mKmeansCenterUpdateLostFinalizeCompProg;

    protected int mKmeansGestureFindCogCompProg;
    protected int mKmeansGestureDispCogCompProg;

    protected boolean mKmeanResetCenter = true;
    protected boolean mKmeanHaveResetCenterOnce = false;


    protected boolean mRemoveContourBreak = false;

    protected int mClearAngleDebugBuffCompProg;

    protected int mMapProg;
    protected int mArrowProg;

    protected int mChangeBackgroundColourCompProg;
    protected int mZeroTextureCompProg;

    //TODO make this better by using functions holy hell am I a dumb POS
    protected void initAlternateShaders() {
        mThreshProg = mCameraShaderProgram;

        mYCbCrProg        = makeShadProgFromFileName("touchcolor.vert.glsl", "ycbcr.frag.glsl");
        mBaseShaderProg   = makeShadProgFromFileName("touchcolor.vert.glsl", "baseCamShader.frag.glsl");
        mDoNothingProg    = makeShadProgFromFileName("donothing.vert.glsl", "donothing.frag.glsl");
        mZoomProg         = makeShadProgFromFileName("zoom.vert.glsl", "donothing.frag.glsl");

        mArrowProg = makeShadProgFromFileName("arrow.vert.glsl", "arrow.frag.glsl");
        mMapProg   = makeShadProgFromFileName("map.vert.glsl", "map.frag.glsl");

        mSimpleThresholdProg    = makeShadProgFromFileName("touchcolor.vert.glsl", "simpleThreshWhite.frag.glsl");
        mSimpleThresholdAllProg = makeShadProgFromFileName("touchcolor.vert.glsl", "simpleThreshAll.frag.glsl");

        mInitCodebookProg = makeCompProgFromFileName("codebookInit.comp.glsl");
        mGenCodebookSqrSqrtCompProg  = makeCompProgFromFileName("codebookGen.SqrSqrt.comp.glsl");
        mGenCodebookAdditiveCompProg = makeCompProgFromFileName("codebookGen.Additive.comp.glsl");
        mMomentAccumPorg = makeCompProgFromFileName("momentAccum.comp.glsl");
        mDrawCOGProg    = makeCompProgFromFileName("drawCOG.comp.glsl");
        mMomentInitProg = makeCompProgFromFileName("momentInit.comp.glsl");
        mMomentCalcProg = makeCompProgFromFileName("momentCalc.comp.glsl");

        mCodebookMergeCompProg = makeCompProgFromFileName("codebookMerge.comp.glsl");

        mCodebookBGSubYCbCrSqrSqrtCompProg  = makeCompProgFromFileName("codebookBGSub.YCbCrSqrSqrt.comp.glsl");
        mCodebookBGSubYCbCrAdditiveCompProg = makeCompProgFromFileName("codebookBGSub.YCbCrAdditive.comp.glsl");
        mCodebookBGSubRGBSqrSqrtCompProg    = makeCompProgFromFileName("codebookBGSub.RGBSqrSqrt.comp.glsl");
        mCodebookBGSubRGBAdditiveCompProg   = makeCompProgFromFileName("codebookBGSub.RGBAdditive.comp.glsl");

        mMorphCrossErodeCompProg    = makeCompProgFromFileName("morph.cross.erode.comp.glsl");
        mMorphCrossDilateCompProg   = makeCompProgFromFileName("morph.cross.dilate.comp.glsl");
        mMorphBoxErodeCompProg      = makeCompProgFromFileName("morph.box.erode.comp.glsl");
        mMorphBoxDilateCompProg     = makeCompProgFromFileName("morph.box.dilate.comp.glsl");
        mMorphDiamondErodeCompProg = makeCompProgFromFileName("morph.diamond.erode.comp.glsl");
        mMorphDiamondDilateCompProg = makeCompProgFromFileName("morph.diamond.dilate.comp.glsl");
        mMorphCurErodeCompProg = mMorphCrossErodeCompProg;
        mMorphCurDilateCompProg = mMorphCrossDilateCompProg;

        mMaskCompProg = makeCompProgFromFileName("mask.comp.glsl");

        mCodebookDisplay = makeCompProgFromFileName("codebookBGDisplay.comp.glsl");

        mComplimentCompProg = makeCompProgFromFileName("compliment.comp.glsl");


        mFillInHolesNeighbourCompProg = makeCompProgFromFileName("fillInHolesNeighbour.comp.glsl");
        mFillInHolesDistanceCompProg = makeCompProgFromFileName("fillInHolesDist.comp.glsl");

        mDrawCamTexProg = makeShadProgFromFileName("touchcolor.vert.glsl", "drawcamtex.frag.glsl");

        mArmWidthInitCompProg        = makeCompProgFromFileName("armWidthInit.comp.glsl");
        mArmWidthAccumCompProg       = makeCompProgFromFileName("armWidthAccum.comp.glsl");
        mArmWidthFindMininumCompProg = makeCompProgFromFileName("armWidthFindMin.comp.glsl");


        mCannyConvToGrayCompProg          = makeCompProgFromFileName("canny.convToGrey.comp.glsl");
        mCannyGaussBlurHorizontalCompProg = makeCompProgFromFileName("canny.GaussBlurHorizontal.comp.glsl");
        mCannyGaussBlurVerticalCompProg   = makeCompProgFromFileName("canny.GaussBlurVertical.comp.glsl");
        mCannySobelCompProg               = makeCompProgFromFileName("canny.Sobel.comp.glsl");
        mCannyNonMaxSupCompProg           = makeCompProgFromFileName("canny.NonMaxSup.comp.glsl");

        mSimpleEdgeCompProg         = makeCompProgFromFileName("simpleEdge.comp.glsl");
        mSimpleEdgeOrthCompProg     = makeCompProgFromFileName("simpleEdgeOrth.comp.glsl");
        mContourLeiteCompProg       = makeCompProgFromFileName("contour.Leite.comp.glsl");

        mSimpleEdgeThinningZSCompProg = makeCompProgFromFileName("simpleEdgeThinning.ZhangeSuen.comp.glsl");
        mSimpleEdgeThinningKWKPass1CompProg = makeCompProgFromFileName("simpleEdgeThinning.KwonWoongKang.Pass1.comp.glsl");
        mSimpleEdgeThinningKWKPass2CompProg = makeCompProgFromFileName("simpleEdgeThinning.KwonWoongKang.Pass2.comp.glsl");
        mSimpleEdgeThinningAggressiveCompProg = makeCompProgFromFileName("simpleEdgeThinning.Aggresive.comp.glsl");

        mCopyTex1To3CompProg = makeCompProgFromFileName("copyTex.comp.glsl");

        mFingertipPreProcCompProg        = makeCompProgFromFileName("finger.preProc.comp.glsl");
        mFingertipPreProcDebugCompProg   = makeCompProgFromFileName("finger.preProcDebug.comp.glsl");
        mFingertipDetectLineCompProg     = makeCompProgFromFileName("finger.detect.line.comp.glsl");
        mFingertipDetectMidpointCompProg = makeCompProgFromFileName("finger.detect.midpoint.comp.glsl");
        mFingertipDetectBoxCompProg      = makeCompProgFromFileName("finger.detect.box.comp.glsl");

        mDistFilterCompProg = makeCompProgFromFileName("distanceFilter.comp.glsl");

        mFingerCandidateInitCompProg   = makeCompProgFromFileName("finger.candidate.init.comp.glsl");
        mFingerCandidateAccumCompProg  = makeCompProgFromFileName("finger.candidate.accum.comp.glsl");
        mFingerCandidateThreshCompProg = makeCompProgFromFileName("finger.candidate.thresh.comp.glsl");

        mFingPosInitCompProg     = makeCompProgFromFileName("finger.candidate.findMax.init.comp.glsl");
        mFingPosFindCompProg     = makeCompProgFromFileName("finger.candidate.findMax.comp.glsl");
        mFingPosDispCompProg     = makeCompProgFromFileName("finger.candidate.findMax.disp.comp.glsl");
        mFingPosConvolveSmoothCompProg = makeCompProgFromFileName("finger.candidate.findMax.convolve.comp.glsl");
        mFingPosConvolveNonMaxSuppCompProg = makeCompProgFromFileName("finger.candidate.findMax.nonMaxSupp.comp.glsl");
        mFingPosConvolveAreaAddCompProg = makeCompProgFromFileName("finger.candidate.findMax.convolve.areaAdd.comp.glsl");


        mKmeansCentersInitCompProg           = makeCompProgFromFileName("kmeans.initCenters.comp.glsl");
        mKmeansCentersResetCompProg          = makeCompProgFromFileName("kmeans.resetCenters.comp.glsl");
        mKmeansCentersUpdateCompProg         = makeCompProgFromFileName("kmeans.updateCenters.comp.glsl");
        mKmeansCentersUpdateFinalizeCompProg = makeCompProgFromFileName("kmeans.updateCenters.finalize.comp.glsl");
        mKmeansLabelsInitCompProg            = makeCompProgFromFileName("kmeans.initLabels.comp.glsl");
        mKmeansLabelsUpdateCompProg          = makeCompProgFromFileName("kmeans.updateLabels.comp.glsl");
        mKmeansLabelsDisplayCompProg         = makeCompProgFromFileName("kmeans.displayLabels.comp.glsl");
        mKmeansCenterUpdateLostMoveMeanCompProg = makeCompProgFromFileName("kmeans.updateLostCenter.moveMeanOnCoG.comp.glsl");
        mKmeansCenterUpdateLostCompProg         = makeCompProgFromFileName("kmeans.updateLostCenter.comp.glsl");
        mKmeansCenterUpdateLostFinalizeCompProg = makeCompProgFromFileName("kmeans.updateLostCenter.finalize.comp.glsl");

        mKmeansGestureFindCogCompProg = makeCompProgFromFileName("kmeans.gesture.findCentersCOG.comp.glsl");
        mKmeansGestureDispCogCompProg = makeCompProgFromFileName("kmeans.gesture.displayCOG.comp.glsl");

        mClearAngleDebugBuffCompProg = makeCompProgFromFileName("clearAngleDebugBuff.comp.glsl");

        mChangeBackgroundColourCompProg = makeCompProgFromFileName("changeBackground.comp.glsl");

        mZeroTextureCompProg = makeCompProgFromFileName("zeroTexture.comp.glsl");

        //default to draw bg
        mCameraShaderProgram = mDrawCamTexProg;
        mCompProg = mCodebookBGSubYCbCrSqrSqrtCompProg;
    }

    private int makeCompProgFromFileName(String path) {
        String source = null;
        try {
            source = ShaderUtils.getStringFromFileInAssets(mContext, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (source == null)
            throw new IllegalStateException("Error no file exists for path " + path);

        int id;
        try {
            id = GlUtil.createComputeProgram(source);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Error compiling " + path + "\n\n" + e.getMessage());
        }
        return id;
    }

    private int makeShadProgFromFileName(String vertPath, String fragPath) {
        String vertSource = null;
        String fragSource = null;
        try {
            vertSource = ShaderUtils.getStringFromFileInAssets(mContext, vertPath);
            fragSource = ShaderUtils.getStringFromFileInAssets(mContext, fragPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return GlUtil.createProgram(vertSource, fragSource);
    }

    // ------------------------------------------------------------
    // deinit
    // ------------------------------------------------------------

    public void deinitGL() {
        deinitGLComponents();

        mWindowSurface.release();
        mRecordSurface.release();

        mEglCore.release();

        if(mMediaRecorder != null)
            mMediaRecorder.release();
    }

    protected void deinitGLComponents() {
        GLES31.glDeleteTextures(MAX_TEXTURES, mTexturesIds, 0);
        GLES31.glDeleteProgram(mCameraShaderProgram);

        mPreviewTexture.release();
        mPreviewTexture.setOnFrameAvailableListener(null);
    }

    // ------------------------------------------------------------
    // setup
    // ------------------------------------------------------------

    /**
     * override this method if there's anything else u want to accomplish before
     * the main camera setup gets underway
     */
    private void onPreSetupGLComponents() {

    }

    private int[] mQueryIDs;
    private static int NUM_QUERY_IDS = 20;
    private void setupQuerries() {
        mQueryIDs = new int[NUM_QUERY_IDS];
        GLES31.glGenQueries(NUM_QUERY_IDS, mQueryIDs, 0);
    }

    protected void setupVertexBuffer() {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // for the arrow
        ByteBuffer abb = ByteBuffer.allocateDirect(arrowCoords.length * 4);
        abb.order(ByteOrder.nativeOrder());
        arrowVertexBuffer = abb.asFloatBuffer();
        arrowVertexBuffer.put(arrowCoords);
        arrowVertexBuffer.position(0);
        ByteBuffer adb = ByteBuffer.allocateDirect(arrowDrawOrder.length * 4);
        adb.order(ByteOrder.nativeOrder());
        arrowDrawListBuffer = adb.asShortBuffer();
        arrowDrawListBuffer.put(arrowDrawOrder);
        arrowDrawListBuffer.position(0);

        // for the map
        ByteBuffer mtbb = ByteBuffer.allocateDirect(mapTextureCoords.length * 4);
        mtbb.order(ByteOrder.nativeOrder());
        mapTextureBuffer = mtbb.asFloatBuffer();
        mapTextureBuffer.put(mapTextureCoords);
        mapTextureBuffer.position(0);

        ByteBuffer lhvbb = ByteBuffer.allocateDirect(leftHalfSquareCoords.length * 4);
        lhvbb.order(ByteOrder.nativeOrder());
        leftHalfVertexBuffer = lhvbb.asFloatBuffer();
        leftHalfVertexBuffer.put(leftHalfSquareCoords);
        leftHalfVertexBuffer.position(0);

        ByteBuffer rhvbb = ByteBuffer.allocateDirect(rightHalfSquareCoords.length * 4);
        rhvbb.order(ByteOrder.nativeOrder());
        rightHalfVertexBuffer = rhvbb.asFloatBuffer();
        rightHalfVertexBuffer.put(rightHalfSquareCoords);
        rightHalfVertexBuffer.position(0);
    }

    protected void setupTextures()
    {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the max amount texture ids
        GLES31.glGenTextures(MAX_TEXTURES, mTexturesIds, 0);
        checkGlError("Texture generate");
    }

    /**
     * Remember that Android's camera api returns camera texture not as {@link GLES31#GL_TEXTURE_2D}
     * but rather as {@link GLES11Ext#GL_TEXTURE_EXTERNAL_OES}, which we bind here
     */
    protected void setupCameraTexture() {
        //set texture[0] to camera texture
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexturesIds[0]);
        checkGlError("Texture bind");

        mPreviewTexture = new SurfaceTexture(mTexturesIds[0]);
        mPreviewTexture.setOnFrameAvailableListener(this);
    }

    /**
     * Handling this manually here but check out another impl at {@link GlUtil#createProgram(String, String)}
     */
    protected void setupShaders() {
        int vertexShaderHandle = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        GLES31.glShaderSource(vertexShaderHandle, vertexShaderCode);
        GLES31.glCompileShader(vertexShaderHandle);
        checkGlError("Vertex shader compile");

        Log.d(TAG, "vertexShader info log:\n " + GLES31.glGetShaderInfoLog(vertexShaderHandle));

        int fragmentShaderHandle = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        GLES31.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
        GLES31.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        Log.d(TAG, "fragmentShader info log:\n " + GLES31.glGetShaderInfoLog(fragmentShaderHandle));

        mCameraShaderProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mCameraShaderProgram, vertexShaderHandle);
        GLES31.glAttachShader(mCameraShaderProgram, fragmentShaderHandle);
        GLES31.glLinkProgram(mCameraShaderProgram);
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES31.glGetProgramiv(mCameraShaderProgram, GLES31.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES31.GL_TRUE) {
            String error = GLES31.glGetProgramInfoLog(mCameraShaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
            throw new IllegalStateException("Error while linking program:\n" + error);
        }
    }

    /**
     * called when all setup is complete on basic GL stuffs
     * override for adding textures and other shaders and make sure to call
     * super so that we can let them know we're done
     */
    protected void onSetupComplete() {
        mOnRendererReadyListener.onRendererReady();
    }

    @Override
    public synchronized void start() {
        if(mOnRendererReadyListener == null)
            throw new RuntimeException("OnRenderReadyListener is not set! Set listener prior to calling start()");

        super.start();
    }


    /**
     * primary loop - this does all the good things
     */
    @Override
    public void run()
    {
        Looper.prepare();

        //create handler for communication from UI
        mHandler = new RenderHandler(this);

        //initialize all GL on this context
        initGL();

        //LOOOOOOOOOOOOOOOOP
        Looper.loop();

        //we're done here
        deinitGL();

        mOnRendererReadyListener.onRendererFinished();
    }

    /**
     * stop our thread, and make sure we kill a recording if its still happening
     *
     * this should only be called from our handler to ensure thread-safe
     */
    public void shutdown() {
        synchronized (this) {
            if (mIsRecording)
                stopRecording();
            else //not recording but still needs release
                mMediaRecorder.release();
        }

        //kill ouy thread
        Looper.myLooper().quit();
    }


    private long mLastDrawCallTime = 0;

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        boolean swapResult;

        synchronized (this)
        {
            updatePreviewTexture();

            if(mEglCore.getGlVersion() >= 3)
            {

                long curTime = (System.nanoTime() / 1000) / 1000; //ms
                //Log.v(TAG, String.format("Time since last draw call: %05d ms", (curTime - mLastDrawCallTime)));
                mLastDrawCallTime = curTime;
                draw();


                if(mIsRecording) {
                    mRecordSurface.makeCurrentReadFrom(mWindowSurface);

                    GlUtil.checkGlError("before glBlitFramebuffer");

                    GLES31.glBlitFramebuffer(
                            0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight(),
                            0, 0, mRecordSurface.getWidth(), mRecordSurface.getHeight(), //must match the mediarecorder surface size
                            GLES31.GL_COLOR_BUFFER_BIT, GLES31.GL_NEAREST
                    );

                    int err;
                    if ((err = GLES31.glGetError()) != GLES31.GL_NO_ERROR)
                        Log.w(TAG, "ERROR: glBlitFramebuffer failed: 0x" + Integer.toHexString(err));

                    mRecordSurface.setPresentationTime(surfaceTexture.getTimestamp());
                    mRecordSurface.swapBuffers();
                }

                //swap main buff
                mWindowSurface.makeCurrent();
                swapResult = mWindowSurface.swapBuffers();
            }
            else //gl v2
            {
                draw();

                if(mIsRecording) {
                    // Draw for recording, swap.
                    mRecordSurface.makeCurrent();

                    setViewport(mRecordSurface.getWidth(), mRecordSurface.getHeight());
                    draw();

                    mRecordSurface.setPresentationTime(surfaceTexture.getTimestamp());
                    mRecordSurface.swapBuffers();

                    setViewport(mWindowSurface.getWidth(), mWindowSurface.getHeight());
                }

                mWindowSurface.makeCurrent();
                swapResult = mWindowSurface.swapBuffers();
            }

            if (!swapResult) {
                // This can happen if the Activity stops without waiting for us to halt.
                Log.e(TAG, "swapBuffers failed, killing renderer thread");
                shutdown();
            }
        }
    }

    protected void morphLaunch(int input, int output) {
        GLES31.glBindImageTexture(0, input, 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA32F);
        GlUtil.checkGlError("setup input");
        GLES31.glBindImageTexture(1, output, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
        GlUtil.checkGlError("setup output");
        GLES31.glDispatchCompute(VIDEO_WIDTH / 8, VIDEO_HEIGHT / 8, 1);
        GlUtil.checkGlError("Dispatch");
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        GlUtil.checkGlError("Mem barrior");
    }
    // erode -> dilate
    private void morphCrossOpen(int inputOutputMask, int intermediateMask) {
        GLES31.glUseProgram(mMorphCrossErodeCompProg);
        morphLaunch(inputOutputMask, intermediateMask);

        GLES31.glUseProgram(mMorphCrossDilateCompProg);
        morphLaunch(intermediateMask, inputOutputMask);
    }

    // dilate -> erode
    private void morphCrossClose(int inputOutputMask, int intermediateMask) {
        GLES31.glUseProgram(mMorphCrossDilateCompProg);
        morphLaunch(inputOutputMask, intermediateMask);

        GLES31.glUseProgram(mMorphCrossErodeCompProg);
        morphLaunch(intermediateMask, inputOutputMask);
    }

    protected void setUniformsCompProg() {
        GLES31.glUseProgram(mCompProg);
    }

    protected void reInintializeCodeBookBuffer() {
        GLES31.glDeleteBuffers(1, mCodebookSSBO, 0);
        GlUtil.checkGlError("Delete buffer");
        GLES31.glGenBuffers(1, mCodebookSSBO, 0);
        GlUtil.checkGlError("Gen buffer");
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mCodebookSSBO[0]);
        GlUtil.checkGlError("Bind buffer");
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, VIDEO_WIDTH *
                        VIDEO_HEIGHT *
                        mNumCodeElements *
                        SIZE_OF_CODE_ELEMENTS,
                null, GLES31.GL_DYNAMIC_DRAW);
        GlUtil.checkGlError("Buffer data");

        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mCodebookSSBO[0]);
        GlUtil.checkGlError("bind buffer base");
        GLES31.glUseProgram(mInitCodebookProg);
        GlUtil.checkGlError("Set init codebook prog");
        GLES31.glDispatchCompute(VIDEO_WIDTH / 8, VIDEO_HEIGHT / 8, 1);
        GlUtil.checkGlError("Dispatch");
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        GlUtil.checkGlError("Mem barrior");
    }

    private void mergeCodebook() {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mCodebookSSBO[0]);
        GlUtil.checkGlError("Bind buffer");
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mCodebookSSBO[0]);
        GlUtil.checkGlError("bind buffer base");
        GLES31.glUseProgram(mCodebookMergeCompProg);
        GlUtil.checkGlError("Set init codebook prog");
        GLES31.glDispatchCompute(VIDEO_WIDTH / 8, VIDEO_HEIGHT / 8, 1);
        GlUtil.checkGlError("Dispatch");
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        GlUtil.checkGlError("Mem barrior");
    }

    //TODO pretty hacky
    protected int mMorphCurErodeCompProg;
    protected int mMorphCurDilateCompProg;
    protected int mMorphNumSteps = 0;
    protected void setupMorphology() {

    }
    protected void performMorphology(int inputMaskTexIdx, int scratchInputMaskTexIdx) {

    }

    protected FingerEdgeDetectionStep mFingerDetectionState = FingerEdgeDetectionStep.OFF;
    public enum FingerEdgeDetectionStep {
        OFF,
        CANNY,
        SIMPLE_EDGE,
        SIMPLE_EDGE_THIN,
        SIMPLE_EDGE_STEP,
        SIMPLE_EDGE_DEBUG,
        FINGER_DETECT_LINE,
        FINGER_DETECT_BOX,
        FINGER_DETECT_MIDPOINT,
        FINGER_DETECT_ACCUM_DISPLAY,
        FINGER_DETECT_ACCUM_CONVOLVE,
        FINGER_DETECT_ACCUM_NONMAXSUPP,
        FINGER_DETECT_ACCUM_AREA_ADD,
        FINGER_DETECT_KMEANS
    }
    protected ContourGenerationAlg mContourGenerationAlg = ContourGenerationAlg.SIMPLE;
    public enum ContourGenerationAlg {
        SIMPLE,
        SIMPLE_ORTH,
        LEITE,
    }
    protected ContourThinningAlg mContourThinningAlg = ContourThinningAlg.KWK;
    public enum ContourThinningAlg {
        OFF,
        KWK,
        ZS,
        KWK_THIRD_PASS_ONLY,
    }

    // TODO input is input; output is scratch (decided by best method - simple)
    private void contourGeneration(int texIDInput, int texIDScratch, int texIDScratch2) {
        GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
        GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
        switch (mContourGenerationAlg) {
            case SIMPLE:
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeCompProg);
            endGLTimer("Contour Generation");
                //GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                //GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                //launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
                break;
            case SIMPLE_ORTH:
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeOrthCompProg);
                //GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                //GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                //launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
                break;
            case LEITE:
                GLES31.glBindImageTexture(0, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glUseProgram(mMorphCrossDilateCompProg);
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mMorphCrossDilateCompProg, "structuringElementSize"), 3);
                launchCompProgWithDefaultWorkGroupSize(mMorphCrossDilateCompProg);
                // texIDScratch now contains texIDInput dilated

                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDScratch2, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mComplimentCompProg);
                // scratch2 now contains compliment

                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(4, texIDScratch2, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mContourLeiteCompProg);
                // TODO nightmare fuel
                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
                break;
            default:
                throw new IllegalStateException("Unknown contour generation alg: " + mContourGenerationAlg);
        }
    }

    // TODO these are awful
    //TODO input is scratch; output is input (kill me now)
    private void contourThinning(int texIDInput, int texIDScratch) {
        switch (mContourThinningAlg) {
            case KWK:
                // pass 1 sub iteration 1
                GLES31.glUseProgram(mSimpleEdgeThinningKWKPass1CompProg);
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mSimpleEdgeThinningKWKPass1CompProg, "iteration"), 0);
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeThinningKWKPass1CompProg);
                // pass 1 sub iteration 2
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mSimpleEdgeThinningKWKPass1CompProg, "iteration"), 1);
                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeThinningKWKPass1CompProg);
            case KWK_THIRD_PASS_ONLY: // Intentional fall through
                // pass 2
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeThinningKWKPass2CompProg);
            endGLTimer("Contour Thin KWK3");
                break;
            case ZS:
                GLES31.glUseProgram(mSimpleEdgeThinningZSCompProg);
                // sub it 1
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mSimpleEdgeThinningZSCompProg, "iteration"), 0);
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeThinningZSCompProg);
                // sub it 2
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mSimpleEdgeThinningZSCompProg, "iteration"), 1);
                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mSimpleEdgeThinningZSCompProg);
                //TODO I become spaghetti, destroyer of coherence (only case with even number of iterations so need an extra copy)
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
                break;
            case OFF:
                break;
        }
        //GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
        //GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
        //launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
    }

    // TODO OPTIMIZE rewrite this to return which texture the output is in big money gains without losing debug stuff
    private int fingerDetection(int texIDInput, int texIDScratch, int texIDScratch2) {
        // Create a simple edge from mask
            // input is input; scratch is output
            contourGeneration(texIDInput, texIDScratch, texIDScratch2);
        if (mFingerDetectionState == FingerEdgeDetectionStep.SIMPLE_EDGE) return texIDScratch;

        // skip leite?
        // Thinning
            //input is scratch; output is input
            // these work by removing pixels from input but not possible to do in place so output needs to be copy of input
            startGLTimer();
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
            endGLTimer("Contour copy");
            contourThinning(texIDInput, texIDScratch);

        if (mFingerDetectionState == FingerEdgeDetectionStep.SIMPLE_EDGE_THIN) return texIDInput;

        // do preprocessing for finger detection
            // do we want to do the debugging?
            int fingPreProcCompProg = (mFingerDetectionState == FingerEdgeDetectionStep.SIMPLE_EDGE_DEBUG)?
                    mFingertipPreProcDebugCompProg
                    : //if not use normal shader
                    mFingertipPreProcCompProg;


        startGLTimer();
            GLES31.glUseProgram(fingPreProcCompProg);
            int truthVal = mRemoveContourBreak ? 1 : 0;
            GLES31.glUniform1i(GLES31.glGetUniformLocation(fingPreProcCompProg, "removeContourBreaks"), truthVal);

            GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            launchCompProgWithDefaultWorkGroupSize(fingPreProcCompProg);
        endGLTimer("Fingertip DetectPreProc");
        if (mFingerDetectionState == FingerEdgeDetectionStep.SIMPLE_EDGE_STEP || mFingerDetectionState == FingerEdgeDetectionStep.SIMPLE_EDGE_DEBUG) return texIDScratch;

        //TODO add another state, button and early return
        //TODO can probably add the first step to this but its a bit messy for now probs should just do if else
        //Finger tip detection
        if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_LINE
                || mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_BOX
                || mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_KMEANS
                ) {

            //GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mAngleDebugSSBO[0]);
            //GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 7, mAngleDebugSSBO[0]);
            //launchCompProgWithDefaultWorkGroupSize(mClearAngleDebugBuffCompProg);

            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mMomentSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 5, mMomentSSBO[0]);

            int lineDetectProg;
            lineDetectProg = (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_BOX  )?//  ||
                              //mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_KMEANS) ?
                    mFingertipDetectBoxCompProg
                    : // by this point there are no other states so know to use line prog
                    mFingertipDetectLineCompProg;

            // init the kmeans labels
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansLabelsSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mKmeansLabelsSSBO[0]);
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansWeightsSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 4, mKmeansWeightsSSBO[0]);
    startGLTimer();
            launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsInitCompProg);
    endGLTimer("Kmeans init labels");
            //bind the remaining needed buffers
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansCentersSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, mKmeansCentersSSBO[0]);

    startGLTimer();
            GLES31.glUseProgram(lineDetectProg);
            GLES31.glUniform1i(GLES31.glGetUniformLocation(lineDetectProg, "neighbourhoodSize"), mFingerNeighbourhoodSize);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "angleThreshMax"), mFingerAngleThreshMax);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "angleThreshMin"), mFingerAngleThreshMin);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "drawShortLine"), mDrawShortLine);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "drawLongLine"), mDrawLongLine);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "distThreshMin"), mDistThreshMin);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(lineDetectProg, "kMeansDistThresh"), mKmeansDistThresh);
            GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
    endGLTimer("Fingertip DetectionSetParams");
            launchCompProgWithDefaultWorkGroupSize(lineDetectProg);
    endGLTimer("Fingertip Detection ");

            GlUtil.checkGlError("line detect prog");

            if (
                    mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_BOX ||
                    mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_LINE
                ) {
                if (mSaveData) {
                    writeSSBOToFile(mAngleDebugSSBO[0], ONE_VAL_PER_PIX_SSBO_SIZE, "angleList");
                }
                return texIDInput;
            }

            //TODO this is somewhat annoying needing to set numCenters for each step is there a way to set it once for each shader
            //TODO otherwise could just make a kmeansProg func seems like a waste for 2 lines tho and not needed for every step
            // init the kmeans centers based off cog / dist thresh
                GLES31.glUseProgram(mKmeansCentersInitCompProg);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mKmeansCentersInitCompProg, "distThresh"), mDistThreshMax);
                GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mMomentSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 5, mMomentSSBO[0]);
                if (mKmeanResetCenter) {
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mKmeansCentersInitCompProg);
                endGLTimer("Kmeans init centers");
                } else if (!mKmeanHaveResetCenterOnce) {
                    mKmeanHaveResetCenterOnce = true;
                    launchCompProgWithDefaultWorkGroupSize(mKmeansCentersInitCompProg);
                }

            //if(!mHaveWrittenKmeansToFile) {
            //    mHaveWrittenKmeansToFile = true;
            /*
            if (mSaveData) {
                writeSSBOToFile(mKmeansCentersSSBO[0], KMEANS_CENTERS_SSBO_SIZE, "Kmeans Centers");
            }
            */

            for (int i = 0; i<mKmeansIterations; ++i) {
                 // update points based on min distance to each center
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsUpdateCompProg);
                endGLTimer("Kmeans label update");

                // clear kmeans to zero
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mKmeansCentersResetCompProg);
                endGLTimer("Kmeans center rest");

                // re update centers (no sem needed just atomicAdd)
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mKmeansCentersUpdateCompProg);
                endGLTimer("Kmeans center update");

                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mKmeansCentersUpdateFinalizeCompProg);
                endGLTimer("Kmeans center up finalize");
            }

            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mKmeansCenterUpdateLostMoveMeanCompProg);
            endGLTimer("Move lost means");
            // correct lost points
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mKmeansCenterUpdateLostCompProg);
            endGLTimer("Kmeans lost centers");
                GLES31.glUseProgram(mKmeansCenterUpdateLostFinalizeCompProg);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mKmeansCenterUpdateLostFinalizeCompProg, "centerReposThresh"), mKmeansCenterReposThresh);
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mKmeansCenterUpdateLostFinalizeCompProg);
            endGLTimer("Kmeans lost center finalize");

            // display (maybe different colours for each cluster? could draw pts or just singular kmeans)
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsDisplayCompProg);
            endGLTimer("Draw kmeans centers");

            if (mSaveData) {
                writeSSBOToFile(mKmeansCentersSSBO[0], KMEANS_CENTERS_SSBO_SIZE, "Kmeans Centers");
            }
            //test read every frame
            /*
                GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansCentersSSBO[0]);
                ByteBuffer buf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, KMEANS_CENTERS_SSBO_SIZE, GLES31.GL_MAP_READ_BIT);
                GlUtil.checkGlError("Map byte buf");
                buf.order(ByteOrder.nativeOrder());
                FloatBuffer fbuf =  buf.asFloatBuffer();
                Log.d(TAG, "Test output center co-ord " + fbuf.get() + " " + fbuf.get());
                GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
            */
            gestureRecognition(texIDInput);

            return texIDInput;
        }

        if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_DISPLAY  ||
            mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_CONVOLVE ||
            mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_NONMAXSUPP ||
            mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_AREA_ADD) {
            // re init floats that will be accumulated
            GLES31.glUseProgram(mFingerCandidateInitCompProg);
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mFingerCandidateSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mFingerCandidateSSBO[0]);
            launchCompProgWithDefaultWorkGroupSize(mFingerCandidateInitCompProg);
            // accumulate the lines
            GLES31.glUseProgram(mFingerCandidateAccumCompProg);
            GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glUniform1i(GLES31.glGetUniformLocation(mFingerCandidateAccumCompProg, "neighbourhoodSize"), mFingerNeighbourhoodSize);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(mFingerCandidateAccumCompProg, "angleThreshMax"), mFingerAngleThreshMax);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(mFingerCandidateAccumCompProg, "angleThreshMin"), mFingerAngleThreshMin);
            launchCompProgWithDefaultWorkGroupSize(mFingerCandidateAccumCompProg);
            //re init pos buffers;
            GLES31.glUseProgram(mFingPosInitCompProg);
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mFingerPosSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, mFingerPosSSBO[0]);
            launchCompProgWithDefaultWorkGroupSize(mFingPosInitCompProg);

            String accumFilePost = "";

            if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_CONVOLVE) {
                GLES31.glUseProgram(mFingPosConvolveSmoothCompProg);
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mFingPosConvolveSmoothCompProg, "kernelSize"), mFingPosConvolveKernSize);
                launchCompProgWithDefaultWorkGroupSize(mFingPosConvolveSmoothCompProg);
                accumFilePost = "Convolve";
            } else if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_NONMAXSUPP) {
                GLES31.glUseProgram(mFingPosConvolveNonMaxSuppCompProg);
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mFingPosConvolveNonMaxSuppCompProg, "kernelSize"), mFingPosConvolveKernSize);
                launchCompProgWithDefaultWorkGroupSize(mFingPosConvolveNonMaxSuppCompProg);
                accumFilePost = "NonMaximalSupression";
            } else if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_ACCUM_AREA_ADD) {
                GLES31.glUseProgram(mFingPosConvolveAreaAddCompProg);
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mFingPosConvolveAreaAddCompProg, "kernelSize"), mFingPosConvolveKernSize);
                launchCompProgWithDefaultWorkGroupSize(mFingPosConvolveAreaAddCompProg);
                accumFilePost = "AreaAdd";
            }

            // find the max
            launchCompProgWithDefaultWorkGroupSize(mFingPosFindCompProg);
            // draw the max

            //TODO make this a button if display
            launchCompProgWithDefaultWorkGroupSize(mFingPosDispCompProg);

            //TODO is this neeeded I unno
            GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);

            if (mSaveData) {
                writeSSBOToFile(mFingerCandidateSSBO[0], ONE_VAL_PER_PIX_SSBO_SIZE, "Accum" + accumFilePost);
            }
        }

        if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_MIDPOINT
                //|| mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_KMEANS
                ) {

            GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);

            // while this is not needed if only drawing mid points,
            // it must come before midpoint which set initial kmeans labels
                // init the kmeans labels
                GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansLabelsSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mKmeansLabelsSSBO[0]);

                GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansWeightsSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 4, mKmeansWeightsSSBO[0]);
                launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsInitCompProg);

            // find the mid points
                GLES31.glUseProgram(mFingertipDetectMidpointCompProg);
            // set input tex
            // TODO REMOVE ?
                GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                // setup parameters
                GLES31.glUniform1i(GLES31.glGetUniformLocation(mFingertipDetectMidpointCompProg, "neighbourhoodSize"), mFingerNeighbourhoodSize);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mFingertipDetectMidpointCompProg, "angleThreshMax"), mFingerAngleThreshMax);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mFingertipDetectMidpointCompProg, "angleThreshMin"), mFingerAngleThreshMin);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mFingertipDetectMidpointCompProg, "kMeansDistThresh"), mKmeansDistThresh);
            // setup input output text
                GLES31.glBindImageTexture(1, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);

            // bind the kmeans buffers
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansCentersSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, mKmeansCentersSSBO[0]);

                // ready to go
                launchCompProgWithDefaultWorkGroupSize(mFingertipDetectMidpointCompProg);

            // if we're only displaying midpoints, we're done
            if (mFingerDetectionState == FingerEdgeDetectionStep.FINGER_DETECT_MIDPOINT) return texIDInput;


            // init the kmeans centers based off cog / dist thresh
                GLES31.glUseProgram(mKmeansCentersInitCompProg);
                GLES31.glUniform1f(GLES31.glGetUniformLocation(mKmeansCentersInitCompProg, "distThresh"), mDistThreshMax);
                GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mMomentSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 5, mMomentSSBO[0]);
                launchCompProgWithDefaultWorkGroupSize(mKmeansCentersInitCompProg);
            for (int i = 0; i<mKmeansIterations; ++i) {
                // update points based on min distance to each center
                launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsUpdateCompProg);
                // clear kmeans to zero
                launchCompProgWithDefaultWorkGroupSize(mKmeansCentersResetCompProg);
                // re update centers (no sem needed just atomicAdd)
                launchCompProgWithDefaultWorkGroupSize(mKmeansCentersUpdateCompProg);
                launchCompProgWithDefaultWorkGroupSize(mKmeansCentersUpdateFinalizeCompProg);
            }
            // correct lost points
                launchCompProgWithDefaultWorkGroupSize(mKmeansCenterUpdateLostCompProg);
                launchCompProgWithDefaultWorkGroupSize(mKmeansCenterUpdateLostFinalizeCompProg);

            // display (maybe different colours for each cluster? could draw pts or just singular kmeans)
                GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            launchCompProgWithDefaultWorkGroupSize(mKmeansLabelsDisplayCompProg);

            //TODO is this neeeded I unno
            GLES31.glBindImageTexture(1, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            GLES31.glBindImageTexture(3, texIDScratch, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
            launchCompProgWithDefaultWorkGroupSize(mCopyTex1To3CompProg);
        }
        return texIDInput;
    }

    // TODO this is pretty hacky should be a speparat class or at least a sub class
    protected int lastNumActiveCenters = 0;
    private float lastAvgDistToCenters = 0.0f;
    private final float DEF_MAP_SIZE = 1.0f;
    private float sizeOfCOG = 10.0f;
    private float cursorXPos  = 990.0f;
    private float cursorYPos  = 660.0f;
    private float cursorAngle = 0.0f;
    private float cursorSize  = 0.1f;
    private float mapXPos  = 990.0f;
    private float mapYPos  = 540.0f;
    private float mapAngle = 0.0f;
    private float mapSize  = DEF_MAP_SIZE;
    private float lastCenterX = 0.0f;
    private float lastCenterY = 0.0f;
    // expect data to be in SSBOs already
    private void gestureRecognition(int texIDInput) {
        // switch on num active fingers
        //TODO "active" currently is based on minDist maybe should be a decay instead?

        // maybe should draw midpoint then scale it base off of above and then don't change if num active fingers != 2

        //bind needed buffers/iamges
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansCentersSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 2, mKmeansCentersSSBO[0]);
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansGestureCogSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 4, mKmeansGestureCogSSBO[0]);
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mMomentSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 5, mMomentSSBO[0]);
            GLES31.glBindImageTexture(3, texIDInput, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);

        //TODO these are probably not going to be in parallel anyway so should test if transfer to from GPU is worth it
        //first find cog of centers which is used by gestures
        startGLTimer();
            launchCompProgWithDefaultWorkGroupSize(mKmeansGestureFindCogCompProg);
        endGLTimer("Gesture preproc");

        // This code doesn't actually do anything but just put it in to test timing
        /*
    startGLTimer();
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansCentersSSBO[0]);
            ByteBuffer kmeanBBuf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, KMEANS_CENTERS_SSBO_SIZE, GLES31.GL_MAP_READ_BIT | GLES31.GL_MAP_WRITE_BIT);
            GlUtil.checkGlError("test map bvuffer");
            kmeanBBuf.order(ByteOrder.nativeOrder());
            FloatBuffer kmeanFBuf = kmeanBBuf.asFloatBuffer();
            float lastVal = 0.0f;
            while (kmeanFBuf.hasRemaining()) {lastVal = kmeanFBuf.get();}
            kmeanFBuf.position(KMEANS_CENTERS_SSBO_SIZE/4 - 1);
            kmeanFBuf.put(lastVal);
            GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
    endGLTimer("Kmeans test buffer read");
        */

    /*
    startGLTimer();
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansLabelsSSBO[0]);
        ByteBuffer lbuf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, ONE_VAL_PER_PIX_SSBO_SIZE, GLES31.GL_MAP_READ_BIT | GLES31.GL_MAP_WRITE_BIT);
        lbuf.order(ByteOrder.nativeOrder());
        FloatBuffer flbuf =  lbuf.asFloatBuffer();
        int pos = 0;
        float lastVal = 0.0f;
        while(flbuf.hasRemaining()) {
            lastVal = flbuf.get();
            if(lastVal != -1.0f) {
              flbuf.position(pos);
              flbuf.put(lastVal * 2.0f);
            }
            pos++;
        }
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
    endGLTimer("Read labels t" + Float.toString(lastVal) + "buffer");
    */

        // read number of active centers
    startGLTimer();
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mKmeansGestureCogSSBO[0]);
            ByteBuffer buf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, KMEAN_CENTER_COG_SSBO_SIZE, GLES31.GL_MAP_READ_BIT);
            GlUtil.checkGlError("Map byte buf");
            buf.order(ByteOrder.nativeOrder());
            FloatBuffer fbuf =  buf.asFloatBuffer();
            float centerCogX = fbuf.get();
            float centerCogY = fbuf.get();
            int numActiveCenters = (int)fbuf.get();
            float avgDistToCOG = fbuf.get();
            float angle = fbuf.get();
            GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
    endGLTimer("Gesture read kmeans buffer");
        //Log.d(TAG, "Num active centers " + numActiveCenters + " COG " + centerCogX + "x " + centerCogY + "y angle " + angle);

        // check if state changed
            long timePreGesture;
            if (PERFORM_TIMING) {
                timePreGesture = System.nanoTime() / 1000;
            }
            if (lastNumActiveCenters != numActiveCenters) {
                lastAvgDistToCenters = avgDistToCOG;
                lastCenterX = centerCogX;
                lastCenterY = centerCogY;
            } else {
                switch (numActiveCenters) {
                    case 1: // move
                        // if cursor is close to the kmean center cog move cursor with cog
                        //float xDist = Math.abs(centerCogX -  cursorXPos);
                        //float yDist = Math.abs(centerCogY -  cursorYPos);
                        //float dist = (float)Math.sqrt(xDist*xDist + yDist*yDist);
                        //TODO mshould be its own slider YOLO though
                        //if (dist < 500.0f) {
                            cursorXPos = centerCogX;
                            cursorYPos = centerCogY;
                        //}
                        break;
                    case 2: //scale
                        //float sizeDif = avgDistToCOG < lastAvgDistToCenters ? -1.0f : 1.0f;
                        /*
                        cursorSize += 0.01 * sizeDif;
                        cursorSize = cursorSize < 0.02f? 0.02f : cursorSize;
                        cursorSize = cursorSize > 0.30f? 0.30f : cursorSize;
                        lastAvgDistToCenters = avgDistToCOG;
                        */
                        //mapSize -= 0.01 * sizeDif; //bigger is smaller
                        float ratio = lastAvgDistToCenters / avgDistToCOG;
                        //lastAvgDistToCenters = avgDistToCOG;
                        mapSize = ratio * DEF_MAP_SIZE;

                        break;
                    case 3: // scroll
                        float xDif = centerCogX - lastCenterX;
                        float yDif = centerCogY - lastCenterY;
                        //cursorXPos += 2.0f * xDif;
                        //cursorYPos += 2.0f * yDif;
                        mapXPos += xDif;
                        mapYPos += yDif;
                        lastCenterX = centerCogX;
                        lastCenterY = centerCogY;
                        break;
                    case 4: // rotate T
                        //cursorAngle = angle;
                        mapAngle = angle;
                        break;
                    default:
                        break;
                }
            }
        lastNumActiveCenters = numActiveCenters;
        if (PERFORM_TIMING) {
            long timePostGesture = System.nanoTime() / 1000;
            Log.v(TAG, String.format("Gesture us %d", timePostGesture - timePreGesture));
        }


        //display cog of centers
        startGLTimer();
            launchCompProgWithDefaultWorkGroupSize(mKmeansGestureDispCogCompProg);
        endGLTimer("Draw gesture center");

        //TODO put test draw shape here
        // Hopefully can be written to something usable by compute shader should be as zoom works
        // might need to be last thing in main draw but that wouldn't be too much of a problem (can't zoom + this which is fine
    }

    private void writeSSBOToFile(int ssboID, int size, String fileName) {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, ssboID);

        ByteBuffer buf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, size, GLES31.GL_MAP_READ_BIT);
        GlUtil.checkGlError("Map byte buf");
        buf.order(ByteOrder.nativeOrder());
        FloatBuffer fbuf =  buf.asFloatBuffer();
        File fileHandle = new File(Environment.getExternalStorageDirectory(), fileName);
        try {
            FileOutputStream fileStream = new FileOutputStream(fileHandle);
            FileChannel fileChannel = fileStream.getChannel();
            fileChannel.write(buf);
            fileChannel.close();
            fileStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create a write for file " + fileHandle + '\n' +e.toString());
        }

        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
        GlUtil.checkGlError("unMap buffer");

        Toast.makeText(this.mContext, "Saved file " + fileHandle + " with length " + fbuf.capacity(), Toast.LENGTH_LONG).show();
    }

    private void clearShaderStorageBufferOnCpu(int buffer, int bufferSize) {
        fillShaderStorageBufferOnCPU(buffer, bufferSize, 0.0f);
    }

    private void fillShaderStorageBufferOnCPU(int buffer, int bufferSize, float val) {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffer);
        ByteBuffer bbuf = (ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, bufferSize, GLES31.GL_MAP_WRITE_BIT);
        bbuf.order(ByteOrder.nativeOrder());
        FloatBuffer fbuf = bbuf.asFloatBuffer();
        while (fbuf.hasRemaining()) {fbuf.put(val);}
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
    }
    /**
     * main draw routine
     */
    public void draw()
    {
        GLES31.glViewport(0, 0, mViewportWidth, mViewportHeight);

        GLES31.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);

        if (mCompProg != -1 )setUniformsCompProg();
        setUniformsAndAttribs();
        setExtraTextures();

        if (mReInitializeCodebookBuffer) {
            //TODO
            reInintializeCodeBookBuffer();
            mReInitializeCodebookBuffer = false;
        }

        mergeCodebook();

        //set shader
        GLES31.glUseProgram(mCameraShaderProgram);

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mFBO[0]);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, mIntermediateTex[0], 0);
        GlUtil.checkGlError("framebuffer texture 2d");

    startGLTimer();
        drawElements();
    endGLTimer("codebook convert to YCbCr");

        // copy the camera texture to normal textures twice
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mFBO[0]);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, mIntermediateTex[1], 0);
        GlUtil.checkGlError("framebuffer texture 2d");

        drawElements();

        //TODO this is also pretty hacky this shit needs to be refactored real bad I feel bad for doing this
        int morphOutputTexId = mIntermediateTex[0];
        int outputOfFingerDetectionTex = mIntermediateTex[0];

        if (mCompProg != -1) {

            /*compute*/
            GLES31.glUseProgram(mCompProg);
            GlUtil.checkGlError("Use comp prog");

            GLES31.glBindImageTexture(1, mIntermediateTex[0], 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA32F);
            GlUtil.checkGlError("Bind image texture");
            GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mCodebookSSBO[0]);
            GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mCodebookSSBO[0]);
            GlUtil.checkGlError("Buffer base");

        startGLTimer();
            launchCompProgWithDefaultWorkGroupSize(mCompProg);
        endGLTimer("codebook background genb/sub");

            if (mCompProg == mCodebookBGSubYCbCrSqrSqrtCompProg ||
                mCompProg == mCodebookBGSubYCbCrAdditiveCompProg ||
                mCompProg == mCodebookBGSubRGBSqrSqrtCompProg ||
                mCompProg == mCodebookBGSubRGBAdditiveCompProg
                    ) {

                // put morphology here
            startGLTimer();
                performMorphology(mIntermediateTex[0], mIntermediateTex[2]);
            endGLTimer("Morphology");

                //TODO this is also pretty hacky
                morphOutputTexId = (mMorphNumSteps % 2 == 0)? mIntermediateTex[0] : mIntermediateTex[2];

                // Tex slot 1 has the mask after applying morphology
                // tex slot 0 has the original camera texture
                GLES31.glBindImageTexture(1, morphOutputTexId, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                GlUtil.checkGlError("Bind image texture");
                GLES31.glBindImageTexture(0, mIntermediateTex[1], 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA32F);
                GlUtil.checkGlError("Bind camera texture");

                // moments
                    // bind the moment buffers
                    GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mMomentSSBO[0]);
                    GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mMomentSSBO[0]);
                    // re init the buffers
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mMomentInitProg);
                endGLTimer("Moment Init");
                        // accumulate the moments
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mMomentAccumPorg);
                endGLTimer("Moment Accum");
                        // calculate the compound moments
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mMomentCalcProg);
                endGLTimer("Moment Calc");
                    // use moments (where we learn COG) to do another filter
                    GLES31.glUseProgram(mDistFilterCompProg);
                    GLES31.glUniform1f(GLES31.glGetUniformLocation(mDistFilterCompProg, "distThresh"), mDistThreshMax);
                startGLTimer();
                    launchCompProgWithDefaultWorkGroupSize(mDistFilterCompProg);
                endGLTimer("Maximum Distance Filter");
                //TODO are the cases where this actually helps frequent enough to warrant this?
                    // and recenter the COG
                    // re init the buffers
                    launchCompProgWithDefaultWorkGroupSize(mMomentInitProg);
                    // accumulate the moments
                    launchCompProgWithDefaultWorkGroupSize(mMomentAccumPorg);
                    // calculate the compound moments
                    launchCompProgWithDefaultWorkGroupSize(mMomentCalcProg);
                // fill in holes?
                if (mFillMethod == FillMethod.FILL_NEIGHBOUR) {
                    GLES31.glUseProgram(mFillInHolesNeighbourCompProg);
                    GLES31.glUniform1f(GLES31.glGetUniformLocation(mFillInHolesNeighbourCompProg, "fillInHolesThresh"), mFillInHolesThresh);
                    launchCompProgWithDefaultWorkGroupSize(mFillInHolesNeighbourCompProg);
                }
                // from the moments count the width of the arm
                    // TODO this is pretty useless cut out wrist candidate stuff
                    // TODO lazy waaaaaay first
                    // re init the arm width buffers;
                    /*
                    GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mArmWidthSSBO[0]);
                    GlUtil.checkGlError("Bind Arm Width");
                    GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 3, mArmWidthSSBO[0]);
                    GlUtil.checkGlError("bind bas moment");
                    GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, mWristCandidateSSBO[0]);
                    GlUtil.checkGlError("Bind moment");
                    GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 4, mWristCandidateSSBO[0]);
                    GlUtil.checkGlError("bind bas moment");
                    GLES31.glUseProgram(mArmWidthInitCompProg);
                    launchCompProgWithDefaultWorkGroupSize(mArmWidthInitCompProg);
                    // accumulate the non zero pixels (foreground)
                    launchCompProgWithDefaultWorkGroupSize(mArmWidthAccumCompProg);
                    // find wrist candidates
                    GLES31.glUseProgram(mArmWidthFindMininumCompProg);
                    GLES31.glUniform1f(GLES31.glGetUniformLocation(mArmWidthFindMininumCompProg, "armWidthThresh"), mArmWidthThreshold);
                    launchCompProgWithDefaultWorkGroupSize(mArmWidthFindMininumCompProg);
                    */

                // TODO do I want a second threshold here for min arm width?
                if (mFillMethod == FillMethod.FILL_DISTANCE) {
                    GLES31.glUseProgram(mFillInHolesDistanceCompProg);
                    GLES31.glUniform1f(GLES31.glGetUniformLocation(mFillInHolesDistanceCompProg, "fillInHolesThresh"), mFillInHolesThresh);
                    launchCompProgWithDefaultWorkGroupSize(mFillInHolesDistanceCompProg);
                }

                // change the mask to have pixels from the camera
                launchCompProgWithDefaultWorkGroupSize(mMaskCompProg);

                // TODO this doesn't do much can probs cut this out as well
                // Finger Detection
                outputOfFingerDetectionTex = morphOutputTexId;
                if (mFingerDetectionState == FingerEdgeDetectionStep.CANNY) {
                    //conv grey scale
                    GLES31.glBindImageTexture(3, mIntermediateTex[3], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GlUtil.checkGlError("Bind a prev unused texture");
                    launchCompProgWithDefaultWorkGroupSize(mCannyConvToGrayCompProg);
                    //gauss blur
                    GLES31.glBindImageTexture(1, mIntermediateTex[3], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GLES31.glBindImageTexture(3, mIntermediateTex[4], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    launchCompProgWithDefaultWorkGroupSize(mCannyGaussBlurHorizontalCompProg);
                    GLES31.glBindImageTexture(1, mIntermediateTex[4], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GLES31.glBindImageTexture(3, mIntermediateTex[3], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    launchCompProgWithDefaultWorkGroupSize(mCannyGaussBlurVerticalCompProg);
                    //x,y derivs
                    GLES31.glBindImageTexture(1, mIntermediateTex[3], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GLES31.glBindImageTexture(3, mIntermediateTex[4], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    launchCompProgWithDefaultWorkGroupSize(mCannySobelCompProg);
                    //non max sup
                    GLES31.glBindImageTexture(1, mIntermediateTex[4], 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GLES31.glBindImageTexture(3, morphOutputTexId, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                    GLES31.glUseProgram(mCannyNonMaxSupCompProg);
                    GLES31.glUniform1f(GLES31.glGetUniformLocation(mCannyNonMaxSupCompProg, "magThresh"), mCannyThreshold);
                    launchCompProgWithDefaultWorkGroupSize(mCannyNonMaxSupCompProg);
                } else if (mFingerDetectionState != FingerEdgeDetectionStep.OFF){
                    outputOfFingerDetectionTex = fingerDetection(morphOutputTexId, mIntermediateTex[3], mIntermediateTex[4]);
                }

                // draw the arm properties / center of gravity on top of the masked camera image
                GLES31.glBindImageTexture(1, outputOfFingerDetectionTex, 0, false, 0, GLES31.GL_READ_WRITE, GLES31.GL_RGBA32F);
                //GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 4, mWristCandidateSSBO[0]);
                GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mMomentSSBO[0]);
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mDrawCOGProg);
            endGLTimer("Draw COG");

                //TODO this is very very in efficent takes 5FPS remove for timing
            startGLTimer();
                launchCompProgWithDefaultWorkGroupSize(mChangeBackgroundColourCompProg);
            endGLTimer("Draw Change Background colour");
            }
        }

        // output the texture to the screen by drawing to framebuffer 0
        // don't want to modify the final texture so just use simple shader that doesn't transform the texture
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, outputOfFingerDetectionTex);
        //GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mMapTexId);
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
        if (mUseZoom) {
            // TODO add a zoom level parameter and it would be nice to get scrolling zoom
            GLES31.glUseProgram(mZoomProg);
            GLES31.glUniform1f(GLES31.glGetUniformLocation(mZoomProg, "zoom"), mZoomLevel);
        } else {
            GLES31.glUseProgram(mDoNothingProg);
        }

        int textureParamHandle = GLES31.glGetUniformLocation(mDoNothingProg, "camTexture");
        int posHandle          = GLES31.glGetAttribLocation(mDoNothingProg, "position");
        int texCoordinateHandle= GLES31.glGetAttribLocation(mDoNothingProg, "camTexCoordinate");

        GLES31.glEnableVertexAttribArray(posHandle);
        GLES31.glVertexAttribPointer(posHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, vertexBuffer);
        //GLES31.glVertexAttribPointer(posHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, leftHalfVertexBuffer);
        GLES31.glUniform1i(textureParamHandle, 0);
        GLES31.glEnableVertexAttribArray(texCoordinateHandle);
        GlUtil.checkGlError("Enable tex coord");
        GLES31.glVertexAttribPointer(texCoordinateHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, textureBuffer);

    startGLTimer();
        drawElements();
    endGLTimer("Draw elements");

        GLES31.glDisableVertexAttribArray(posHandle);
        GLES31.glDisableVertexAttribArray(texCoordinateHandle);

        //drawMap();

        //drawArrow();

        onDrawCleanup();
    }

    private void drawMap() {
        //textures
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mMapTexId);

        GLES31.glUseProgram(mMapProg);

        GLES31.glUniform1f(GLES31.glGetUniformLocation(mMapProg, "scale"), mapSize);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mMapProg, "xPos"),  mapXPos);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mMapProg, "yPos"),  mapYPos);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mMapProg, "angle"), mapAngle);

        // pos
        int mapVertexHandle = GLES31.glGetAttribLocation(mMapProg, "position");
        GlUtil.checkGlError("Map vertex pos");
        GLES31.glEnableVertexAttribArray(mapVertexHandle);
        GlUtil.checkGlError("Map vertex pos enable");
        GLES31.glVertexAttribPointer(mapVertexHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, rightHalfVertexBuffer);
        GlUtil.checkGlError("Map vertex pos set");

        // tex pos
        int mapTexHandle = GLES31.glGetAttribLocation(mMapProg, "mapTexCoordinate");
        GlUtil.checkGlError("Map tex pos");
        GLES31.glEnableVertexAttribArray(mapTexHandle);
        GlUtil.checkGlError("Map tex pos enable handle: " + mapTexHandle);
        GLES31.glVertexAttribPointer(mapTexHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, mapTextureBuffer);
        GlUtil.checkGlError("Map tex set");

    startGLTimer();
        GLES31.glDrawElements(GLES31.GL_TRIANGLES, drawOrder.length, GLES31.GL_UNSIGNED_SHORT, drawListBuffer);
    endGLTimer("Draw Map");
        GlUtil.checkGlError("Map draw");

        GLES31.glDisableVertexAttribArray(mapVertexHandle);
        GlUtil.checkGlError("Map vertex pos disable");
        GLES31.glDisableVertexAttribArray(mapTexHandle);
        GlUtil.checkGlError("Map tex pos disable");
    }

    private void drawArrow() {
        GLES31.glUseProgram(mArrowProg);
        int arrowPosHandle = GLES31.glGetAttribLocation(mArrowProg, "position");
        GLES31.glEnableVertexAttribArray(arrowPosHandle);
        GlUtil.checkGlError("enable arrow VAO");
        GLES31.glVertexAttribPointer(arrowPosHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, arrowVertexBuffer);
        GlUtil.checkGlError("try setting arrow vao");

        GLES31.glUniform1f(GLES31.glGetUniformLocation(mArrowProg, "scale"), cursorSize);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mArrowProg, "xPos") , cursorXPos);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mArrowProg, "yPos") , cursorYPos);
        GLES31.glUniform1f(GLES31.glGetUniformLocation(mArrowProg, "angle"), cursorAngle);

    startGLTimer();
        GLES31.glDrawElements(GLES31.GL_TRIANGLES, arrowDrawOrder.length, GLES31.GL_UNSIGNED_SHORT, arrowDrawListBuffer);
    endGLTimer("Draw Arrow");
        GlUtil.checkGlError("draw the arrow");
        GLES31.glDisableVertexAttribArray(arrowPosHandle);
        GlUtil.checkGlError("clean up after the arrow");
    }

    /**
     * update the SurfaceTexture to the latest camera image
     */
    protected void updatePreviewTexture()
    {
        mPreviewTexture.updateTexImage();
        mPreviewTexture.getTransformMatrix(mCameraTransformMatrix);
    }

    /**
     * base amount of attributes needed for rendering camera to screen
     */
    protected void setUniformsAndAttribs()
    {
        GLES31.glUseProgram(mCameraShaderProgram);

        int textureParamHandle = GLES31.glGetUniformLocation(mCameraShaderProgram, "camTexture");
        int textureTranformHandle = GLES31.glGetUniformLocation(mCameraShaderProgram, "camTextureTransform");
        textureCoordinateHandle = GLES31.glGetAttribLocation(mCameraShaderProgram, "camTexCoordinate");
        positionHandle = GLES31.glGetAttribLocation(mCameraShaderProgram, "position");


        GLES31.glEnableVertexAttribArray(positionHandle);
        GLES31.glVertexAttribPointer(positionHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, vertexBuffer);

        //camera texture
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexturesIds[0]);
        GLES31.glUniform1i(textureParamHandle, 0);

        GLES31.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES31.glVertexAttribPointer(textureCoordinateHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, textureBuffer);

        GLES31.glUniformMatrix4fv(textureTranformHandle, 1, false, mCameraTransformMatrix, 0);
    }

    protected void setUniformsAttribsForProgram(int program) {
        GLES31.glUseProgram(program);

        int textureParamHandle = GLES31.glGetUniformLocation(program, "camTexture");
        int textureTranformHandle = GLES31.glGetUniformLocation(program, "camTextureTransform");
        int texCoordinateHandle = GLES31.glGetAttribLocation(program, "camTexCoordinate");
        int posHandle = GLES31.glGetAttribLocation(program, "position");


        GLES31.glEnableVertexAttribArray(posHandle);
        GLES31.glVertexAttribPointer(posHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, vertexBuffer);

        //camera texture
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexturesIds[0]);
        GLES31.glUniform1i(textureParamHandle, 0);

        GLES31.glEnableVertexAttribArray(texCoordinateHandle);
        GLES31.glVertexAttribPointer(texCoordinateHandle, 2, GLES31.GL_FLOAT, false, 4 * 2, textureBuffer);

        GLES31.glUniformMatrix4fv(textureTranformHandle, 1, false, mCameraTransformMatrix, 0);
    }

    /**
     * creates a new texture with specified resource id and returns the
     * tex id num upon completion
     * @param resource_id
     * @param uniformName
     * @return
     */
    public int addTexture(int resource_id, String uniformName)
    {
        int texId = mTextureConsts[mTextureArray.size()];
        if(mTextureArray.size() + 1 >= MAX_TEXTURES)
            throw new IllegalStateException("Too many textures! Please don't use so many :(");

        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), resource_id);

        return addTexture(texId, bmp, uniformName, true);
    }

    public int addTexture(Bitmap bitmap, String uniformName)
    {
        int texId = mTextureConsts[mTextureArray.size()];
        if(mTextureArray.size() + 1 >= MAX_TEXTURES)
            throw new IllegalStateException("Too many textures! Please don't use so many :(");

        return addTexture(texId, bitmap, uniformName, true);
    }

    public int addTexture(int texId, Bitmap bitmap, String uniformName, boolean recycle)
    {
        int num = mTextureArray.size() + 1;

        GLES31.glActiveTexture(texId);
        checkGlError("Texture generate");
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTexturesIds[num]);
        checkGlError("Texture bind");
        GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_NEAREST);
        GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST);
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0);

        if(recycle)
            bitmap.recycle();

        Texture tex = new Texture(num, texId, uniformName);

        if(!mTextureArray.contains(tex)) {
            mTextureArray.add(tex);
            Log.d(TAG, "addedTexture() " + mTexturesIds[num] + " : " + tex);
        }

        return num;
    }

    /**
     * updates specific texture and recycles bitmap used for updating
     * @param texNum
     * @param drawingCache
     */
    public void updateTexture(int texNum, Bitmap drawingCache)
    {
        GLES31.glActiveTexture(mTextureConsts[texNum - 1]);
        checkGlError("Texture generate");
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTexturesIds[texNum]);
        checkGlError("Texture bind");
        GLUtils.texSubImage2D(GLES31.GL_TEXTURE_2D, 0, 0, 0, drawingCache);
        checkGlError("Tex Sub Image");

        drawingCache.recycle();
    }

    /**
     * override this and copy if u want to add your own mTexturesIds
     * if u need different uv coordinates, refer to {@link #setupTextures()}
     * for how to create your own buffer
     */
    protected void setExtraTextures()
    {
        for(int i = 0; i < mTextureArray.size(); i++)
        {
            Texture tex = mTextureArray.get(i);
            int imageParamHandle = GLES31.glGetUniformLocation(mCameraShaderProgram, tex.uniformName);

            GLES31.glActiveTexture(tex.texId);
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTexturesIds[tex.texNum]);
            GLES31.glUniform1i(imageParamHandle, tex.texNum);
        }
    }

    protected void drawElements() {
        GLES31.glDrawElements(GLES31.GL_TRIANGLES, drawOrder.length, GLES31.GL_UNSIGNED_SHORT, drawListBuffer);
    }

    protected void onDrawCleanup() {
        GLES31.glDisableVertexAttribArray(positionHandle);
        GLES31.glDisableVertexAttribArray(textureCoordinateHandle);
        mSaveData = false;
    }

    /**
     * utility for checking GL errors
     * @param op
     */
    public void checkGlError(String op) {
        int error;
        while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    //getters and setters

    public void setViewport(int viewportWidth, int viewportHeight)
    {
        mViewportWidth = viewportWidth;
        mViewportHeight = viewportHeight;
    }

    public float[] getCameraTransformMatrix() {
        return mCameraTransformMatrix;
    }

    public SurfaceTexture getPreviewTexture() {
        return mPreviewTexture;
    }

    public RenderHandler getRenderHandler() {
        return mHandler;
    }

    public void setOnRendererReadyListener(OnRendererReadyListener listener) {
        mOnRendererReadyListener = listener;

    }

    /**
     * Triggers our built-in MediaRecorder to start recording
     * @param outputFile a {@link File} where we'll be saving the completed render
     */
    public void startRecording(File outputFile) {
        mOutputFile = outputFile;

        if(mOutputFile == null)
            throw new RuntimeException("No output file specified! Make sure to call setOutputFile prior to recording!");

        synchronized (this) {
            mIsRecording = true;
            mMediaRecorder.start();
        }
    }

    /**
     * stops our mediarecorder if its still running and starts our copy from temp to regular
     */
    public void stopRecording() {
        synchronized (this) {
            if(!mIsRecording)
                return;

            mMediaRecorder.stop();
            mMediaRecorder.release();

            mIsRecording = false;

            try {
                copyFile(mTempOutputFile, mOutputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRecording() {
        synchronized (this) {
            return mIsRecording;
        }
    }

    /**
     * Copies file recorded to our temp file into the user-defined file upon completion
     */
    protected void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally {
            if (inChannel != null)
                inChannel.close();

            if (outChannel != null)
                outChannel.close();
        }
    }


    private final int COMP_PROG_DEF_SIZE_X = VIDEO_WIDTH / 8;
    private final int COMP_PROG_DEF_SIZE_Y = VIDEO_HEIGHT / 8;
    private final int COMP_PROG_DEF_SIZE_Z = 1;
    private void launchCompProgWithDefaultWorkGroupSize(int compProgID) {
        GLES31.glUseProgram(compProgID);
        GLES31.glDispatchCompute(COMP_PROG_DEF_SIZE_X, COMP_PROG_DEF_SIZE_Y, COMP_PROG_DEF_SIZE_Z);
        GlUtil.checkGlError("Dispatch");
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT);
        GlUtil.checkGlError("Mem barrior");
    }

    private long mStartTime;
    private long mEndTime;
    private final static boolean PERFORM_TIMING = false;
    protected void startGLTimer() {
        if (PERFORM_TIMING) {
            GLES31.glFinish();
            mStartTime = System.nanoTime() / 1000; //us
        }
    }
    protected void endGLTimer(String message) {
        if (PERFORM_TIMING) {
            GLES31.glFinish();
            mEndTime = System.nanoTime() / 1000; //us
            Log.v(TAG, String.format(message + " us %d", mEndTime - mStartTime));
        }
    }
    /**
     * Internal class for storing refs to mTexturesIds for rendering
     */
    private class Texture {
        public int texNum;
        public int texId;
        public String uniformName;

        private Texture(int texNum, int texId, String uniformName) {
            this.texNum = texNum;
            this.texId = texId;
            this.uniformName = uniformName;
        }
        @Override
        public String toString() {
            return "[Texture] num: " + texNum + " id: " + texId + ", uniformName: " + uniformName;
        }

    }

    /**
     * {@link Handler} responsible for communication between this render thread and the UI thread.
     *
     * For now, the only thing we really need to worry about is shutting down the thread upon completion
     * of recording, since we cannot access the {@link android.media.MediaRecorder} surface once
     * {@link MediaRecorder#stop()} is called.
     */
    public static class RenderHandler extends Handler
    {
        private static final String TAG = RenderHandler.class.getSimpleName();

        private static final int MSG_SHUTDOWN = 0;

        /**
         * Our camera renderer ref, weak since we're dealing with static class so it doesn't leak
         */
        private WeakReference<CameraRenderer> mWeakRenderer;

        /**
         * Call from render thread.
         */
        public RenderHandler(CameraRenderer rt) {
            mWeakRenderer = new WeakReference<>(rt);
        }

        /**
         * Sends the "shutdown" message, which tells the render thread to halt.
         * Call from UI thread.
         */
        public void sendShutdown() {
            sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
        }

        @Override
        public void handleMessage(Message msg)
        {
            CameraRenderer renderer = mWeakRenderer.get();
            if (renderer == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            int what = msg.what;
            switch (what) {
                case MSG_SHUTDOWN:
                    renderer.shutdown();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }

    /**
     * Interface for callbacks when render thread completes its setup
     */
    public interface OnRendererReadyListener {
        /**
         * Called when {@link #onSetupComplete()} is finished with its routine
         */
        void onRendererReady();

        /**
         * Called once the looper is killed and our {@link #run()} method completes
         */
        void onRendererFinished();
    }
}
