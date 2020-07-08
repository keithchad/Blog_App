package com.chad.photoblogapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private static final int MAX_LENGTH = 100;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private ProgressBar newPostProgress;
    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;

    private Uri postImageUri = null;
    private String current_user_id;
    private Bitmap compressedImageFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);


        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        current_user_id = FirebaseAuth.getInstance().getUid();

        newPostImage = (ImageView) findViewById(R.id.new_post_image);
        newPostDesc = (EditText) findViewById(R.id.new_post_desc);
        newPostBtn = (Button) findViewById(R.id.post_btn);
        newPostProgress = (ProgressBar) findViewById(R.id.new_post_progress);

        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .setMinCropResultSize(512, 512)
                        .start(NewPostActivity.this);
            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                final String desc = newPostDesc.getText().toString();

                if(!TextUtils.isEmpty(desc) && postImageUri != null) {

                 newPostProgress.setVisibility(View.VISIBLE);

                 final String randomName = random();

                    storageReference.child("post_images").child( randomName + ".jpg").putFile(postImageUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                    Task<Uri> downloadUri = taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            File newImageFile = new File(postImageUri.getPath());

                                            try {

                                                compressedImageFile = new Compressor(NewPostActivity.this).compressToBitmap(newImageFile);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                            byte[] thumbData = baos.toByteArray();

                                            UploadTask uploadTask = storageReference.child("post_images/thumbs")
                                                    .child(randomName + ".jpg").putBytes(thumbData);

                                            uploadTask

                                            Map<String, Object> postMap = new HashMap<>();
                                            postMap.put("image_url", uri.toString());
                                            postMap.put("desc", desc);
                                            postMap.put("user_id", current_user_id);
                                            postMap.put("timestamp", FieldValue.serverTimestamp());



                                            firebaseFirestore.collection("Posts").document().set(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful()) {

                                                        Toast.makeText(NewPostActivity.this, " Post was Added", Toast.LENGTH_LONG).show();
                                                        Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
                                                        startActivity(mainIntent);
                                                        finish();

                                                    }else {

                                                        String error = task.getException().getMessage();
                                                        Toast.makeText(NewPostActivity.this, " FIRESTORE Error" + error, Toast.LENGTH_SHORT).show();

                                                        newPostProgress.setVisibility(View.INVISIBLE);

                                                    }

                                                }
                                            });

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(NewPostActivity.this, "Failed to Upload", Toast.LENGTH_SHORT).show();

                                        }
                                    });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(NewPostActivity.this, " Image Error", Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {

                postImageUri = result.getUri();
                newPostImage.setImageURI(postImageUri);

            }else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();

            }

        }
    }

    public static String random() {
        Random generator =  new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for(int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) +32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

}