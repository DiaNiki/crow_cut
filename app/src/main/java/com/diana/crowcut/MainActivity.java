package com.diana.crowcut;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    protected static final int RESULT_LOAD_IMAGE = 1;
    protected static final int PERMISSION_REQUEST_WRITE_STORAGE = 2;
    protected Bitmap imageData = null;
    protected ImageView imageView = null;
    protected float imageViewWidth;
    protected float imageViewHeight;
    protected DrawableImageView drawingArea = null;

    @SuppressLint({"DefaultLocale", "StaticFieldLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        drawingArea = findViewById(R.id.drawingCanvas);
        imageView.addOnLayoutChangeListener((view, i, i1, i2, i3, i4, i5, i6, i7) -> {
            imageViewWidth = view.getWidth();
            imageViewHeight = view.getHeight();
        });

        final Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(view -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        });

        final Button processButton = findViewById(R.id.processImage);
        processButton.setOnClickListener(view -> {
            if (imageData != null)
            {
                final Switch parallelSwitch = findViewById(R.id.parallelSwitch);
                final ProgressBar progressIndicator = findViewById(R.id.progressIndicator);
                new AsyncTask<Void, Void, Bitmap>(){
                    long startTime;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        startTime = System.currentTimeMillis();
                        progressIndicator.setVisibility(View.VISIBLE);
                        SetControlsEnabled(false);
                    }

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return GrowCut.run(MainActivity.this, imageData,
                                drawingArea.getBitmap(imageData.getWidth(), imageData.getHeight()),
                                parallelSwitch.isChecked());
                    }

                    @Override
                    protected void onPostExecute(Bitmap result) {
                        super.onPostExecute(result);
                        progressIndicator.setVisibility(View.GONE);
                        SetControlsEnabled(true);
                        long endTime = System.currentTimeMillis();
                        Toast.makeText(MainActivity.this,
                                String.format("Calculated in %d ms", endTime - startTime),
                                Toast.LENGTH_LONG).show();
                        imageView.setImageBitmap(result);
                        drawingArea.clear();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Please select an image first", Toast.LENGTH_LONG).show();
            }
        });

        final Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            if (imageData != null)
            {
                SaveCurrentImage();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Nothing to save", Toast.LENGTH_LONG).show();
            }
        });

        final RadioGroup drawingSelector = findViewById(R.id.drawingSelector);
        drawingSelector.setOnCheckedChangeListener((radioGroup, i) -> {
            RadioButton drawingOption = findViewById(i);
            drawingArea.setColor(drawingOption.getCurrentTextColor());
            if (imageData != null)
            {
                drawingArea.setIsEnabled(true);
            }
        });
        ((RadioButton)findViewById(R.id.foregroundOption)).setTextColor(GrowCut.FOREGROUND_COLOUR);
        ((RadioButton)findViewById(R.id.backgroundOption)).setTextColor(GrowCut.BACKGROUND_COLOUR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null)
        {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);

                imageData = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(imageData);
                drawingArea.resize(imageData.getWidth(), imageData.getHeight(), imageViewWidth, imageViewHeight);
                drawingArea.clear();
            } catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            SaveCurrentImage();
        }
    }

    protected void SetControlsEnabled(Boolean isEnabled) {
        drawingArea.setIsEnabled(isEnabled);
        findViewById(R.id.foregroundOption).setEnabled(isEnabled);
        findViewById(R.id.backgroundOption).setEnabled(isEnabled);
        findViewById(R.id.parallelSwitch).setEnabled(isEnabled);
        findViewById(R.id.loadButton).setEnabled(isEnabled);
        findViewById(R.id.processImage).setEnabled(isEnabled);
        findViewById(R.id.saveButton).setEnabled(isEnabled);
    }

    protected void SaveCurrentImage()
    {
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_WRITE_STORAGE);
            return; // Callback will try to save again
        }

        try
        {
            File root = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                    "/CrowCut/".replace("/", File.separator));
            root.mkdirs();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            File imageFile = new File(root,
                    "Img-" + dateFormat.format(Calendar.getInstance().getTime()) + ".png");
            OutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(this, "Successfully saved!", Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Error saving the image", Toast.LENGTH_LONG).show();
        }
    }
}
