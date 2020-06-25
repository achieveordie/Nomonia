package com.example.nomonia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URI;

import static android.graphics.ImageDecoder.decodeBitmap;


public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button search_button;
    Button reset_button;
    TextView prediction_text;
    private static final int PICK_IMAGE = 100;
    Uri imageUri;
    Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.image_box);
        search_button = (Button)findViewById(R.id.button_search);
        reset_button = (Button)findViewById(R.id.button_reset);
        prediction_text = (TextView)findViewById(R.id.prediction_text);

        classifier = new Classifier(, Classifier.Device.CPU, -1);

        search_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openGallery();
            }
        });

        reset_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                resetImage();
            }
        });
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    private void resetImage(){
        imageView.setImageURI(null);
        prediction_text.setText(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            imageView.setImageURI(imageUri);

            ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
            Bitmap bitmap = null;
            try {
                bitmap = ImageDecoder.decodeBitmap(source);
            } catch (IOException e) {
                e.printStackTrace();
            }
            prediction_text.setText(classifier.recognizeImage(bitmap));
            prediction_text.setVisibility(View.VISIBLE);
        }
    }
}
