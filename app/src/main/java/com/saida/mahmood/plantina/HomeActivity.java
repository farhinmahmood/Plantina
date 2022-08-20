package com.saida.mahmood.plantina;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import static com.saida.mahmood.plantina.TempFile.image;

import java.io.FileDescriptor;
import java.io.IOException;

public class HomeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_CAMERA = 102;
    private static final int REQUEST_CODE_GALLERY = 103;
    Dialog dialogPicChoose;
    private LinearLayout plantBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        plantBtn = findViewById(R.id.plantLl);

        dialogPicChoose = new Dialog(HomeActivity.this);

        checkPermissionsAndGive();
        onClick();
    }

    private void checkPermissionsAndGive() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {

        } else {
            Toast.makeText(this, "Permissions need to run this app", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(HomeActivity.this, permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissionsAndGive();
    }

    private void onClick() {
        plantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsAndGive();
                } else {
                    openDialog();

                }

            }
        });

    }

    private void openDialog() {
        dialogPicChoose.setContentView(R.layout.custom_dialog_pic_choose);
        LinearLayout cameraBtn = dialogPicChoose.findViewById(R.id.cameraBtn);
        LinearLayout galleryBtn = dialogPicChoose.findViewById(R.id.openGalleryBtn);
        dialogPicChoose.show();
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraForAge();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGalleryForAge();
            }
        });


    }

    private void openGalleryForAge() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    private void openCameraForAge() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK){

                image = (Bitmap) data.getExtras().get("data");
                Intent imgIntent = new Intent(HomeActivity.this, PlantDetectActivity.class);
                startActivity(imgIntent);
            }
        }

        if (requestCode == REQUEST_CODE_GALLERY) {
            if (resultCode == Activity.RESULT_OK){

                Uri contentUri = data.getData();
                Bitmap img = uriToBitmap(contentUri);
                Intent imgIntent = new Intent(HomeActivity.this, PlantDetectActivity.class);
                image = img;
                startActivity(imgIntent);
            }
        }
    }

    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }



}