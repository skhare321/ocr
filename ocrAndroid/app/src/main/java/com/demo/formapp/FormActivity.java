package com.demo.formapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class FormActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int PERM_REQUEST_CODE = 2;
    private ProgressDialog progressDialog;
    private EditText etDateSubmission, etWeight, etVitALast;
    private Spinner spinnerOedema, spinnerReferral;
    private RadioGroup rgMotherCounseling, rgNightStay, rgAmoxicillinToday, rgVitA1st, rgSugar1st,
            rgAlbendazole7th, rgFolicAcidToday, rgFormulaGiven, rgPotassiumToday, rgMagnesiumToday,
            rgZincToday, rgMultivitamin, rgIron7th;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Initialize fields
        etDateSubmission = findViewById(R.id.et_date_submission);
        etWeight = findViewById(R.id.et_weight);
        etVitALast = findViewById(R.id.et_vit_a_last);
        spinnerOedema = findViewById(R.id.spinner_oedema);
        spinnerReferral = findViewById(R.id.spinner_referral);
        rgMotherCounseling = findViewById(R.id.rg_mother_counseling);
        rgNightStay = findViewById(R.id.rg_night_stay);
        rgAmoxicillinToday = findViewById(R.id.rg_amoxicillin_today);
        rgVitA1st = findViewById(R.id.rg_vit_a_1st);
        rgSugar1st = findViewById(R.id.rg_sugar_1st);
        rgAlbendazole7th = findViewById(R.id.rg_albendazole_7th);
        rgFolicAcidToday = findViewById(R.id.rg_folic_acid_today);
        rgFormulaGiven = findViewById(R.id.rg_formula_given);
        rgPotassiumToday = findViewById(R.id.rg_potassium_today);
        rgMagnesiumToday = findViewById(R.id.rg_magnesium_today);
        rgZincToday = findViewById(R.id.rg_zinc_today);
        rgMultivitamin = findViewById(R.id.rg_multivitamin);
        rgIron7th = findViewById(R.id.rg_iron_7th);

        // Setup Spinners
        ArrayAdapter<String> oedemaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"-Select-", "None", "+", "++", "+++"});
        spinnerOedema.setAdapter(oedemaAdapter);

        ArrayAdapter<String> referralAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"-Select-", "None", "Referred to Hospital", "Referred to Clinic"});
        spinnerReferral.setAdapter(referralAdapter);

        // Date Pickers
        setDatePicker(etDateSubmission);
        setDatePicker(etVitALast);

        // Scan Button
        findViewById(R.id.btn_scan_form).setOnClickListener(v -> checkPermissionsAndOpenCamera());
    }

    private void setDatePicker(EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(FormActivity.this, (view, year, month, day) -> {
                editText.setText(String.format("%02d-%02d-%d", day, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERM_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            File destination = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
            try (FileOutputStream fo = new FileOutputStream(destination)) {
                fo.write(bytes.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
                return;
            }
            new UploadAndProcessTask().execute(destination.getAbsolutePath());
        }
    }

    private class UploadAndProcessTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(FormActivity.this);
            progressDialog.setMessage("Processing OCR...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String filePath = params[0];
            String serverUrl = "http://10.0.2.2:8000/ocr"; // Replace with your local server URL
            try {
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;

                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + filePath + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                FileInputStream fis = new FileInputStream(filePath);
                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fis.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fis.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fis.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                fis.close();
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return response.toString(); // JSON string
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (jsonResponse != null) {
                try {
                    JSONObject json = new JSONObject(jsonResponse);
                    // Fill fields
                    if (json.has("date_of_submission")) etDateSubmission.setText(json.getString("date_of_submission"));
                    if (json.has("weight")) etWeight.setText(json.getString("weight"));
                    if (json.has("vitamin_a_last_given")) etVitALast.setText(json.getString("vitamin_a_last_given"));

                    // Spinners
                    if (json.has("bilateral_pitting_oedema")) {
                        String value = json.getString("bilateral_pitting_oedema");
                        for (int i = 0; i < spinnerOedema.getAdapter().getCount(); i++) {
                            if (value.equals(spinnerOedema.getAdapter().getItem(i))) {
                                spinnerOedema.setSelection(i);
                                break;
                            }
                        }
                    }
                    if (json.has("higher_facility_referral")) {
                        String value = json.getString("higher_facility_referral");
                        for (int i = 0; i < spinnerReferral.getAdapter().getCount(); i++) {
                            if (value.equals(spinnerReferral.getAdapter().getItem(i))) {
                                spinnerReferral.setSelection(i);
                                break;
                            }
                        }
                    }

                    // Radio Groups (example for all)
                    setRadioGroup(rgMotherCounseling, json.optString("mother_counseling"));
                    setRadioGroup(rgNightStay, json.optString("night_stay"));
                    setRadioGroup(rgAmoxicillinToday, json.optString("is_child_given_amoxicillin_today"));
                    setRadioGroup(rgVitA1st, json.optString("is_child_given_vit_a_on_1st_day"));
                    setRadioGroup(rgSugar1st, json.optString("is_child_given_sugar_solution_on_1st_day"));
                    setRadioGroup(rgAlbendazole7th, json.optString("is_child_given_albendazole_on_7th_day"));
                    setRadioGroup(rgFolicAcidToday, json.optString("is_child_given_folic_acid_today"));
                    setRadioGroup(rgPotassiumToday, json.optString("is_child_given_potassium_today"));
                    setRadioGroup(rgMagnesiumToday, json.optString("is_child_given_magnesium_today"));
                    setRadioGroup(rgZincToday, json.optString("is_child_given_zinc_today"));
                    setRadioGroup(rgMultivitamin, json.optString("is_child_given_multi_vitamin_syrup"));
                    setRadioGroup(rgIron7th, json.optString("is_child_given_iron_from_7th_day"));

                    // Special for formula given (not yes/no, but select radio by text)
                    if (json.has("formula_given")) {
                        String value = json.getString("formula_given");
                        for (int i = 0; i < rgFormulaGiven.getChildCount(); i++) {
                            RadioButton rb = (RadioButton) rgFormulaGiven.getChildAt(i);
                            if (value.equals(rb.getText().toString())) {
                                rb.setChecked(true);
                                break;
                            }
                        }
                    }

                    Toast.makeText(FormActivity.this, "Form filled from OCR", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormActivity.this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(FormActivity.this, "Server error", Toast.LENGTH_SHORT).show();
            }
        }

        private void setRadioGroup(RadioGroup rg, String value) {
            if ("Yes".equalsIgnoreCase(value)) {
                ((RadioButton) rg.getChildAt(0)).setChecked(true);
            } else if ("No".equalsIgnoreCase(value)) {
                ((RadioButton) rg.getChildAt(1)).setChecked(true);
            }
        }
    }
}