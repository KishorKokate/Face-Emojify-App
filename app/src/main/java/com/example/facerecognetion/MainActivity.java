package com.example.facerecognetion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    ImageView imageView;
    Button button;
    FloatingActionButton share_btn, save_btn, clear_btn;
    TextView textView, Note;

    private String mTempPhotoPath;
    private String mFinalPhotoPath = "";
    private Bitmap mResultsBitmap;
    private Boolean mImageIsSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        button = findViewById(R.id.emojify_button);
        share_btn = findViewById(R.id.share_button);
        save_btn = findViewById(R.id.save_button);
        clear_btn = findViewById(R.id.clear_button);
        textView = findViewById(R.id.title_text_view);
        Note = findViewById(R.id.note);

        Timber.plant(new Timber.DebugTree());
    }

    public void emojifyMe(View view) {
        //Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //if you do not have permission, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSION);
        } else {
            //Launch the camera if the permission exists
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Permossion Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

    }


    //Creates a temporary image file and capture a picture to store in it.
    private void launchCamera() {
        //Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException e) {
                e.printStackTrace();
            }

            /// Cotinue only if the file was successfully crreated
            if (photoFile != null) {
                // get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                //get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);

                //Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                //Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //If the image capture activity was called and was successful
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            processAndSetImage();
        } else {
            //Otherwise deelete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }

    }

    private void processAndSetImage() {

        //Toggle visibility of the views
        button.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        Note.setVisibility(View.GONE);
        save_btn.setVisibility(View.VISIBLE);
        share_btn.setVisibility(View.VISIBLE);
        clear_btn.setVisibility(View.VISIBLE);

        //Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);

        // Detect the faces and overlay the appropriate emoji
        mResultsBitmap = Emojifier.detectFacesandOverlayEmoji(this, mResultsBitmap);


        //Set the new bitmap to the ImageView
        imageView.setImageBitmap(mResultsBitmap);
        mImageIsSaved = false;

    }

    public void Saveimage(View view) {
        //Delete the temporary file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        //Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
        if (mImageIsSaved == false) {
            mFinalPhotoPath = BitmapUtils.saveImage(this, mResultsBitmap);
            mImageIsSaved = true;
        } else {
            Toast.makeText(this, "Image is already saved", Toast.LENGTH_SHORT).show();
        }

    }

    public void ClearImage(View view) {
        //clear the image and toggle the view visibility
        imageView.setImageResource(0);
        button.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        Note.setVisibility(View.VISIBLE);
        save_btn.setVisibility(View.GONE);
        share_btn.setVisibility(View.GONE);
        clear_btn.setVisibility(View.GONE);

        //Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        launchCamera();


    }

    public void ShareImage(View view) {
        //Delete the temporary file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        //Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
        if (mImageIsSaved == false) {
            mFinalPhotoPath = BitmapUtils.saveImage(this, mResultsBitmap);
            mImageIsSaved = true;
        } else {
            Toast.makeText(this, "Image is already saved", Toast.LENGTH_SHORT).show();
        }

        //Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);
        BitmapUtils.shareImage(this, mFinalPhotoPath);


    }

  /*  @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d("TAG", "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions3
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.d("TAG", "storage & Camera services permission granted");
                        launchCamera();
                        // process the normal flow
                        //else any one or both the permissions are not granted
                    } else {
                        Log.d("TAG", "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showDialogOK("SMS and Location Services Permission required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    emojifyMe();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    break;
                                            }
                                        }
                                    });
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                        else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                    .show();
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }

    private void emojifyMe() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {
            //if you do not have permission, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSION);
        } else {
            //Launch the camera if the permission exists
            launchCamera();
        }

    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }


   */
}