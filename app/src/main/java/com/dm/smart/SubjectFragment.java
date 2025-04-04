package com.dm.smart;

import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_DELETE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHARE;
import static com.dm.smart.RecyclerViewAdapterRecords.RECORD_SHOW_IMAGE;
import static com.dm.smart.RecyclerViewAdapterSubjects.SUBJECT_CHANGE_NAME;
import static com.dm.smart.RecyclerViewAdapterSubjects.SUBJECT_DELETE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dm.smart.items.Record;
import com.dm.smart.items.Subject;
import com.dm.smart.ui.elements.CustomAlertDialogs;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubjectFragment extends Fragment {

    RecyclerViewAdapterSubjects adapterSubjects;
    RecyclerViewAdapterRecords adapterRecords;
    boolean showNames;
    SharedPreferences sharedPref;
    private ArrayList<Subject> subjects;
    private ArrayList<Record> records;

    static Subject extractSubjectFromTheDB(Cursor cursor) {
        @SuppressLint("Range") int id = cursor.getInt(cursor.
                getColumnIndex(DBAdapter.SUBJECT_ID));
        @SuppressLint("Range") String name =
                cursor.getString(cursor.
                        getColumnIndex(com.dm.smart.DBAdapter.SUBJECT_NAME));
        @SuppressLint("Range") String config =
                cursor.getString(cursor.
                        getColumnIndex(DBAdapter.SUBJECT_CONFIG));
        @SuppressLint("Range") String scheme =
                cursor.getString(cursor.
                        getColumnIndex(DBAdapter.SUBJECT_SCHEME));
        @SuppressLint("Range") long timestamp = cursor.getLong(cursor.
                getColumnIndex(DBAdapter.SUBJECT_TIMESTAMP));
        return new Subject(id, name, config, scheme, timestamp);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read shared preference to shown subjects' names
        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        showNames = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        View mView = inflater.inflate(R.layout.fragment_subject, container, false);

        // Spinner for body scheme selection
        Spinner spinner = mView.findViewById(R.id.spinner_body_scheme);
        boolean customConfig = sharedPref.getBoolean(getString(R.string.sp_custom_config), false);
        String configPath = sharedPref.getString(getString(R.string.sp_custom_config_path), "");
        String configName = sharedPref.getString(getString(R.string.sp_selected_config), "Built-in");
        Configuration configuration = new Configuration(configPath, configName);
        try {
            configuration.formConfig("neutral");
        } catch (IOException e) {
            // switch off custom config if it is not found
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.sp_custom_config), false);
            editor.apply();
            customConfig = false;
        }

        ArrayAdapter<CharSequence> adapterBodyScheme;
        if (customConfig) {
            // get the config path from shared preferences
            adapterBodyScheme = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, configuration.getBodySchemes());
        } else {
            adapterBodyScheme = ArrayAdapter.createFromResource(requireContext(),
                    R.array.schemes, android.R.layout.simple_spinner_item);
        }

        adapterBodyScheme.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterBodyScheme);

        // RecyclerView for Subjects
        RecyclerView listViewSubjects = mView.findViewById(R.id.list_view_subjects);
        listViewSubjects.setLayoutManager(new LinearLayoutManager(requireActivity()));
        subjects = new ArrayList<>();
        adapterSubjects = new RecyclerViewAdapterSubjects(requireActivity(), subjects, showNames);
        listViewSubjects.setAdapter(adapterSubjects);
        adapterSubjects.setClickListener((int position) -> {
            int previousSelectedSubjectId = adapterSubjects.selectedSubjectPosition;
            adapterSubjects.selectedSubjectPosition = position;
            adapterSubjects.notifyItemChanged(position);
            adapterSubjects.notifyItemChanged(previousSelectedSubjectId);
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            Cursor cursorSingleSubject =
                    DBAdapter.getSubjectById(adapterSubjects.getItem(position).getId());
            cursorSingleSubject.moveToFirst();
            MainActivity.currentlySelectedSubject = extractSubjectFromTheDB(cursorSingleSubject);
            cursorSingleSubject.close();
            DBAdapter.close();
            populateListRecords(false);
        });

        // RecyclerView for Records
        RecyclerView listViewRecords = mView.findViewById(R.id.list_view_records);
        listViewRecords.setLayoutManager(new LinearLayoutManager(requireActivity()));
        records = new ArrayList<>();
        adapterRecords = new RecyclerViewAdapterRecords(requireActivity(), records);
        listViewRecords.setAdapter(adapterRecords);

        // EditText and Button for adding new Subjects
        EditText edittextPatientName = mView.findViewById(R.id.edittext_subject_name);
        Button buttonAddPatients = mView.findViewById(R.id.button_add_subject);
        boolean finalCustomConfig = customConfig;
        buttonAddPatients.setOnClickListener((View view) -> {
            if (edittextPatientName.getText().toString().isEmpty()) {
                Toast toast = Toast.makeText(getContext(), getString(R.string.toast_empty_name), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                DBAdapter DBAdapter = new DBAdapter(requireActivity());
                DBAdapter.open();
                Subject new_subject;
                if (finalCustomConfig) {
                    new_subject =
                            new Subject(edittextPatientName.getText().toString(), configName, (String) spinner.getSelectedItem());
                } else {
                    new_subject =
                            new Subject(edittextPatientName.getText().toString(), "Built-in", (String) spinner.getSelectedItem());
                }

                new_subject.setId((int) DBAdapter.insertSubject(new_subject));
                DBAdapter.close();
                // create a folder for the images
                File folder = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + new_subject.getId());
                if (!folder.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    folder.mkdirs();
                }

                MainActivity.currentlySelectedSubject = new_subject;
                edittextPatientName.setText("");
                populateListSubjects();
                populateListRecords(false);
            }
        });
        populateListSubjects();
        populateListRecords(false);
        return mView;
    }

    private void populateListSubjects() {
        subjects.clear();
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        Cursor cursorSubjects = DBAdapter.getAllSubjects();
        updateArraySubjects(cursorSubjects);
        DBAdapter.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void populateListRecords(boolean noSelection) {
        records.clear();
        if (!noSelection) {
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            Cursor cursorRecords =
                    DBAdapter.getRecordsSingleSubject(MainActivity.currentlySelectedSubject.getId());
            updateArrayRecords(cursorRecords);
            DBAdapter.close();
        } else {
            adapterRecords.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArraySubjects(Cursor cursorSubjects) {
        if (cursorSubjects.moveToFirst())
            do {
                Subject newSubject = extractSubjectFromTheDB(cursorSubjects);
                subjects.add(newSubject);
            } while (cursorSubjects.moveToNext());
        Integer idToSelect = MainActivity.currentlySelectedSubject.getId();
        Subject selected = subjects.stream().filter(carnet ->
                idToSelect.equals(carnet.getId())).findFirst().orElse(null);
        adapterSubjects.selectedSubjectPosition = subjects.indexOf(selected);
        adapterSubjects.notifyDataSetChanged();
        cursorSubjects.close();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateArrayRecords(Cursor cursorRecords) {
        if (cursorRecords.moveToFirst())
            do {
                @SuppressLint("Range") int id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_ID));
                @SuppressLint("Range") int subject_id = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_SUBJECT_ID));
                @SuppressLint("Range") String config = cursorRecords.getString(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_CONFIG));
                @SuppressLint("Range") int n = cursorRecords.getInt(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_N));
                @SuppressLint("Range") String sensations = cursorRecords.getString(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_SENSATIONS));
                @SuppressLint("Range") long timestamp = cursorRecords.getLong(cursorRecords.
                        getColumnIndex(DBAdapter.RECORD_TIMESTAMP));
                records.add(0, new Record(id, subject_id, config, n, sensations, timestamp));
            } while (cursorRecords.moveToNext());
        adapterRecords.notifyDataSetChanged();
        cursorRecords.close();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == SUBJECT_DELETE) {
            showDeleteSubjectDialog();
            return true;
        } else if (item.getItemId() == SUBJECT_CHANGE_NAME) {
            // if the option to show names is not on, show the password dialog
            showChangeNameDialog();
            if (!showNames) {
                android.app.AlertDialog alertDialog =
                        CustomAlertDialogs.requestPassword(getActivity(), null, null, null);
                alertDialog.show();
                Objects.requireNonNull(alertDialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
            }
            return true;
        } else if (item.getItemId() == RECORD_DELETE) {
            showDeleteRecordDialog();
            return true;
        } else if (item.getItemId() == RECORD_SHARE) {
            shareSensations();
            return true;
        } else if (item.getItemId() == RECORD_SHOW_IMAGE) {
            try {
                showMergedImageDialog();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void shareSensations() {
        // Load all images from the directory
        Record selectedRecord = adapterRecords.getItem(adapterRecords.selectedRecordPosition);
        Subject selectedSubject = subjects.get(adapterSubjects.selectedSubjectPosition);
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubject.getId()
                + "/" + selectedRecord.getN());

        File[] imageFiles = directory.listFiles((dir, name) -> name.contains("fig"));

        ArrayList<Uri> imageUris = new ArrayList<>();
        if (imageFiles != null) {
            for (File imageFile : imageFiles) {
                Uri imageUri = FileProvider.getUriForFile(requireActivity(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        imageFile);
                imageUris.add(imageUri);
            }
        }

        // take a txt file with sensations
        File textFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubject.getId()
                + "/" + selectedRecord.getN() + "/" + selectedSubject.getId() + "_" + selectedRecord.getN() + ".txt");
        Uri textSensationsUri = FileProvider.getUriForFile(requireActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                textFile);
        imageUris.add(textSensationsUri);

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("image/png");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

        // Make a date
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedRecord.getTimestamp());
        SimpleDateFormat formatter =
                new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateString = formatter.format(cal.getTime());

        // Write the colored list of sensations to email text
        List<Integer> colors = Arrays.stream(requireActivity().getResources().
                getIntArray(R.array.colors_symptoms)).boxed().collect(Collectors.toList());
        String textSensations = selectedRecord.getSensations();
        ArrayList<String> listSensations = new ArrayList<>(Arrays.asList(textSensations.split(";")));
        StringBuilder textSensationsColored = new StringBuilder();
        for (int i = 0; i < listSensations.size(); i++) {
            listSensations.set(i, "<font color=" + colors.get(i) + ">" + listSensations.get(i) + "</font>");
            textSensationsColored.append(listSensations.get(i)).append("<br/>");
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(textSensationsColored.toString(), Html.FROM_HTML_MODE_LEGACY));
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                getResources().getString(R.string.menu_export_title, selectedSubject.getName(), dateString));
        startActivity(Intent.createChooser(shareIntent, "SEND IMAGE"));
    }

    public void showDeleteSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_delete_subject)).
                setPositiveButton(getResources().getString(R.string.dialog_yes),
                        (dialog, id) -> {
                            Subject selectedSubject =
                                    adapterSubjects.getItem(adapterSubjects.selectedSubjectPosition);
                            DBAdapter DBAdapter = new DBAdapter(requireActivity());
                            DBAdapter.open();
                            DBAdapter.deleteSubject(selectedSubject.getId());
                            DBAdapter.close();
                            subjects.remove(selectedSubject);
                            if (!subjects.isEmpty()) {
                                MainActivity.currentlySelectedSubject = subjects.get(0);
                                populateListSubjects();
                                populateListRecords(false);
                            } else {
                                populateListSubjects();
                                populateListRecords(true);
                            }
                            // remove the folder with the images
                            File folder = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubject.getId());
                            if (folder.exists()) {
                                File[] subFolders = folder.listFiles();
                                if (subFolders != null) {
                                    for (File subFolder : subFolders) {
                                        File[] files = subFolder.listFiles();
                                        if (files != null) {
                                            for (File file : files) {
                                                //noinspection ResultOfMethodCallIgnored
                                                file.delete();
                                            }
                                        }
                                        //noinspection ResultOfMethodCallIgnored
                                        subFolder.delete();
                                    }
                                }
                                //noinspection ResultOfMethodCallIgnored
                                folder.delete();
                            }

                        })
                .setNegativeButton(getResources().getString(R.string.dialog_no),
                        (dialog, id) -> {
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        @SuppressLint("InflateParams") View alertView =
                getLayoutInflater().inflate(R.layout.alert_change_name, null);
        EditText editTextName = alertView.findViewById(R.id.edit_text_name);
        editTextName.setText(adapterSubjects.getItem(adapterSubjects.selectedSubjectPosition).getName());
        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_cancel).setOnClickListener(v -> dialog.dismiss());
        alertView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            Subject selectedSubject =
                    adapterSubjects.getItem(adapterSubjects.selectedSubjectPosition);
            DBAdapter DBAdapter = new DBAdapter(requireActivity());
            DBAdapter.open();
            selectedSubject.setName(editTextName.getText().toString());
            DBAdapter.updateSubject(selectedSubject);
            DBAdapter.close();
            populateListSubjects();
            dialog.dismiss();
        });
        dialog.show();
    }

    public void showDeleteRecordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(getResources().getString(R.string.dialog_delete_record)).
                setPositiveButton(getResources().getString(R.string.dialog_yes),
                        (dialog, id) -> {
                            Record selectedRecord =
                                    adapterRecords.getItem(adapterRecords.selectedRecordPosition);
                            DBAdapter DBAdapter = new DBAdapter(requireActivity());
                            DBAdapter.open();
                            DBAdapter.deleteRecord(selectedRecord.getId());
                            DBAdapter.close();
                            populateListRecords(false);
                            // remove the folder with the images
                            File folder = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedRecord.getSubjectId()
                                    + "/" + selectedRecord.getN());
                            if (folder.exists()) {
                                File[] files = folder.listFiles();
                                if (files != null) {
                                    for (File file : files) {
                                        //noinspection ResultOfMethodCallIgnored
                                        file.delete();
                                    }
                                }
                                //noinspection ResultOfMethodCallIgnored
                                folder.delete();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.dialog_no),
                        (dialog, id) -> {
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("ResourceType")
    public void showMergedImageDialog() throws NoSuchFieldException, IllegalAccessException {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View alertView = getLayoutInflater().inflate(R.layout.alert_image, null);
        ImageView imageViewBody = alertView.findViewById(R.id.image_view_body);
        Record selectedRecord = adapterRecords.getItem(adapterRecords.selectedRecordPosition);
        DBAdapter DBAdapter = new DBAdapter(requireActivity());
        DBAdapter.open();
        int selectedSubjectId = selectedRecord.getSubjectId();

        // Get the directory containing the images
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS) + "/SMaRT/" + selectedSubjectId
                + "/" + selectedRecord.getN());

        // Get an array of files with "fig" in the name
        File[] imageFiles = directory.listFiles((dir, name) -> name.contains("fig"));

        // Create a list to store all the image paths
        List<String> imagePaths = new ArrayList<>();
        if (imageFiles != null) {
            for (File imageFile : imageFiles) {
                imagePaths.add(imageFile.getAbsolutePath());
            }
        }
        if (imagePaths.isEmpty()) {
            return;
        }

        // Load the first image
        Bitmap firstImage = BitmapFactory.decodeFile(imagePaths.get(0));
        imageViewBody.setImageBitmap(firstImage);

        // Create a counter to keep track of the current image
        final int[] currentImageIndex = {0};

        // Set the onClickListener to cycle through the images
        imageViewBody.setOnClickListener(v -> {
            currentImageIndex[0] = (currentImageIndex[0] + 1) % imagePaths.size();
            Bitmap nextImage = BitmapFactory.decodeFile(imagePaths.get(currentImageIndex[0]));
            imageViewBody.setImageBitmap(nextImage);
        });

        builder.setView(alertView);
        AlertDialog dialog = builder.create();
        alertView.findViewById(R.id.button_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE);
        boolean show_names = sharedPref.getBoolean(getString(R.string.sp_show_names), false);
        adapterSubjects.setShowNames(show_names);
        populateListSubjects();
        populateListRecords(false);

    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.saveCurrentlySelectedSubjectId();
    }

}