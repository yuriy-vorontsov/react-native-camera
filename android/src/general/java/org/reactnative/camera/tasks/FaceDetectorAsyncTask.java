package org.reactnative.camera.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.cameraview.CameraView;
import com.google.android.gms.vision.face.Face;

import org.reactnative.camera.utils.ImageDimensions;
import org.reactnative.facedetector.FaceDetectorUtils;
import org.reactnative.frame.RNFrame;
import org.reactnative.frame.RNFrameFactory;
import org.reactnative.facedetector.RNFaceDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.NullPointerException;

public class FaceDetectorAsyncTask extends android.os.AsyncTask<Void, Void, SparseArray<Face>> {
  private byte[] mImageData;
  private int mWidth;
  private int mHeight;
  private int mRotation;
  private RNFaceDetector mFaceDetector;
  private FaceDetectorAsyncTaskDelegate mDelegate;
  private ImageDimensions mImageDimensions;
  private double mScaleX;
  private double mScaleY;
  private int mPaddingLeft;
  private int mPaddingTop;

  public FaceDetectorAsyncTask(
      FaceDetectorAsyncTaskDelegate delegate,
      RNFaceDetector faceDetector,
      byte[] imageData,
      int width,
      int height,
      int rotation,
      float density,
      int facing,
      int viewWidth,
      int viewHeight,
      int viewPaddingLeft,
      int viewPaddingTop
  ) {
    mImageData = imageData;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mDelegate = delegate;
    mFaceDetector = faceDetector;
    mImageDimensions = new ImageDimensions(width, height, rotation, facing);
    mScaleX = (double) (viewWidth) / (mImageDimensions.getWidth() * density);
    mScaleY = (double) (viewHeight) / (mImageDimensions.getHeight() * density);
    mPaddingLeft = viewPaddingLeft;
    mPaddingTop = viewPaddingTop;
  }

  @Override
  protected SparseArray<Face> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mFaceDetector == null || !mFaceDetector.isOperational()) {
      return null;
    }

    RNFrame frame = RNFrameFactory.buildFrame(mImageData, mWidth, mHeight, mRotation);
    return mFaceDetector.detect(frame);
  }

  @Override
  protected void onPostExecute(SparseArray<Face> faces) {
    super.onPostExecute(faces);

    if (faces == null) {
      mDelegate.onFaceDetectionError(mFaceDetector);
    } else {
      if (faces.size() > 0) {
        // generate base64String
        String base64String = "";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
          // Decode image from the retrieved buffer to JPEG
          YuvImage image = new YuvImage(mImageData, ImageFormat.NV21, mWidth, mHeight, null);
          image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, outputStream);

          if (mRotation > 0) {
            // This is the same image as the preview but in JPEG and not rotated
            byte[] rawImage = outputStream.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);

            Matrix matrix = new Matrix();
            matrix.postRotate(mRotation);

            // We dump the rotated Bitmap to the stream
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight, matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, rotatedStream);
            base64String = Base64.encodeToString(rotatedStream.toByteArray(), Base64.NO_WRAP);

          } else {
            base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
          }

          YuvImage image = new YuvImage(mImageData, ImageFormat.NV21, mWidth, mHeight, null);
          image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, outputStream);
          base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);

        } catch (NullPointerException e) {
          // skip
          Log.e("RNCamera+", "problem compressing jpeg: ", e);

        } finally {
          try {
            outputStream.close();
          } catch (IOException e) {
            Log.e("RNCamera+", "problem compressing jpeg: ", e);
          }
        }
        // end of generate base64String

        mDelegate.onFacesDetected(serializeEventData(faces), base64String);
      }
      mDelegate.onFaceDetectingTaskCompleted();
    }
  }

  private WritableArray serializeEventData(SparseArray<Face> faces) {
    WritableArray facesList = Arguments.createArray();

    for(int i = 0; i < faces.size(); i++) {
      Face face = faces.valueAt(i);
      WritableMap serializedFace = FaceDetectorUtils.serializeFace(face, mScaleX, mScaleY, mWidth, mHeight, mPaddingLeft, mPaddingTop);
      if (mImageDimensions.getFacing() == CameraView.FACING_FRONT) {
        serializedFace = FaceDetectorUtils.rotateFaceX(serializedFace, mImageDimensions.getWidth(), mScaleX);
      } else {
        serializedFace = FaceDetectorUtils.changeAnglesDirection(serializedFace);
      }
      facesList.pushMap(serializedFace);
    }

    return facesList;
  }
}
