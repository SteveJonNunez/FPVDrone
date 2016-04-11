package com.parrot.freeflight.activities.base;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.parrot.freeflight.R;
import com.parrot.freeflight.utils.FontUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by kryonex on 4/9/2016.
 */
public class ParrotCardboardActivity extends CardboardFragmentActivity implements CardboardView.StereoRenderer {
    private final String TAG = "ParrotCardboardActivity";
    private final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    //    private final int GL_TEXTURE_EXTERNAL_OES = GLES20.GL_TEXTURE_2D;
    final int COORDS_PER_VERTEX = 2; //number of coordinates per vertex in this array

    public int imageWidth;
    public int imageHeight;

    public int textureWidth;
    public int textureHeight;

    private LayoutInflater inflater;

    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private int program;

    private SurfaceTexture surface;
    private int textureImage;


    private float squareVertices[] = { // in counterclockwise order:
            -1.0f, -1.0f,   // 0.left - mid
            1.0f, -1.0f,   // 1. right - mid
            -1.0f, 1.0f,   // 2. left - top
            1.0f, 1.0f,   // 3. right - top
    };

    private short drawOrder[] = {0, 2, 1, 1, 2, 3}; // order to draw vertices

    private float textureVertices[] = {
            0.0f, 1.0f,  // A. left-bottom
            1.0f, 1.0f,  // B. right-bottom
            0.0f, 0.0f,  // C. left-top
            1.0f, 0.0f   // D. right-top

    };

    private CardboardView cardboardView;
    private float[] mView;
    private float[] mCamera;


    private android.graphics.Matrix matrix;
    private Object videoFrameLock;
    private Bitmap video;
    private float videoSize[] = {0, 0, //image width, height
            0, 0}; //
    public int screenWidth;
    public int screenHeight;
    private int videoWidth;
    private int videoHeight;
    private boolean isVideoReady = false;

    private int prevImgWidth;
    private int prevImgHeight;

    private int x;
    private int y;
    private int[] textures = new int[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        videoFrameLock = new Object();
        video = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);

        setContentView(R.layout.common_ui);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        mCamera = new float[16];
        mView = new float[16];
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        View rootView = findViewById(android.R.id.content);
        FontUtils.applyFont(this, (ViewGroup) rootView);
    }

    public View inflateView(int resource, ViewGroup root, boolean attachToRoot) {
        View result = inflater.inflate(resource, root, attachToRoot);

        if (result != null) {
            FontUtils.applyFont(this, result);
        }

        return result;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        onUpdateVideoTextureOriNative(program, textures[0]);
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(textures[0]);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);


        int mPositionHandle = GLES20.glGetAttribLocation(program, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        int vertexStride = COORDS_PER_VERTEX * 4;
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertexBuffer);


        int mTextureCoordHandle = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, textureVerticesBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);


        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
        //STUB
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {
        onSurfaceChangedNative(i, i1);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well

        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);


        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        String vertexShaderCode = "attribute vec4 position;" +
                "attribute vec2 inputTextureCoordinate;" +
                "varying vec2 textureCoordinate;" +
                "void main()" +
                "{" +
                "gl_Position = position;" +
                "textureCoordinate = inputTextureCoordinate;" +
                "}";
        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 textureCoordinate;\n" +
                "uniform samplerExternalOES s_texture;\n" +
                "void main(void) {" +
                "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                "}";
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(program, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(program, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(program);


        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        onUpdateVideoTextureOriNative(program, textures[0]);

    }

    @Override
    public void onRendererShutdown() {

    }

    private int loadGLShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    private native boolean onUpdateVideoTextureNative(int program, int textureId);

    private native boolean onUpdateVideoTextureOriNative(int program, int textureId);

    private native void onSurfaceCreatedNative();

    private native void onSurfaceChangedNative(int width, int height);

    private native boolean getVideoFrameNative(Bitmap bitmap, float[] ret);
}
