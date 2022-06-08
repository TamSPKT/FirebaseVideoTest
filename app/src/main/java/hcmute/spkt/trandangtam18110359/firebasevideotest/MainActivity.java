package hcmute.spkt.trandangtam18110359.firebasevideotest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ViewDataBinding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton addVideoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Videos");

        addVideoBtn = findViewById(R.id.addVideoFloatingButton);
        addVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddVideoActivity.class));
            }
        });
    }
}