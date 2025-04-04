package com.dm.smart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dm.smart.items.Record;
import com.dm.smart.items.Subject;

import java.util.Date;

public class DBAdapter {
    public static final String SUBJECT_ID = "id";
    public static final String SUBJECT_NAME = "name";
    public static final String SUBJECT_CONFIG = "config";
    public static final String SUBJECT_SCHEME = "scheme";
    public static final String SUBJECT_TIMESTAMP = "timestamp";
    public static final String RECORD_ID = "id";
    public static final String RECORD_SUBJECT_ID = "subject_id";
    public static final String RECORD_CONFIG = "config";
    public static final String RECORD_N = "n";
    public static final String RECORD_SENSATIONS = "sensations";
    public static final String RECORD_TIMESTAMP = "timestamp";
    private static final String DATABASE_NAME = "smart.db";
    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_TABLE_SUBJECTS = "subjects";
    private static final String DATABASE_TABLE_RECORDS = "records";
    private final DBOpenHelper dbHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context _context) {
        dbHelper = new DBOpenHelper(_context
        );
    }

    public void close() {
        db.close();
    }

    public void open() throws SQLiteException {
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = dbHelper.getReadableDatabase();
        }
    }

    void deleteSubject(long subject_id) {
        // Delete all records of the subject
        db.delete(DATABASE_TABLE_RECORDS, RECORD_SUBJECT_ID + "=" + subject_id, null);
        // Delete the subject
        db.delete(DATABASE_TABLE_SUBJECTS, SUBJECT_ID + "=" + subject_id, null);
    }

    void deleteRecord(long record_id) {
        db.delete(DATABASE_TABLE_RECORDS, RECORD_ID + "=" + record_id, null);
    }

    Cursor getSubjectById(long subject_id) {
        return db.query(DATABASE_TABLE_SUBJECTS,
                new String[]{SUBJECT_ID, SUBJECT_NAME, SUBJECT_CONFIG, SUBJECT_SCHEME, SUBJECT_TIMESTAMP},
                SUBJECT_ID + "='" + subject_id + "'",
                null, null, null, RECORD_ID);
    }

    Cursor getAllSubjects() {
        return db.query(DATABASE_TABLE_SUBJECTS,
                new String[]{SUBJECT_ID, SUBJECT_NAME, SUBJECT_CONFIG, SUBJECT_SCHEME, SUBJECT_TIMESTAMP},
                null, null, null, null, SUBJECT_ID + " DESC");
    }

    Cursor getRecordsSingleSubject(long subject_id) {
        return db.query(DATABASE_TABLE_RECORDS,
                new String[]{RECORD_ID, RECORD_SUBJECT_ID, RECORD_CONFIG, RECORD_N, RECORD_SENSATIONS, RECORD_TIMESTAMP},
                (RECORD_SUBJECT_ID + "='" + subject_id + "'"),
                null, null, null, RECORD_ID);
    }

    public long insertSubject(Subject new_subject) {
        ContentValues cv_new_subject = new ContentValues();
        cv_new_subject.put(SUBJECT_NAME, new_subject.getName());
        cv_new_subject.put(SUBJECT_CONFIG, new_subject.getConfig());
        cv_new_subject.put(SUBJECT_SCHEME, new_subject.getBodyScheme());
        long timestamp = new Date().getTime();
        cv_new_subject.put(SUBJECT_TIMESTAMP, timestamp);
        return db.insert(DATABASE_TABLE_SUBJECTS, null, cv_new_subject);
    }

    public void insertRecord(Record new_record) {
        ContentValues cv_new_record = new ContentValues();
        cv_new_record.put(RECORD_SUBJECT_ID, new_record.getSubjectId());
        cv_new_record.put(RECORD_CONFIG, new_record.getConfig());
        cv_new_record.put(RECORD_N, new_record.getN());
        cv_new_record.put(RECORD_SENSATIONS, new_record.getSensations());
        long timestamp = new Date().getTime();
        cv_new_record.put(SUBJECT_TIMESTAMP, timestamp);
        db.insert(DATABASE_TABLE_RECORDS, null, cv_new_record);
    }

    public void updateSubject(Subject selectedSubject) {
        ContentValues updateSubject = new ContentValues();
        updateSubject.put(SUBJECT_NAME, selectedSubject.getName());
        updateSubject.put(SUBJECT_CONFIG, selectedSubject.getConfig());
        updateSubject.put(SUBJECT_SCHEME, selectedSubject.getBodyScheme());
        long timestamp = new Date().getTime();
        updateSubject.put(SUBJECT_TIMESTAMP, timestamp);
        db.update(DATABASE_TABLE_SUBJECTS, updateSubject, SUBJECT_ID + "="
                + selectedSubject.getId(), null);
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {

        private static final String DATABASE_CREATE_1 = "create table "
                + DATABASE_TABLE_SUBJECTS + " (" + SUBJECT_ID
                + " integer primary key, " + SUBJECT_NAME
                + " string, " + SUBJECT_CONFIG + " string, " + SUBJECT_SCHEME + " string, "
                + SUBJECT_TIMESTAMP + " long);";

        private static final String DATABASE_CREATE_2 = "create table " + DATABASE_TABLE_RECORDS +
                " (" + RECORD_ID + " integer primary key, " + RECORD_SUBJECT_ID +
                " integer, " + RECORD_CONFIG + " string, " + RECORD_N + " integer, " + RECORD_SENSATIONS + " string, "
                + RECORD_TIMESTAMP + " long, " +
                "FOREIGN KEY(" + RECORD_SUBJECT_ID + ") REFERENCES " + DATABASE_TABLE_SUBJECTS + "(" + SUBJECT_ID + "));";

        DBOpenHelper(Context context) {
            super(context, DBAdapter.DATABASE_NAME, null, DBAdapter.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase _db) {
            _db.execSQL(DATABASE_CREATE_1);
            _db.execSQL(DATABASE_CREATE_2);
        }

        @Override
        public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
                              int _newVersion) {
            Log.w("diaDBAdapter", "Upgrading from version " + _oldVersion
                    + " to " + _newVersion
                    + ", which will destroy some old data");

            // Drop the old table
            _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SUBJECTS);
            _db.execSQL(DATABASE_CREATE_1);
            _db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RECORDS);
            _db.execSQL(DATABASE_CREATE_2);
        }
    }
}