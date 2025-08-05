package com.demo.formapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FormActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_FULL    = 42;
    private static final int REQUEST_PICK_GALLERY   = 84;
    private static final int PERM_REQUEST_CAMERA    = 100;

    private String photoFilePath, ocrType;
    private Uri photoUri;

    private EditText dateOfSubmission, weight, etVitALast,
            formName, havingTravelHistory, iecGiven,
            isTreatmentGiven, lmpDate, referralReason;
    private RadioGroup motherCounseling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        ocrType = OcrConfig.getOcrType();
        Log.d("FormActivity", "OCR type: " + ocrType);

        bindViews();
        setDatePicker(etVitALast);

        findViewById(R.id.btn_scan_form)
                .setOnClickListener(v -> checkCameraPermissionAndOpen());

        findViewById(R.id.btn_pick_gallery)
                .setOnClickListener(v -> pickImageFromGallery());
    }

    private void bindViews() {
        dateOfSubmission = findViewById(R.id.et_date_submission);
        weight = findViewById(R.id.et_weight);
        etVitALast = findViewById(R.id.et_vit_a_last);
        formName = findViewById(R.id.form_name);
        havingTravelHistory  = findViewById(R.id.having_travel_history);
        iecGiven = findViewById(R.id.iec_given);
        isTreatmentGiven = findViewById(R.id.is_treatment_given);
        lmpDate = findViewById(R.id.lmp_date);
        referralReason = findViewById(R.id.referral_reason);
        motherCounseling = findViewById(R.id.rg_mother_counseling);
    }

    private void setDatePicker(EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(
                    FormActivity.this,
                    (view, year, month, day) ->
                            editText.setText(String.format("%02d-%02d-%d", day, month + 1, year)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.CAMERA },
                    PERM_REQUEST_CAMERA
            );
        } else {
            openCameraFullRes();
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERM_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraFullRes();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCameraFullRes() {
        try {
            File photoFile = createImageFile();
            photoFilePath = photoFile.getAbsolutePath();
            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile
            );
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, REQUEST_CAMERA_FULL);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String filename = "ocr_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(filename, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA_FULL && resultCode == RESULT_OK) {
            uploadImage(new File(photoFilePath));
        } else if (requestCode == REQUEST_PICK_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                handleImageUri(uri);
            }
        }
    }

    private void handleImageUri(Uri uri) {
        try {
            File tmp = createImageFile();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            uploadImage(tmp);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage(File photoFile) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Processing OCR…");
        pd.setCancelable(false);
        pd.show();

        RequestBody reqFile = RequestBody.create(photoFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData(
                "image", photoFile.getName(), reqFile
        );

        OcrApi service = ApiClient.getClient(ocrType).create(OcrApi.class);
        service.extractKeyValues(body)
                .enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(
                            Call<Map<String, String>> call,
                            Response<Map<String, String>> resp
                    ) {
                        pd.dismiss();
                        if (resp.isSuccessful() && resp.body() != null) {
                            try {
                                JSONObject json = new JSONObject(resp.body());
                                View[] fields = {
                                        formName, havingTravelHistory, iecGiven,
                                        isTreatmentGiven, lmpDate, referralReason,
                                        dateOfSubmission, weight, motherCounseling
                                };
                                FormFieldMapper.fill(FormActivity.this, fields, json);
                                Toast.makeText(
                                        FormActivity.this,
                                        "Form filled from OCR",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(
                                        FormActivity.this,
                                        "Parsing error",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        } else {
                            Toast.makeText(
                                    FormActivity.this,
                                    "Server error",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        pd.dismiss();
                        Toast.makeText(
                                FormActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
