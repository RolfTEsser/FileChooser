package de.floresse.fc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.floresse.mylibrary.activity.FileChooser;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Intent intent = new Intent(this, FileChooser.class);
        Intent intent = new Intent(this, FileChooser.class);
        startActivity(intent);
    }
}

