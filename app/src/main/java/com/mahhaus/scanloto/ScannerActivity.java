package com.mahhaus.scanloto;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import at.nineyards.anyline.camera.CameraController;
import at.nineyards.anyline.camera.CameraOpenListener;
import at.nineyards.anyline.models.AnylineImage;
import at.nineyards.anyline.modules.document.DocumentResultListener;
import at.nineyards.anyline.modules.document.DocumentScanView;

public class ScannerActivity extends AppCompatActivity implements CameraOpenListener{

    private static final String TAG = ScannerActivity.class.getSimpleName();
    private DocumentScanView documentScanView;
    private Toast notificationToast;
    private ImageView imageViewResult;
    private ProgressDialog progressDialog;
    private List<PointF> lastOutline;
    private ObjectAnimator errorMessageAnimator;
    private FrameLayout errorMessageLayout;
    private TextView errorMessage;
    private long lastErrorRecieved = 0;

    private android.os.Handler handler = new android.os.Handler();

    // takes care of fading the error message out after some time with no error reported from the SDK
    private Runnable errorMessageCleanup = new Runnable(){
        @Override
        public void run() {
            if( System.currentTimeMillis() > lastErrorRecieved + getApplication().getResources().getInteger(R.integer.error_message_delay)) {
                if(errorMessage == null || errorMessageAnimator == null){
                    return;
                }
                if(errorMessage.getAlpha() == 0f){
                    errorMessage.setText("");
                } else if(!errorMessageAnimator.isRunning()){
                    errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", errorMessage.getAlpha(), 0f);
                    errorMessageAnimator.setDuration(getResources().getInteger(R.integer.error_message_delay));
                    errorMessageAnimator.setInterpolator(new AccelerateInterpolator());
                    errorMessageAnimator.start();
                }
            }
            handler.postDelayed(errorMessageCleanup, getResources().getInteger(R.integer.error_message_delay));

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_scan);
        //Set the flag to keep the screen on (otherwise the screen may go dark during scanning)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageViewResult = (ImageView) findViewById(R.id.image_result);
        errorMessageLayout = (FrameLayout) findViewById(R.id.error_message_layout);
        errorMessage = (TextView) findViewById(R.id.textViewResult);

        documentScanView = (DocumentScanView) findViewById(R.id.document_scan_view);
        // add a camera open listener that will be called when the camera is opened or an error occurred
        //  this is optional (if not set a RuntimeException will be thrown if an error occurs)
        documentScanView.setCameraOpenListener(ScannerActivity.this);
        // the view can be configured via a json file in the assets, and this config is set here
        // (alternatively it can be configured via xml, see the Energy Example for that)
        documentScanView.setConfigFromAsset("document_view_config.json");

        // Optional: Set a ratio you want the documents to be restricted to. default is set to DIN_AX
        //documentScanView.setDocumentRatios(DocumentScanView.DocumentRatio.DIN_AX_PORTRAIT.getRatio());

        // Optional: Set a maximum deviation for the ratio. 0.15 is the default
        //documentScanView.setMaxDocumentRatioDeviation(0.15);

        // initialize Anyline with the license key and a Listener that is called if a result is found
        documentScanView.initAnyline(getString(R.string.anyline_license_key), new DocumentResultListener() {
//            @Override
//            public void onResult(AnylineImage transformedImage, AnylineImage fullFrame, List<PointF> documentOutline) {
//
//
//            }


            @Override
            public void onResult(AnylineImage transformedImage, AnylineImage fullFrame) {
                errorMessage.setText(fullFrame.toString());
                // handle the result document images here
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                imageViewResult.setImageBitmap(Bitmap.createScaledBitmap(transformedImage.getBitmap(), 100, 160, false));

                /**
                 * IMPORTANT: cache provided frames here, and release them at the end of this onResult. Because
                 * keeping them in memory (e.g. setting the full frame to an ImageView)
                 * will result in a OutOfMemoryError soon. This error is reported in {@link #onTakePictureError
                 * (Throwable)}
                 *
                 * Use a DiskCache http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#disk-cache
                 * for example
                 *
                 */
                File outDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ok");
                outDir.mkdir();
                // change the file ending to png if you want a png
                File outFile = new File(outDir, "" + System.currentTimeMillis() + ".jpg");
                try {
                    // convert the transformed image into a gray scaled image internally
                    // transformedImage.getGrayCvMat(false);
                    // get the transformed image as bitmap
                    // Bitmap bmp = transformedImage.getBitmap();
                    // save the image with quality 100 (only used for jpeg, ignored for png)
                    transformedImage.save(outFile, 100);
                    showToast(getString(R.string.document_image_saved_to) +" " + outFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // release the images
                transformedImage.release();
                fullFrame.release();

            }

            @Override
            public void onPreviewProcessingSuccess(AnylineImage anylineImage) {
                // this is called after the preview of the document is completed, and a full picture will be
                // processed automatically
            }

            @Override
            public void onPreviewProcessingFailure(DocumentScanView.DocumentError documentError) {
                // this is called on any error while processing the document image
                // Note: this is called every time an error occurs in a run, so that might be quite often
                // An error message should only be presented to the user after some time

                showErrorMessageFor(documentError);
            }

            @Override
            public void onPictureProcessingFailure(DocumentScanView.DocumentError documentError) {

                showErrorMessageFor(documentError,true);
                if(progressDialog != null && progressDialog.isShowing()){
                    progressDialog.dismiss();
                }

                // if there is a problem, here is how images could be saved in the error case
                // this will be a full, not cropped, not transformed image
                AnylineImage image = documentScanView.getCurrentFullImage();

                if (image != null) {
                    File outDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "error");
                    outDir.mkdir();
                    File outFile = new File(outDir, "" + System.currentTimeMillis() + documentError.name() + ".jpg");
                    try {
                        image.save(outFile, 100);
                        Log.d(TAG, "error image saved to " + outFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    image.release();
                }
            }

            @Override
            public boolean onDocumentOutlineDetected(List<PointF> list, boolean documentShapeAndBrightnessValid) {
                // is called when the outline of the document is detected. return true if the outline is consumed by
                // the implementation here, false if the outline should be drawn by the DocumentScanView
                lastOutline = list; // saving the outline for the animations
                return false;
            }

            @Override
            public void onTakePictureSuccess() {
                // this is called after the image has been captured from the camera and is about to be processed
                progressDialog = ProgressDialog.show(ScannerActivity.this, "", "",
                        true);

                if (errorMessageAnimator != null && errorMessageAnimator.isRunning()) {
                    errorMessageAnimator.cancel();
                    errorMessageLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTakePictureError(Throwable throwable) {
                // This is called if the image could not be captured from the camera (most probably because of an
                // OutOfMemoryError)
                throw new RuntimeException(throwable);
            }

        });

        // optionally stop the scan once a valid result was returned
        // documentScanView.cancelOnResult(true);

    }


    private void showErrorMessageFor(DocumentScanView.DocumentError documentError){
        showErrorMessageFor(documentError,false);
    }
    private void showErrorMessageFor(DocumentScanView.DocumentError documentError, boolean highlight) {
        String text = getString(R.string.document_picture_error);
        switch (documentError) {
            case DOCUMENT_NOT_SHARP:
                text += getString(R.string.document_error_not_sharp);
                break;
            case DOCUMENT_SKEW_TOO_HIGH:
                text += getString(R.string.document_picture_error);
                break;
            case DOCUMENT_OUTLINE_NOT_FOUND:
                //text += getString(R.string.document_error_outline_not_found);
                return; // exit and show no error message for now!
            case IMAGE_TOO_DARK:
                text += getString(R.string.document_picture_error);
                break;
            case SHAKE_DETECTED:
                text += getString(R.string.document_picture_error);
                break;
            case UNKNOWN:
                break;
            default:
                text += getString(R.string.document_error_unknown);
                return; // exit and show no error message for now!
        }

        if(highlight) {
            showHighlightErrorMessageUiAnimated(text);
        }
        else {
            showErrorMessageUiAnimated(text);
        }
    }

    private void showErrorMessageUiAnimated(String message) {
        if(lastErrorRecieved == 0) {
            // the cleanup takes care of removing the message after some time if the error did not show up again
            handler.post(errorMessageCleanup);
        }
        lastErrorRecieved = System.currentTimeMillis();
        if (errorMessageAnimator != null && (errorMessageAnimator.isRunning() || errorMessage.getText().equals
                (message))) {
            return;
        }

        errorMessageLayout.setVisibility(View.VISIBLE);
        errorMessage.setBackgroundColor(ContextCompat.getColor(this, R.color.anyline_blue_darker));
        errorMessage.setAlpha(0f);
        errorMessage.setText(message);
        errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", 0f, 1f);
        errorMessageAnimator.setDuration(getResources().getInteger(R.integer.error_message_delay));
        errorMessageAnimator.setInterpolator(new DecelerateInterpolator());
        errorMessageAnimator.start();
    }
    private void showHighlightErrorMessageUiAnimated(String message){
        lastErrorRecieved = System.currentTimeMillis();
        errorMessageLayout.setVisibility(View.VISIBLE);
        errorMessage.setBackgroundColor(ContextCompat.getColor(this,R.color.anyline_red));
        errorMessage.setAlpha(0f);
        errorMessage.setText(message);

        if(errorMessageAnimator != null && errorMessageAnimator.isRunning()){
            errorMessageAnimator.cancel();
        }

        errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", 0f, 1f);
        errorMessageAnimator.setDuration(getResources().getInteger(R.integer.error_message_delay));
        errorMessageAnimator.setInterpolator(new DecelerateInterpolator());
        errorMessageAnimator.setRepeatMode(ValueAnimator.REVERSE);
        errorMessageAnimator.setRepeatCount(1);
        errorMessageAnimator.start();
    }

    private void showToast(String text) {
        try {
            notificationToast.setText(text);
        } catch (Exception e) {
            notificationToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        }
        notificationToast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //start the actual scanning
        documentScanView.startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop the scanning
        documentScanView.cancelScanning();
        //release the camera (must be called in onPause, because there are situations where
        // it cannot be auto-detected that the camera should be released)
        documentScanView.releaseCameraInBackground();
    }

    @Override
    public void onCameraOpened(CameraController cameraController, int width, int height) {
        //the camera is opened async and this is called when the opening is finished
        Log.d(TAG, "Camera opened successfully. Frame resolution " + width + " x " + height);
    }

    @Override
    public void onCameraError(Exception e) {
        //This is called if the camera could not be opened.
        // (e.g. If there is no camera or the permission is denied)
        // This is useful to present an alternative way to enter the required data if no camera exists.
        throw new RuntimeException(e);
    }
}