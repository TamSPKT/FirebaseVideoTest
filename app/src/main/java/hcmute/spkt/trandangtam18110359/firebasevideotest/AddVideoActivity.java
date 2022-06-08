package hcmute.spkt.trandangtam18110359.firebasevideotest;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddVideoActivity extends AppCompatActivity {
    private static final int VIDEO_PICK_GALLERY_CODE = 100;
    private static final int VIDEO_PICK_CAMERA_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private String[] cameraPermissions;
    private Uri videoUri;

    private ProgressDialog progressDialog;

    private ActionBar actionBar;
    private VideoView videoView;
    private Button uploadBtn;
    private FloatingActionButton pickBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add new video");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        videoView = findViewById(R.id.uploadVideoView);
        uploadBtn = findViewById(R.id.uploadButton);
        pickBtn = findViewById(R.id.pickVideoFloatingButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setMessage("Uploading video...");
        progressDialog.setCanceledOnTouchOutside(false);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoUri == null) {
                    Toast.makeText(AddVideoActivity.this, "Must pick a video before upload", Toast.LENGTH_LONG).show();
                    return;
                }
                UploadVideoToFirebase();
            }
        });
        pickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PickVideoDialog();
            }
        });
    }

    private void UploadVideoToFirebase() {
        progressDialog.show();
        String timestamp = String.format("%d", System.currentTimeMillis());
        String filePath = String.format("Videos/video_%s", timestamp);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePath);
        storageReference.putFile(videoUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();
                        if (uriTask.isSuccessful()) {
                            // Add video detail to firebase database (Realtime Database)
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("id", timestamp);
                            hashMap.put("timestamp", timestamp);
                            hashMap.put("videoUri", String.format("%s", downloadUri));
                            Log.d("VideoURI", String.format("%s", downloadUri));

                            DatabaseReference reference = FirebaseDatabase
                                    .getInstance("https://fir-videotest-c29b8-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                    .getReference("Videos");
                            reference.child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            progressDialog.dismiss();
                                            Toast.makeText(AddVideoActivity.this, "Video uploaded.", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            Toast.makeText(AddVideoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddVideoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void RequestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean CheckCameraPermission() {
        boolean r1 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean r2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED;
        return r1 && r2;
    }

    private void PickVideoFromGallery() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select video"), VIDEO_PICK_GALLERY_CODE);
    }

    private void PickVideoFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, VIDEO_PICK_CAMERA_CODE);
    }

    private void PickVideoDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Video from")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) { // Camera
                            if (!CheckCameraPermission()) {
                                RequestCameraPermission();
                            } else {
                                PickVideoFromCamera();
                            }
                        } else if (i == 1) { // Gallery
                            PickVideoFromGallery();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        PickVideoFromCamera();
                    } else {
                        Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_LONG).show();
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void SetVideoToVideoView() {
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        videoView.setMediaController(mediaController);
        videoView.setVideoURI(videoUri);
        videoView.requestFocus();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.pause();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == VIDEO_PICK_GALLERY_CODE || requestCode == VIDEO_PICK_CAMERA_CODE) {
                videoUri = data.getData();
                SetVideoToVideoView();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}