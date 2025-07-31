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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class FormActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int PERM_REQUEST_CODE = 2;
    private static final String TAG = "FormActivity";
    private ProgressDialog progressDialog;
    private EditText etDateSubmission, etWeight, etVitALast, formName, havingTravelHistory, iecGiven,
                        isTreatmentGiven, lmpDate, referralReason;
    private Spinner spinnerOedema, spinnerReferral;
    private RadioGroup rgMotherCounseling, rgNightStay, rgAmoxicillinToday, rgVitA1st, rgSugar1st,
            rgAlbendazole7th, rgFolicAcidToday, rgFormulaGiven, rgPotassiumToday, rgMagnesiumToday,
            rgZincToday, rgMultivitamin;

    private View overlay;
    private ProgressBar loader;
    private Map<String, View> viewMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        viewMap = new HashMap<>();
        // Initialize fields
        etDateSubmission = findViewById(R.id.et_date_submission);
        etWeight = findViewById(R.id.et_weight);
        etVitALast = findViewById(R.id.et_vit_a_last);
        formName = findViewById(R.id.form_name);
        lmpDate = findViewById(R.id.lmp_date);

        isTreatmentGiven = findViewById(R.id.is_treatment_given);
        iecGiven = findViewById(R.id.iec_given);
        havingTravelHistory = findViewById(R.id.having_travel_history);
        referralReason = findViewById(R.id.referral_reason);

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
//        rgIron7th = findViewById(R.id.rg_iron_7th);

        overlay = findViewById(R.id.overlay);
        loader  = findViewById(R.id.progress_loader);

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

            File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir == null) {
                Toast.makeText(this, "Unable to access pictures directory", Toast.LENGTH_SHORT).show();
                return;
            }
            File destination = new File(picturesDir, "temp.jpg");

            try (FileOutputStream fo = new FileOutputStream(destination)) {
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fo);
                fo.flush();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
                return;
            }
            new     UploadAndProcessTask().execute(destination.getAbsolutePath());
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
            File imageFile = new File(filePath);
            Log.d(TAG, "doInBackground: filePath=" + filePath + ", exists=" + imageFile.exists());

            String serverUrl = "http://10.127.130.245:8866/ocr";
            try {
                String lineEnd    = "\r\n";
                String twoHyphens = "--";
                String boundary   = "*****";

                HttpURLConnection conn = (HttpURLConnection)new URL(serverUrl).openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes(
                        "Content-Disposition: form-data; name=\"image\";filename=\""
                                + imageFile.getName() + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                FileInputStream fis = new FileInputStream(imageFile);
                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "doInBackground: HTTP response code=" + responseCode);

                if (responseCode == 200) {
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String jsonResponse = sb.toString();
                    Log.d(TAG, "doInBackground: raw JSON response=" + jsonResponse);
                    return jsonResponse;
                } else {
                    Log.e(TAG, "doInBackground: non‑OK response: " + responseCode);
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "doInBackground: exception uploading image", e);
                return null;
            }
        }
        private String normalize(String s) {
            return s.replaceAll("[\\s_\\-]+", "").toLowerCase();
        }
        private Map<String,String> buildJsonKeyMap(JSONObject json) {
            Map<String,String> keyMap = new HashMap<>();
            Iterator<String> it = json.keys();
            while(it.hasNext()) {
                String orig = it.next();
                keyMap.put(normalize(orig), orig);
            }
            return keyMap;
        }
        private void autofillFields(JSONObject json) throws JSONException {
            Map<String,String> keyMap = buildJsonKeyMap(json);

            for(Map.Entry<String,View> entry : viewMap.entrySet()) {
                String normKey = entry.getKey();
                if(!keyMap.containsKey(normKey)) continue;

                String jsonKey = keyMap.get(normKey);
                String value  = json.optString(jsonKey);
                View   view   = entry.getValue();

                if(view instanceof EditText) {
                    ((EditText)view).setText(value);

                } else if(view instanceof Spinner) {
                    Spinner sp = (Spinner)view;
                    ArrayAdapter<?> adapter = (ArrayAdapter<?>)sp.getAdapter();
                    for(int i=0; i<adapter.getCount(); i++) {
                        if(value.equals(adapter.getItem(i))) {
                            sp.setSelection(i);
                            break;
                        }
                    }

                } else if(view instanceof RadioGroup) {
                    setRadioGroup((RadioGroup)view, value);
                }
            }
        }
        @Override
        protected void onPostExecute(String jsonResponse) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (jsonResponse == null) {
                Toast.makeText(FormActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject json = new JSONObject(jsonResponse);
                autofillFields(json);
                Toast.makeText(FormActivity.this, "Form filled from OCR", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(FormActivity.this, "Error parsing or filling form", Toast.LENGTH_SHORT).show();
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