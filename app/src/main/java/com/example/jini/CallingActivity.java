package com.example.jini;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.sql.Ref;
import java.util.HashMap;

public class CallingActivity extends Activity {

    private TextView nameContact,txt;
    private ImageView profileImage;
    private ImageView acceptCallBtn;
    private String receiverUserID="", receiverUsername="",receiverUserImage="";
    private String senderUserID="", senderUsername="",senderUserImage="",flag="";
    private String callingId="", ringingId="";
    private DatabaseReference usersRef;
    private String status="";

    private MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        senderUserID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiverUserID= getIntent().getExtras().getString("visit_user_id").toString();
        nameContact= findViewById(R.id.username_calling);
        profileImage= findViewById(R.id.profile_image_calling);
        ImageView cancelCallBtn = findViewById(R.id.cancel_call);
        acceptCallBtn = findViewById(R.id.make_call);
        txt= findViewById(R.id.txt);



        usersRef= FirebaseDatabase.getInstance().getReference().child("users");

        mediaPlayer =MediaPlayer.create(this,R.raw.ringtone);
        
       cancelCallBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v)
           {
               mediaPlayer.stop();
               flag="clicked";
               cancelCallingUser();
           }
       });

       acceptCallBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v)
           {
               mediaPlayer.stop();
               acceptCallingUser();
               finishAffinity();


           }
       });

        getAndSetReceiverProfileInfo();


    }




    @Override
    protected void onStart()
    {
        super.onStart();
        mediaPlayer.start();
        //************************************


        usersRef.child(receiverUserID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if ( !flag.equals("clicked") && !dataSnapshot.hasChild("Calling")&&!dataSnapshot.hasChild("Ringing"))
                        {
                            final HashMap<String, Object> callingInfo = new HashMap<>();
                            callingInfo.put("calling", receiverUserID);

                            usersRef.child(senderUserID).child("Calling")
                                    .updateChildren(callingInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                final HashMap<String, Object> ringingInfo = new HashMap<>();
                                                ringingInfo.put("Uid", receiverUserID);
                                                ringingInfo.put("name", receiverUsername);
                                                ringingInfo.put("image", receiverUserImage);
                                                ringingInfo.put("ringing", senderUserID);

                                                usersRef.child(receiverUserID).child("Ringing")
                                                        .updateChildren(ringingInfo);
                                            }
                                        }
                                    });

                        }

                        else if (flag.equals("clicked"))
                        {
                            Toast.makeText(CallingActivity.this, "Call Already disconnected", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    private void getAndSetReceiverProfileInfo() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(receiverUserID).exists())
                {

                    receiverUserImage= dataSnapshot.child(receiverUserID).child("image").getValue().toString();
                    receiverUsername= dataSnapshot.child(receiverUserID).child("name").getValue().toString();

                    nameContact.setText(receiverUsername);
                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.profile_image).into(profileImage);
                }

                if (dataSnapshot.child(senderUserID).exists())
                {
                    senderUserImage= dataSnapshot.child(senderUserID).child("image").getKey().toString();
                    senderUsername= dataSnapshot.child(senderUserID).child("name").getKey().toString();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(senderUserID).hasChild("Ringing")&& !dataSnapshot.child(senderUserID).hasChild("Calling"))
                {
                    acceptCallBtn.setVisibility(View.VISIBLE);
                    txt.setVisibility(View.GONE);

                }

                if (dataSnapshot.child(receiverUserID).child("Ringing").hasChild("picked"))
                {
                    mediaPlayer.stop();
                    Intent intent = new Intent(CallingActivity.this,VideoActivity.class);
                    finish();
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void acceptCallingUser()
    {
                usersRef.child(senderUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                   @Override
                   public void onDataChange(DataSnapshot dataSnapshot) {
                       if (dataSnapshot.child("Ringing").exists())
                       {

                           final HashMap<String ,Object> callingPickUpMao = new HashMap<>();
                           callingPickUpMao.put("picked", "picked");

                           usersRef.child(senderUserID).child("Ringing")
                                   .updateChildren(callingPickUpMao)
                                   .addOnCompleteListener(new OnCompleteListener<Void>() {
                                       @Override
                                       public void onComplete(@NonNull Task<Void> task) {
                                           if (task.isComplete())
                                           {
                                               startActivity(new Intent(CallingActivity.this, VideoActivity.class));
                                               finishAffinity();

                                           }
                                       }
                                   });



                       }

                       else
                       {
                           Toast.makeText(CallingActivity.this, "Call Already Ended", Toast.LENGTH_SHORT).show();


                           Intent in = new Intent(CallingActivity.this, RefreshActivity.class);
                          CallingActivity.this.finish();
                           startActivity(in);


                       }
                   }

                   @Override
                   public void onCancelled(DatabaseError databaseError) {

                   }
               });
    }

    private void cancelCallingUser()
    {
        //from sender side
        usersRef.child(senderUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("Calling"))
                {
                    usersRef.child(senderUserID).child("Calling")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists() && dataSnapshot.hasChild("calling"))
                                    {
                                        callingId= dataSnapshot.child("calling").getValue().toString();

                                        usersRef.child(callingId)
                                                .child("Ringing")
                                                .removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                        {
                                                            usersRef.child(senderUserID).child("Calling")
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            Intent i = new Intent(CallingActivity.this,RefreshActivity.class);
                                                                            CallingActivity.this.finish();
                                                                            startActivity(i);

                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }

                                    else
                                    {

                                        startActivity(new Intent(CallingActivity.this,RefreshActivity.class));
                                       CallingActivity.this.finish();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                }


                else if (dataSnapshot.hasChild("Ringing"))
                {
                    usersRef.child(senderUserID).child("Ringing")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists() && dataSnapshot.hasChild("ringing"))
                                    {
                                        ringingId= dataSnapshot.child("ringing").getValue().toString();

                                        usersRef.child(ringingId)
                                                .child("Calling")
                                                .removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                        {
                                                            usersRef.child(senderUserID).child("Ringing")
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            startActivity(new Intent(CallingActivity.this,RefreshActivity.class));
                                                                            finish();
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }

                                    else
                                    {
                                        startActivity(new Intent(CallingActivity.this, RefreshActivity.class));
                                        finish();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                }

                else
                {
                    startActivity(new Intent(CallingActivity.this,RefreshActivity.class));
                    finish();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }



        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CallingActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
