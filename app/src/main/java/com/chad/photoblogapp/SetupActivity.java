package com.chad.photoblogapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI = null;

    private EditText setup_name;
    private Button setupBtn;

    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private ProgressBar setupProgress;
    private FirebaseFirestore firebaseFirestore;

    private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        user_id = mAuth.getCurrentUser().getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setup_name = (EditText) findViewById(R.id.setup_name);
        setupBtn = (Button) findViewById(R.id.setup_btn);
        setupImage = findViewById(R.id.setup_image);
        setupProgress = (ProgressBar) findViewById(R.id.setup_progress);

        setupProgress.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

             if(task.isSuccessful()) {

                 if(task.getResult().exists()) {

                     String name  = task.getResult().getString("name");
                     String image  = task.getResult().getString("image");

                     mainImageURI = Uri.parse(image);

                     setup_name.setText(name);
                     RequestOptions placeholderRequest = new RequestOptions();
                     placeholderRequest.placeholder(R.drawable.profile_image);
                     Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);

                     Toast.makeText(SetupActivity.this, " Data Exists", Toast.LENGTH_SHORT).show();

                 }else {

                     Toast.makeText(SetupActivity.this, " Data Doesnt Exist", Toast.LENGTH_SHORT).show();
                 }

             }else {

                 String error = task.getException().getMessage();
                 Toast.makeText(SetupActivity.this, " FIRESTORE Retrieve Error" + error, Toast.LENGTH_SHORT).show();
             }

             setupProgress.setVisibility(View.INVISIBLE);
             setupBtn.setEnabled(true);

            }
        });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String user_name = setup_name.getText().toString();

                if(!TextUtils.isEmpty(user_name) && mainImageURI != null) {

                    user_id = mAuth.getCurrentUser().getUid();
                    setupProgress.setVisibility(View.VISIBLE);


                    storageReference.child("profile_images").child(user_id + ".jpg").putFile(mainImageURI)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                    Task<Uri> downloadUri = taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            System.out.println(uri.toString());

                                            Map<String, String>  userMap = new HashMap<>();
                                            userMap.put("name", user_name);
                                            userMap.put("image", uri.toString());

                                            firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful()) {

                                                        Toast.makeText(SetupActivity.this, " The User Settings Have been Updated", Toast.LENGTH_SHORT).show();
                                                        Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                                                        startActivity(mainIntent);
                                                        finish();

                                                    }else {

                                                        String error = task.getException().getMessage();
                                                        Toast.makeText(SetupActivity.this, " FIRESTORE Error" + error, Toast.LENGTH_SHORT).show();

                                                        setupProgress.setVisibility(View.INVISIBLE);

                                                    }

                                                }
                                            });

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(SetupActivity.this, "Failed to Upload", Toast.LENGTH_SHORT).show();

                                        }
                                    });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(SetupActivity.this, " Image Error", Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    }else {

                        BringImagePicker();

                    }

                }else {

                    BringImagePicker();

                }

            }
        });

    }

    private void BringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK) {

               mainImageURI = result.getUri();
               setupImage.setImageURI(mainImageURI);

            }else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();

            }

        }
    }
}