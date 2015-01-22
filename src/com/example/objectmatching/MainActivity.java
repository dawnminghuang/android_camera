package com.example.objectmatching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity{
	private Camera mCamera;
	private CameraPreview mPreview;
	private Bitmap testimg;
	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mRgba;
	private Mat mGray;
	private Mat mByte;
	private Scalar CONTOUR_COLOR;
	private boolean isProcess = false;
	private static final String TAG = "Dawn";
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		//mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.objectMatch);
		//mOpenCvCameraView.setCvCameraViewListener(this);
		// Create an instance of Camera
		mCamera = getCameraInstance();
		// Create our Preview view and set it as the content of our activity.
		//mPreview = new CameraPreview(this, mCamera);
		//FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		//preview.addView(mPreview);

		final ImageView showimg = (ImageView) findViewById(R.id.ImgPhoto);
		Button captureButton = (Button) findViewById(R.id.button_capture);
		Button showButton = (Button) findViewById(R.id.button_show);
		Button matchButton = (Button) findViewById(R.id.button_match);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				mCamera.takePicture(null, null, mPicture);
			}
		});
		showButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// show img in the Imageview
				showimg.setImageBitmap(testimg);
			}
		});
		matchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// show img in the Imageview
				isProcess = !isProcess;
			}
		});
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private PictureCallback mPicture = new PictureCallback() {

		/** Create a File for saving an image or video */

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile();
			if (pictureFile == null) {
				Log.d(TAG,
						"Error creating media file, check storage permissions: ");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
			testimg = BitmapFactory.decodeByteArray(data, 0, data.length);// decode
																			// bytearray
																			// to
																			// bitmap

			camera.startPreview();

		}

	};

	private File getOutputMediaFile() {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}
		// Create a media file name
		File mediaFile;
		String timeStamp = String.format("%d.jpg", System.currentTimeMillis());
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");
		return mediaFile;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera(); // release the camera immediately on pause event
/*		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();*/
	}

	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this,
				mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		Log.e("onDestroy", "INITIATED");
		super.onDestroy();
		/*if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
*/
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

/*	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC3);
		mByte = new Mat(height, width, CvType.CV_8UC1);
	}

	public void onCameraViewStopped() {
		// Explicitly deallocate Mats
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		//
		final int k=3;
		Mat testimage = new Mat();
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		CONTOUR_COLOR = new Scalar(255);
		MatOfDMatch matches=new MatOfDMatch();
		MatOfKeyPoint keypoint_train = new MatOfKeyPoint();
		MatOfKeyPoint keypoint_test  = new MatOfKeyPoint();
		KeyPoint kpoint = new KeyPoint();
		Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
		Mat output = new Mat();
		//
		Mat train=new Mat();
		Mat test=new Mat();
		if (isProcess) {
			FeatureDetector detector_train = FeatureDetector
					.create(FeatureDetector.SIFT);
			detector_train.detect(mRgba, keypoint_train);
			Features2d.drawKeypoints(mGray, keypoint_train, output, new Scalar(2,
					254, 255), Features2d.DRAW_RICH_KEYPOINTS);

			DescriptorExtractor descriptor_train = DescriptorExtractor
					.create(DescriptorExtractor.SIFT);
			descriptor_train.compute(mRgba, keypoint_train, train);

			Utils.bitmapToMat(testimg, testimage);
			FeatureDetector detector_test = FeatureDetector
					.create(FeatureDetector.SIFT);
			detector_test.detect(testimage, keypoint_test );
			
			Features2d.drawKeypoints(testimage, keypoint_test , output, new Scalar(2,
					254, 255), Features2d.DRAW_RICH_KEYPOINTS);
			DescriptorExtractor descriptor_test = DescriptorExtractor
					.create(DescriptorExtractor.SIFT);
			descriptor_test.compute(testimage, keypoint_test ,test);
			DescriptorMatcher descriptormatcher=DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
			descriptormatcher.match(test, train, matches);
            Features2d.drawMatches(mRgba,keypoint_train, testimage, keypoint_test, matches,output);
			return mRgba;
		}

		return mRgba;
	}
*/
}