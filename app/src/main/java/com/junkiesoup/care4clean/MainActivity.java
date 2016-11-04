package com.junkiesoup.care4clean;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List; import android.app.ActivityManager;

public class MainActivity extends AppCompatActivity {
    // Vars
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;
    Context context;
        // Com params
    Communication communication;
    String token = "token";
    String imageName = "imageName";
    Bitmap imageData;
        // App views:
    ImageView mImageView;
    View mCaseId;
    static View smCaseId;
    View mCaseDescription;
    View mSubmitCase;
    View progressOverlay;
    ProgressBar mProgressBar;
    View mCamInstructions;
        // Toggles
    Boolean canUpload;

    // Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        canUpload = true;
        context = this;
        // Com
        communication = new Communication(context);
        communication.setupSSLCertificate();

        // SharedPrefs
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        if (prefs.contains("token"))
        {
            token = prefs.getString("token","");
        } else{
            createUser();
        }



        // Initialize app views
        mImageView = (ImageView) findViewById(R.id.case_img);
        mCaseId = findViewById(R.id.case_id);
        smCaseId = mCaseId;
        mCaseDescription = findViewById(R.id.case_description);
        mSubmitCase = findViewById(R.id.submit_case);
        mCamInstructions = findViewById(R.id.cam_instructions);
        mProgressBar = (ProgressBar) findViewById(R.id.ProgressBar);

        Animator progressBarAnim = AnimatorInflater.loadAnimator(this,R.animator.progressloop);
        progressBarAnim.setTarget(mProgressBar);
        progressBarAnim.start();

        showProgressBar(false);

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        Log.d("Available app memory",""+am.getMemoryClass());

