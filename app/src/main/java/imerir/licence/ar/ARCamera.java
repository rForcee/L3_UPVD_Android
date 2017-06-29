package imerir.licence.ar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by Erwann on 01/06/17.
 */

/**
 * Classe implémentant les méthodes pour insérer l'image captée par la caméra sur la vue
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ARCamera extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "ARCamera";

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.Size previewSize;
    List<Camera.Size> supportedPreviewSizes;
    Camera camera;
    Camera.Parameters parameters;
    Activity activity;

    float[] projectionMatrix = new float[16];

    int cameraWidth;
    int cameraHeight;
    private final static float Z_NEAR = 0.5f;
    private final static float Z_FAR = 2000;

    /**
     * Constructeur de la caméra
     * @param context le contexte
     * @param surfaceView la vue
     */
    public ARCamera(Context context, SurfaceView surfaceView) {
        super(context);

        this.surfaceView = surfaceView;
        this.activity = (Activity) context;
        surfaceHolder = this.surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Instancie la caméra
     * @param camera la caméra
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
        if (this.camera != null) {
            supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
            requestLayout();
            Camera.Parameters params = this.camera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                this.camera.setParameters(params);
            }
        }
    }

    /**
     * Instancie la vue en fonction de la taille de l'acran
     * @param widthMeasureSpec la largeur
     * @param heightMeasureSpec la hauteur
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
        }
    }

    /**
     * Si les éléments sur le layout changent
     * @param changed vrai si le layout a changé, faux sinon
     * @param left coordonnées du layout
     * @param top coordonnées du layout
     * @param right coordonnées du layout
     * @param bottom coordonnées du layout
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = right - left;
            final int height = bottom - top;

            int previewWidth = width;
            int previewHeight = height;
            if (previewSize != null) {
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;
            }

            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    /**
     * Paramètrage de l'affichage
     * @param holder la surface
     */
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (camera != null) {

                parameters = camera.getParameters();

                int orientation = getCameraOrientation();

                camera.setDisplayOrientation(orientation);
                camera.getParameters().setRotation(orientation);

                camera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    /**
     * Détermine l'orientation de la caméra
     * @return l'orientation de la caméra
     */
    private int getCameraOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int orientation;
        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
            orientation = (info.orientation + degrees) % 360;
            orientation =  (360 - orientation) % 360;
        } else {
            orientation = (info.orientation -degrees + 360) % 360;
        }

        return orientation;
    }

    /**
     * Lors de l'arrêt de la caméra
     * @param holder la surface
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * Permet de définir la taille la plus adaptée pour l'affichage de l'image de la caméra
     * @param sizes les différentes tailles
     * @param width la largeur
     * @param height la hauteur
     * @return la taille la plus adaptée
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        if(optimalSize == null) {
            optimalSize = sizes.get(0);
        }

        return optimalSize;
    }

    /**
     * Lors du changement d'état de la surface
     * @param holder la surface
     * @param format le format
     * @param width la largeur
     * @param height la hauteur
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(camera != null) {
            this.cameraWidth = width;
            this.cameraHeight = height;

            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(previewSize.width, previewSize.height);
            requestLayout();

            camera.setParameters(params);
            camera.startPreview();

            generateProjectionMatrix();
        }
    }

    /**
     * Matrice de projection de l'image sur l'écran
     */
    private void generateProjectionMatrix() {
        float ratio = (float) this.cameraWidth / this.cameraHeight;
        final int OFFSET = 0;
        final float LEFT =  -ratio;
        final float RIGHT = ratio;
        final float BOTTOM = -1;
        final float TOP = 1;
        Matrix.frustumM(projectionMatrix, OFFSET, LEFT, RIGHT, BOTTOM, TOP, Z_NEAR, Z_FAR);
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix;
    }

}
