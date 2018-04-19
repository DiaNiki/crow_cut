package com.diana.crowcut;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity
{
    protected static int RESULT_LOAD_IMAGE = 1;
    protected Bitmap imageData = null;
    protected ImageView imageView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        final Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        final Button processButton = findViewById(R.id.processImage);
        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageData != null)
                {
                    Bitmap result = ExtractImageObject(imageData);
                    imageView.setImageBitmap(result);
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please select an image first", Toast.LENGTH_LONG).show();
                }
            }
        });
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

                ImageView imageView = findViewById(R.id.imageView);
                imageData = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(imageData);
            } catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected Bitmap ExtractImageObject(Bitmap imageData)
    {
        final RenderScript rs = RenderScript.create(this);
        final ScriptC_GrowCut script = new ScriptC_GrowCut(rs);
        final Allocation inputAllocation = Allocation.createFromBitmap(rs, imageData);
        final Bitmap outputBitmap = imageData.copy(imageData.getConfig(), imageData.isMutable());
        final Allocation outputAllocation = Allocation.createFromBitmap(rs, outputBitmap);
        script.invoke_process(inputAllocation, outputAllocation);
        rs.destroy();
        return outputBitmap;
    }
}
