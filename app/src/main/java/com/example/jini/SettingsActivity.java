package com.example.jini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    private Button saveBtn;
    private EditText userNameET, userBioET;
    private ImageView profileImageView;
    private static int GalleryPick = 1;
    private StorageReference userProfileImgRef;
    private Uri ImageUri;
    private String downloadedUrl;
    private DatabaseReference userRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userProfileImgRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        saveBtn = findViewById(R.id.save_settings_btn);
        userNameET = findViewById(R.id.username_settings);
        userBioET = findViewById(R.id.bio_settings);
        profileImageView = findViewById(R.id.settings_profile_image);
        progressDialog= new ProgressDialog(this);


        retriveUserData();

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GalleryPick);


            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GalleryPick && resultCode == RESULT_OK && data != null) {

            ImageUri = data.getData();
            profileImageView.setImageURI(ImageUri);
        }
    }


    private void saveUserData() {
        final String getUserName = userNameET.getText().toString();
        final String getUserBio = userBioET.getText().toString();

        if (ImageUri == null) {

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")) {
                        saveInfoOnlyWithoutImage();
                    } else {
                        Toast.makeText(SettingsActivity.this, "please enter Image", Toast.LENGTH_SHORT).show();
                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else if (getUserName.equals("")) {
            Toast.makeText(this, "Enter Name first", Toast.LENGTH_SHORT).show();
        } else if (getUserBio.equals("")) {
            Toast.makeText(this, "Enter Bio ", Toast.LENGTH_SHORT).show();
        }
        else {
                progressDialog.setTitle("Account Settings");
                progressDialog.setMessage("please wait...");
                progressDialog.show();

            final StorageReference filePath = userProfileImgRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            final UploadTask uploadTask = filePath.putFile(ImageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful())
                        throw task.getException();
                    downloadedUrl = filePath.getDownloadUrl().toString();


                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isComplete()) {
                        downloadedUrl = task.getResult().toString();

                        HashMap<String, Object> profileMap = new HashMap<>();
                        profileMap.put("Uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("name", getUserName);
                        profileMap.put("status", getUserBio);
                        profileMap.put("image", downloadedUrl);
                        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                                updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(SettingsActivity.this, ContactsActivity.class);
                                    startActivity(intent);
                                    finish();

                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Updated....", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });

                    }

                }
            });

        }

    }

    private void saveInfoOnlyWithoutImage() {
        final String getUserName = userNameET.getText().toString();
        final String getUserBio = userBioET.getText().toString();
        if (getUserName.equals("")) {
            Toast.makeText(this, "Enter Name first", Toast.LENGTH_SHORT).show();
        } else if (getUserBio.equals("")) {
            Toast.makeText(this, "Enter Bio ", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("please wait...");
            progressDialog.show();


            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("Uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name", getUserName);
            profileMap.put("status", getUserBio);

            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                    updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(SettingsActivity.this, ContactsActivity.class);
                        startActivity(intent);
                        finish();

                        progressDialog.dismiss();
                        Toast.makeText(SettingsActivity.this, "Updated....", Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }
    }

    private void retriveUserData(){
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String imageDb = dataSnapshot.child("image").getValue().toString();
                            String userNameDb = dataSnapshot.child("name").getValue().toString();
                            String statusDb = dataSnapshot.child("status").getValue().toString();

                            userNameET.setText(userNameDb);
                            userBioET.setText(statusDb);

                            Picasso.get().load(imageDb).placeholder(R.drawable.profile_image).into(profileImageView);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