        // Click listener for the image field
        mImageView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                }
        );

        // Click listener for upload btn
        mSubmitCase.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        if(validator() && canUpload){
                            imageName+=System.currentTimeMillis()+"";
                            EditText mCaseid = (EditText)findViewById(R.id.case_id);
                            String caseid = mCaseid.getText().toString();
                            EditText mDescription = (EditText)findViewById(R.id.case_description);
                            String description = mDescription.getText().toString();
                            communication.uploadPicture(imageData,token,Integer.parseInt(caseid),description,imageName);
                        }
                    }
                }
        );
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (mCurrentPhotoPath!=null) {
                addPictureToView();
            }
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("imgPath", mCurrentPhotoPath);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        mCurrentPhotoPath = savedInstanceState.getString("imgPath");
        if(mCurrentPhotoPath != null){
            addPictureToView();
        }
    }

    // Methods
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            Log.d("Photo file",""+photoFile);
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.junkiesoup.care4clean.fileprovider",
                        photoFile);
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    // Patch for bug on older Android versions
                    Context context = getApplicationContext();
                    List<ResolveInfo> resolvedIntentActivities = context.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                        String packageName = resolvedIntentInfo.activityInfo.packageName;

                        context.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d("Storage dir",""+storageDir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void addPictureToView(){
        //decoding the file into a bitmap
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath,opts);
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        // Determine downsizing factor, based on allocated memory available
        // - if allocated memory is lessthan or equal to 16 mb, use half size to be safe
        Log.d("Available memory",""+am.getMemoryClass());
        Log.d("Dimensions will be",10*am.getMemoryClass()+"x"+4*am.getMemoryClass());
        int dWidth = 10*am.getMemoryClass();
        int dHeight = 4*am.getMemoryClass();
        dWidth = (dWidth > 2048) ? 2048 : dWidth;
        dHeight = (dHeight > 2048) ? 2048 : dHeight;
        opts.inSampleSize = calculateInSampleSize(opts, dWidth, dHeight);
        opts.inJustDecodeBounds = false;
        Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,opts);

        int imageHeight = opts.outHeight;
        int imageWidth = opts.outWidth;
        String imageType = opts.outMimeType;
        Log.d("Original image","Type: "+imageType+"\nDimensions: "+imageWidth+"x"+imageHeight);

        imageData = imageBitmap;
        mImageView.setImageBitmap(imageBitmap);
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Calculate sample size based around required width and height
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    public void clearApp(Drawable placeholder){
        //MainActivity ac = new MainActivity();
        mImageView.setImageDrawable(getResources().getDrawable(R.drawable.image_placeholder));
        imageData = null;
        imageName = "imageName";
        mCurrentPhotoPath = null;
        ((EditText)mCaseDescription).setText("");
        //((EditText)mCaseId).setText(""); // Uncomment this line to also clear the "Case ID" field
    }
    public void clearApp(){
        clearApp(getResources().getDrawable(R.drawable.image_placeholder));
    }

    public void hideProgressBar(){
        progressOverlay.setVisibility(View.GONE);
    }
    public void removeSpinner(){
        if(imageData != null)
            mImageView.setImageBitmap(imageData);
        else
            mImageView.setImageDrawable(getResources().getDrawable(R.drawable.image_placeholder));
    }
    public void enableUpload(Boolean val){
        canUpload = val;
        Button mButton=(Button)findViewById(R.id.submit_case);
        mButton.setEnabled(val);
    }
    public void showProgressBar(Boolean val, String btnText){
        if(val){
            mCamInstructions.setVisibility((View.GONE));
            mProgressBar.setVisibility(View.VISIBLE);
            Button mButton=(Button)findViewById(R.id.submit_case);
            mButton.setText(btnText);
            mButton.setEnabled(false);
        } else {
            mCamInstructions.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            Button mButton=(Button)findViewById(R.id.submit_case);
            mButton.setText(getString(R.string.input_section_submit_button));
            mButton.setEnabled(true);
        }
    }
    public void showProgressBar(Boolean val){
        showProgressBar(val,getString(R.string.button_status_uploading));
    }
    public void showProgressBar(){
        showProgressBar(true);
    }

    public void createUser() {
        OkCancelInputDialog dialog = new OkCancelInputDialog(this,getString(R.string.create_user_dialog_title),getString(R.string.create_user_dialog_text))
        {
            @Override
            public void clickCancel() {
                super.clickCancel();
            }

            @Override
            public void clickOk() {
                if(getUserInput().length() > 0) {
                    communication.CreateUser(getUserInput(),token+=System.currentTimeMillis());
                    super.clickOk();
                } else {
                    // If the user left the input field blank
                    Toast toast = Toast.makeText(context,getString(R.string.create_user_dialog_toast_blankfield),Toast.LENGTH_LONG);
                }
            }
        };

        dialog.show();
    }
    // Validator method - validates login state (token existence), picture taken and input fields
    // Can be used to check if logged in only, without the rest of the checks, by disabling 'validateAll'
    private Boolean validator(Boolean validateAll){
        // First, check if user is even logged in (if not, open "create user" dialog)
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        if(!prefs.contains("token")){
            if(canUpload)
                // Prevent "create user" to popup if, in this case, a user is being created
                createUser();
            return false;
        }
        else if(!validateAll){
            return true;
        }

        // Secondly, field validation
        Boolean approved = true; // Return value
        String errMsg = getString(R.string.validation_empty_fields); // Variable error message

        // Check if there's a picture
        if(imageData == null){
            errMsg = getString(R.string.validation_missing_photo);
            approved = false;
        }

        // If there's a picture (meaning approved is still true), perform field validation
        if(approved){
            // Load shake animation
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

            // Case ID is empty
            EditText cID = (EditText) mCaseId;
            if(cID.length() == 0){
                Log.d("Validation","Case ID is empty!");
                approved = false;
                cID.startAnimation(shake);
                cID.setError(getString(R.string.validation_field_caseid_missing));
            }
            // Description is empty
            EditText cDE = (EditText) mCaseDescription;
            if(cDE.length() == 0){
                Log.d("Validation","Description is empty!");
                approved = false;
                cDE.startAnimation(shake);
                cDE.setError(getString(R.string.validation_field_description_missing));
            }
        }

        if(!approved){
            Toast valErr = Toast.makeText(context,errMsg,Toast.LENGTH_LONG);
            valErr.show();
        }
        return approved;
    }
    // Parameter-free override
    private Boolean validator(){
        return validator(true);
    }
    // Error messages, etc, for server errors
    public void invalidCaseId(){
        ((EditText)smCaseId).setError(getString(R.string.validation_field_caseid_invalid));
        Log.d("Invalid caseID","Error made it through!");
    }
}
