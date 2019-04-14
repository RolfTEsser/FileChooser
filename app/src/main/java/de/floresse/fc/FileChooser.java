package de.floresse.fc;


import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

//import com.jaiselrahman.filepicker.activity.FilePickerActivity;
//import com.jaiselrahman.filepicker.model.MediaFile;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

public class FileChooser extends ListActivity {

    private static final String TAG = "FileChooser";

    private static final int PERMISSIONS_REQUEST_CODE = 192;

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";

    private String EnvExtStor = "";

    private static final String ROOT = File.separator;

    public static final String START_PATH = "START_PATH";
    public static final String RESULT_PATH = "RESULT_PATH";
    private ArrayList<String> path = null;
    private TextView myPath;

    private Button selectButton;

    private LinearLayout layoutSelect;
    private InputMethodManager inputManager;
    private String parentPath;

    private String[] formatFilter = null;

    private boolean canSelectDir = true;
    private boolean canSelectFile = true;
    private boolean canSelectDownload = false;

    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    List<File> ftreffer = new ArrayList<>();

    private BroadcastReceiver mExternalStorageReceiver;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;

    private ArrayList<HashMap<String, Object>> mList;
    private SimpleAdapter fileList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // für start intent mit file / ohne fileprovider
        //StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        //StrictMode.setVmPolicy(builder.build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, " checking Permission ");
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, " Permission denied");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                }, PERMISSIONS_REQUEST_CODE);
                finish();
            }
        }

        setResult(RESULT_CANCELED, getIntent());
        //Log.i("MyMovies", "FileChooser : onCreate ");

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mList = new ArrayList<HashMap<String, Object>>();
        fileList = new SimpleAdapter(this, mList, R.layout.file_chooser_row, new String[]{
                ITEM_KEY, ITEM_IMAGE}, new int[]{R.id.fdrowtext, R.id.fdrowimage});


        setContentView(R.layout.file_chooser_main);
        myPath = (TextView) findViewById(R.id.path);
        myPath.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(false);
        selectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getIntent().putExtra(RESULT_PATH, (String) selectButton.getTag());
                setResult(RESULT_OK, getIntent());
                //Log.i("MyMovies", "FileChooser : select finish : " + selectedFile.getPath());
                finish();
            }
        });

        final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
        cancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                        		/*
                                setCreateVisible(v);

                                mFileName.setText("");
                                mFileName.requestFocus();
                                */
                finish();
            }
        });

        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);

        String startPath = getIntent().getStringExtra(START_PATH);

        setListAdapter(fileList);

        //if (currentPath == startPath) {
        //File externalDir = Environment.getExternalStorageDirectory();
        //currentPath = externalDir.getAbsolutePath();
        //currentPath = "/storage/8B22-1504/";
        //currentPath = "/mnt/runtime/write";

        Map<String, String> env = new HashMap<String, String>();
        env = System.getenv();
        for (String temp : env.keySet()) {
            //Log.i(TAG, "env " + temp + " " + env.get(temp));
            if (temp.equals("EXTERNAL_STORAGE")) {
                EnvExtStor = env.get(temp);            }
        }

        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter files only to those that can be "opened" and directly accessed
        // as a stream.
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Only show images.
        //intent.setType("*/*");
        //startActivityForResult(intent, 55);

        if (startPath==null || startPath.equals("") || startPath.equals("/")) {
            myPath.setText("");
        } else {
            myPath.setText(startPath);
        }
    }

    public void getRoot() {
        myPath.setText("");

        StorageManager sm = getSystemService(StorageManager.class);
        List<StorageVolume> lsv = sm.getStorageVolumes();
        Class<StorageVolume> clsv = StorageVolume.class;
        for (StorageVolume sv : lsv) {
            /*
            Log.i(TAG, "StorageVolume : " + sv.toString());
            Log.i(TAG, "              : " + sv.getDescription(this));
            Log.i(TAG, " emu prim rem : " + sv.isEmulated() + sv.isPrimary() + sv.isRemovable());
            Log.i(TAG, "        state : " + sv.getState());
            Log.i(TAG, "         Uuid : " + sv.getUuid());
            */
            try {
                //StorageVolume.getPathFile has @hide annotation ???
                Method getPathFile = clsv.getMethod("getPathFile");
                File file = (File) getPathFile.invoke(sv);
                //Log.i(TAG, "         path : " + file.getAbsolutePath() + "  canRead : " + file.canRead());
                if (file.canRead()) {
                    ftreffer.add(file);
                }
                /*
                Method getInternalPath = clsv.getMethod("getInternalPath");
                String path = (String) getInternalPath.invoke(sv);
                File fili = new File(path);
                Log.i(TAG, "internal path : " + path + "  canRead : " + fili.canRead());
                */
            } catch(NoSuchMethodException e) {
                Log.i(TAG, "exception : " + e);
            } catch(Exception e) {
                Log.i(TAG, "exception : " + e);
            }
        }

    }
    public void getRootVar1() {
        myPath.setText("");

        List<File> externalFilesDirs = new ArrayList<>();
        //externalFilesDirs.add(new File(File.separator));
        //externalFilesDirs.add(new File("/sdcard"));
        externalFilesDirs.add(new File("/mnt"));
        externalFilesDirs.add(Environment.getRootDirectory());
        Log.i(TAG, "root : " + Environment.getRootDirectory());
        //externalFilesDirs.add(Environment.getDataDirectory());
        //externalFilesDirs.add(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS));

        externalFilesDirs.addAll(Arrays.asList(getExternalFilesDirs(null)));

        for (File extDir : externalFilesDirs) {
            String sextDir = extDir.getAbsolutePath() + File.separator;
            int i = 0;
            int fromInd = 1;
            boolean treffer = false;
            while ((i = sextDir.indexOf(File.separator, fromInd))!=-1) {
                String tsextDir = sextDir.substring(0,i);
                File fextDir = new File(tsextDir);
                //Log.i(TAG, "externalFilesDirs " + fextDir);
                if (fextDir.canRead()) {
                    // Treffer
                    //Log.i(TAG, "externalFilesDirs / can read        " + fextDir);
                    if (!treffer) {
                        if (!ftreffer.contains(fextDir)) {
                            //Log.i(TAG, "externalFilesDirs / can read treffer " + fextDir);
                            ftreffer.add(fextDir);
                        }
                    }
                    treffer=true;
                } else {
                    treffer=false;
                }
                fromInd=i+1;
            }
        }

    }

    public void setSelection() {
        if (canSelectDir) {
            selectButton.setText("Select " + myPath.getText());
            selectButton.setTag(myPath.getText());
        }
        if (myPath.getText().equals("")) {
            selectButton.setEnabled(false);
        } else {
            selectButton.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 55 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = modifyPath(uri);
            //Log.i(TAG, "--- path " + path);
            if (path!=null) {
                File file = new File(path);
                if (file.canRead()) {
                    //Log.i(TAG, "--- can read " + path);
                    getIntent().putExtra(RESULT_PATH, path);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String modifyPath(final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat) {
            final String docId = uri.getPath();
            ////Log.i(TAG, "modifyPath " + docId);
            final String[] split = docId.split(":");
            final String type = split[0];
            //Log.i(TAG, "modifyPath s " + split[0]);
            //Log.i(TAG, "modifyPath s " + split[1]);

            if ("/tree/primary".equalsIgnoreCase(split[0])) {
                return EnvExtStor + "/" + split[1];
            } else {
                final String[] split1 = split[0].split("/tree/");
                //Log.i(TAG, "modifyPath s " + split1[0]);
                //Log.i(TAG, "modifyPath s " + split1[1]);
                if ("".equalsIgnoreCase(split1[0])) {
                    return "/storage" + "/" + split1[1] + "/" + split[1];
                }
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        startWatchingExternalStorage();

        if (myPath.getText().equals("")) {
            ftreffer.clear();
            getRoot();
        } else {
            File file = new File((String) myPath.getText());
            ftreffer.clear();
            ftreffer.addAll(Arrays.asList(file.listFiles()));
        }
        showDir(ftreffer, myPath.getText());

    }

    public void showDir(List<File> ftreffer, CharSequence parentDir) {

        mList.clear();
        setSelection();

        for (File treffer : ftreffer) {
            //Log.i(TAG,  "treffer: " + treffer.getParent() + " " + treffer.getName());
            String mlai = treffer.getAbsolutePath().substring(parentDir.length());
            int resid = 0;
            if (treffer.isDirectory()) {
                resid=R.drawable.folder;
            } else {
                if (treffer.isFile()) {
                    resid = R.drawable.file;
                } else {
                    if (!treffer.canRead()) {
                        resid = R.drawable.no_access;
                    } else {
                        resid = R.drawable.book_green;
                    }
                }
            }
            mListAddItem(mlai, resid);
        }

        fileList.notifyDataSetChanged();

    }

    @Override
    public void onPause() {
        super.onPause();
        stopWatchingExternalStorage();

    }

    @Override
    public void onBackPressed() {
        String myPathText = myPath.getText().toString();
        if (myPathText.equals("")) {
            super.onBackPressed();
        } else {
            ftreffer.clear();
            int i = myPathText.lastIndexOf("/");
            Log.i(TAG, "myPath onClick : " + myPathText + " " + i);
            if (i > 0) {
                myPath.setText(myPathText.substring(0,i));
                File file = new File(myPath.getText().toString());
                if (file.isDirectory()) {
                    if (file.canRead()) {
                        ftreffer.addAll(Arrays.asList(file.listFiles()));
                    } else {
                        alert(file, (String) getText(R.string.cant_read_folder));
                    }
                } else {
                    alert(file, "interner Fehler");
                }
            } else {
                getRoot();
            }
            showDir(ftreffer, myPath.getText());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        Log.i(TAG, "available : " + mExternalStorageAvailable);
        Log.i(TAG, "writeable : " + mExternalStorageWriteable);
        Log.i(TAG, "removeable : " + Environment.isExternalStorageRemovable());
        //handleExternalStorageState(mExternalStorageAvailable,
        //        mExternalStorageWriteable);
    }

    void startWatchingExternalStorage() {
        mExternalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "WatchingStorage: " + intent.getData());
                updateExternalStorageState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        registerReceiver(mExternalStorageReceiver, filter);
        updateExternalStorageState();
    }

    void stopWatchingExternalStorage() {
        /* */
        unregisterReceiver(mExternalStorageReceiver);
    }

    private void mListAddItem(String fileName, int imageId) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, fileName);
        item.put(ITEM_IMAGE, imageId);
        mList.add(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Log.i(TAG, "onListItemClick");

        File pos = ftreffer.get(position);
        if (!pos.isFile()) {
            if (pos.canRead()) {
                myPath.setText(pos.getAbsoluteFile().toString());
                ftreffer.clear();
                ftreffer.addAll(Arrays.asList(pos.listFiles()));
                showDir(ftreffer, myPath.getText());
            } else {
                alert(pos, (String) getText(R.string.cant_read_folder));
            }
        } else {
            try {
                String extension = android.webkit.MimeTypeMap
                        .getFileExtensionFromUrl(Uri.fromFile(pos).toString());
                String mimeType = android.webkit.MimeTypeMap
                        .getSingleton().getMimeTypeFromExtension(extension);
                Intent i = new Intent();
                i.setAction(android.content.Intent.ACTION_VIEW);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri apkURI = FileProvider.getUriForFile(
                        this,
                        this.getApplicationContext()
                                .getPackageName() + ".provider", pos);
                i.setDataAndType(apkURI, mimeType);
                //i.setData(apkURI);
                Log.i(TAG, "Uri : " + apkURI + " mime : " + mimeType);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                Log.i(TAG, "Activity not found Exception : " + e.toString());
                // net amol ignorieren
            }
        }

        Log.i(TAG, "onListItemClick: " + pos.getAbsolutePath());

    }

    public void alert(File file, String anzeigen) {
        new AlertDialog.Builder(this).setIcon(R.drawable.no_access)
                .setTitle("[" + file.getName() + "] " + anzeigen)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();

    }
}

