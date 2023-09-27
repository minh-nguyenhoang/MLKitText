package com.example.mlkittext;

import static com.google.android.gms.tasks.Tasks.await;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    //UI Views
    private MaterialButton inputImageBtn;
    private MaterialButton recognizedTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    private MaterialButton switchLangBtn;
    private MaterialButton translateBtn;

    private GraphicOverlay mGraphicOverlay;

    public String leftLang;
    public String rightLang;
    private String leftLangCode;
    private String rightLangCode;


    //TAG
    private static final String TAG = "MAIN_TAG";

    private Uri imageUri = null;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;



    private String[] cameraPermissions;
    private String[] storagePermissions;
    private ProgressDialog progressDialog;

    //TextRecognizer
    private TextRecognizer textRecognizer;
    private Translator textTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizedTextBtn = findViewById(R.id.recognizedTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);
        translateBtn = findViewById(R.id.translateBtn);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);


        //init array of permissions required for camera-gallery
        cameraPermissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);


        //Translator part
        switchLangBtn = findViewById(R.id.switchLangBtn);


        leftLang = ((MaterialButton)findViewById(R.id.leftLanguage)).getText().toString();
        rightLang = ((MaterialButton)findViewById(R.id.rightLanguage)).getText().toString();

        leftLangCode = TranslateLanguage.ENGLISH;
        rightLangCode = TranslateLanguage.VIETNAMESE;

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(leftLangCode)
                        .setTargetLanguage(rightLangCode)
                        .build();

        textTranslator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        textTranslator.downloadModelIfNeeded(conditions);

        switchLangBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MaterialButton)findViewById(R.id.leftLanguage)).setText(rightLang);
                ((MaterialButton)findViewById(R.id.rightLanguage)).setText(leftLang);

                leftLang = ((MaterialButton)findViewById(R.id.leftLanguage)).getText().toString();
                rightLang = ((MaterialButton)findViewById(R.id.rightLanguage)).getText().toString();

                String varLangCode = leftLangCode.toString();
                leftLangCode = rightLangCode.toString();
                rightLangCode = varLangCode.toString();


                TranslatorOptions options =
                        new TranslatorOptions.Builder()
                                .setSourceLanguage(leftLangCode)
                                .setTargetLanguage(rightLangCode)
                                .build();

                textTranslator = Translation.getClient(options);
                textTranslator.downloadModelIfNeeded(conditions);




            }
        });



        //handle click, shown input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                showInputImageDialog();
            }
        });

        recognizedTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){

                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                }
                else {
                    recognizeTextFromImage();

                }
            }
        });

        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                }
                else {
                    translateText();
                }
            }
        });
    }

    private void recognizeTextFromImage() {
        Log.d(TAG, "recognizedTextFromImage: ");

        progressDialog.setMessage("Recognizing text...");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            progressDialog.setMessage("Recognizing Text...");

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            progressDialog.dismiss();

//                            processTextRecognitionResult(text);
//                            String recognizedText = text.getText();
//                            Log.d(TAG, "onSuccess: recognizedText:"+ recognizedText);
//
//                            recognizedTextEt.setText(recognizedText);

                            List<Text.TextBlock> blocks = text.getTextBlocks();
                            if (blocks.size() == 0) {
                                Toast.makeText(MainActivity.this, "No text found!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String tmpStr = new String();

                            for (int i = 0; i < blocks.size(); i++) {
                                Text.TextBlock block = blocks.get(i);
                                tmpStr += "Block " + Integer.toString(i) + "\n" + block.getText() + "\n\n";
                            }
                            recognizedTextEt.setText(tmpStr);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });


        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(MainActivity.this, "Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");


        popupMenu.show();


        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                int id = menuItem.getItemId();
                if  (id == 1){
                    Log.d(TAG, "onMenuItemClick: CameraClicked...");
                    if (checkCameraPermissions()){
                        pickImageCamera();
                    }
                    else{
                        requestCameraPermissions();
                    }
                }
                else if (id == 2) {
                    Log.d(TAG, "onMenuItemClick: GalleryClicked...");
                    if (checkStoragePermission()){
                        pickImageGallery();
                    }
                    else {
                        requestStoragePermission();
                    }

                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if picked
                    if (result.getResultCode() == Activity.RESULT_OK){
                        //image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+ imageUri);
                        //set to imageview
                        imageIv.setImageURI(imageUri);

                    }
                    else{
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }

    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);

    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image is already taken from camera

                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){

        boolean result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions(){
        boolean cameraResult = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    //handle permission results

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){

                        pickImageCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }

            }
            break;
            case STORAGE_REQUEST_CODE:{

                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){

                        pickImageGallery();
                    }
                    else{
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }

            }
            break;
        }
    }

    private void translateText(){
        Log.d(TAG, "translateText: ");

        progressDialog.setMessage("Translating text...");
        progressDialog.show();

        try {
            String recognizedText = recognizedTextEt.getText().toString();
            if (recognizedText == null || recognizedText.isEmpty() || recognizedText.trim().isEmpty()){

                try {
                    InputImage inputImage = InputImage.fromFilePath(this, imageUri);

                    progressDialog.setMessage("Recognizing Text...");

                    Task<Text> textTaskResult = textRecognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text text) {

                                    progressDialog.dismiss();
                                    String recognizedText = text.getText();
                                    Log.d(TAG, "onSuccess: recognizedText:"+ recognizedText);
                                    progressDialog.setMessage("Translating text...");
                                    progressDialog.show();
                                    doTranslation(recognizedText);

                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    progressDialog.dismiss();
                                    Log.e(TAG, "onFailure: ", e);
                                    Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });


                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "recognizeTextFromImage: ", e);
                    Toast.makeText(MainActivity.this, "Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_LONG).show();
                }



            }
            else{
                doTranslation(recognizedText);
            }

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(MainActivity.this, "Failed translating text due to "+e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }
    private void doTranslation(String recognizedText){
        Task<String> translateTaskResult = textTranslator.translate(recognizedText)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        progressDialog.dismiss();
                        recognizedTextEt.setText(s);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        progressDialog.dismiss();
                        Log.e(TAG, "onFailure: ", e);
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_LONG).show();

                    }
                });
    }

    private void processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(MainActivity.this, "No text found!", Toast.LENGTH_SHORT).show();
            return;
        }

        mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);

                }
            }
        }
    }

}
