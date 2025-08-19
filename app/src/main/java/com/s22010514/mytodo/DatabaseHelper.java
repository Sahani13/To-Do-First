package com.s22010514.mytodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mytodo.db";
    private static final int DATABASE_VERSION = 4; // Incremented for user separation feature

    private static final String NOTES_TABLE_NAME = "notes_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "TITLE";
    public static final String COL_3 = "BODY";
    public static final String COL_4 = "USER_ID"; // Added user ID column

    private static final String TASKS_TABLE_NAME = "tasks_table";
    public static final String TASK_COL_1 = "ID";
    public static final String TASK_COL_2 = "TITLE";
    public static final String TASK_COL_3 = "DESCRIPTION";
    public static final String TASK_COL_4 = "DATE";
    public static final String TASK_COL_5 = "IS_COMPLETED";
    public static final String TASK_COL_6 = "USER_ID"; // Added user ID column

    // Location Tasks Table
    private static final String LOCATION_TASKS_TABLE_NAME = "location_tasks_table";
    public static final String LOC_TASK_COL_1 = "ID";
    public static final String LOC_TASK_COL_2 = "TITLE";
    public static final String LOC_TASK_COL_3 = "DESCRIPTION";
    public static final String LOC_TASK_COL_4 = "LOCATION_ADDRESS";
    public static final String LOC_TASK_COL_5 = "LATITUDE";
    public static final String LOC_TASK_COL_6 = "LONGITUDE";
    public static final String LOC_TASK_COL_7 = "NOTIFICATION_RADIUS";
    public static final String LOC_TASK_COL_8 = "NOTIFICATION_ENABLED";
    public static final String LOC_TASK_COL_9 = "CREATED_DATE";
    public static final String LOC_TASK_COL_10 = "USER_ID"; // Added user ID column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create notes table with USER_ID
        db.execSQL("CREATE TABLE " + NOTES_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TITLE TEXT, " +
                "BODY TEXT, " +
                "USER_ID TEXT NOT NULL)");

        // Create tasks table with USER_ID
        db.execSQL("CREATE TABLE " + TASKS_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TITLE TEXT, " +
                "DESCRIPTION TEXT, " +
                "DATE INTEGER, " +
                "IS_COMPLETED INTEGER DEFAULT 0, " +
                "USER_ID TEXT NOT NULL)");

        // Create location tasks table with USER_ID
        db.execSQL("CREATE TABLE " + LOCATION_TASKS_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TITLE TEXT, " +
                "DESCRIPTION TEXT, " +
                "LOCATION_ADDRESS TEXT, " +
                "LATITUDE REAL, " +
                "LONGITUDE REAL, " +
                "NOTIFICATION_RADIUS INTEGER, " +
                "NOTIFICATION_ENABLED INTEGER, " +
                "CREATED_DATE INTEGER, " +
                "USER_ID TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add location tasks table for version 2
            db.execSQL("CREATE TABLE IF NOT EXISTS " + LOCATION_TASKS_TABLE_NAME + " (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "TITLE TEXT, " +
                    "DESCRIPTION TEXT, " +
                    "LOCATION_ADDRESS TEXT, " +
                    "LATITUDE REAL, " +
                    "LONGITUDE REAL, " +
                    "NOTIFICATION_RADIUS INTEGER, " +
                    "NOTIFICATION_ENABLED INTEGER, " +
                    "CREATED_DATE INTEGER)");
        }
        if (oldVersion < 3) {
            // Add IS_COMPLETED column for version 3
            db.execSQL("ALTER TABLE " + TASKS_TABLE_NAME + " ADD COLUMN IS_COMPLETED INTEGER DEFAULT 0");
        }
        if (oldVersion < 4) {
            // Add USER_ID columns for version 4 (user separation)
            db.execSQL("ALTER TABLE " + NOTES_TABLE_NAME + " ADD COLUMN USER_ID TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TASKS_TABLE_NAME + " ADD COLUMN USER_ID TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + LOCATION_TASKS_TABLE_NAME + " ADD COLUMN USER_ID TEXT DEFAULT ''");
        }
    }

    // NOTES METHODS - Updated with user filtering
    public boolean insertNote(String title, String body, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, title);
        contentValues.put(COL_3, body);
        contentValues.put(COL_4, userId);
        long result = db.insert(NOTES_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getAllNotes(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + NOTES_TABLE_NAME + " WHERE USER_ID = ?";
        return db.rawQuery(query, new String[]{userId});
    }

    public boolean deleteNoteById(String id, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(NOTES_TABLE_NAME, "ID = ? AND USER_ID = ?", new String[]{id, userId});
        return result > 0;
    }

    public boolean updateNoteById(int id, String title, String body, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, title);
        contentValues.put(COL_3, body);
        int result = db.update(NOTES_TABLE_NAME, contentValues, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }

    // TASKS METHODS - Updated with user filtering
    public boolean insertTask(String title, String description, long date, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TASK_COL_2, title);
        contentValues.put(TASK_COL_3, description);
        contentValues.put(TASK_COL_4, date);
        contentValues.put(TASK_COL_5, 0); // Default to not completed
        contentValues.put(TASK_COL_6, userId);
        long result = db.insert(TASKS_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getAllTasks(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TASKS_TABLE_NAME + " WHERE USER_ID = ? ORDER BY IS_COMPLETED ASC, DATE DESC";
        return db.rawQuery(query, new String[]{userId});
    }

    public boolean updateTaskById(int id, String title, String description, long date, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TASK_COL_2, title);
        contentValues.put(TASK_COL_3, description);
        contentValues.put(TASK_COL_4, date);
        int result = db.update(TASKS_TABLE_NAME, contentValues, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }

    public boolean updateTaskCompletionStatus(int id, boolean isCompleted, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TASK_COL_5, isCompleted ? 1 : 0);
        int result = db.update(TASKS_TABLE_NAME, contentValues, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }

    public boolean deleteTaskById(int id, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TASKS_TABLE_NAME, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }

    // LOCATION TASKS METHODS - Updated with user filtering
    public boolean insertLocationTask(String title, String description, String locationAddress,
                                    double latitude, double longitude, int notificationRadius,
                                    boolean notificationEnabled, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOC_TASK_COL_2, title);
        contentValues.put(LOC_TASK_COL_3, description);
        contentValues.put(LOC_TASK_COL_4, locationAddress);
        contentValues.put(LOC_TASK_COL_5, latitude);
        contentValues.put(LOC_TASK_COL_6, longitude);
        contentValues.put(LOC_TASK_COL_7, notificationRadius);
        contentValues.put(LOC_TASK_COL_8, notificationEnabled ? 1 : 0);
        contentValues.put(LOC_TASK_COL_9, System.currentTimeMillis());
        contentValues.put(LOC_TASK_COL_10, userId);
        long result = db.insert(LOCATION_TASKS_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getAllLocationTasks(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + LOCATION_TASKS_TABLE_NAME + " WHERE USER_ID = ? ORDER BY CREATED_DATE DESC";
        return db.rawQuery(query, new String[]{userId});
    }

    public boolean deleteLocationTaskById(int id, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(LOCATION_TASKS_TABLE_NAME, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }

    public boolean updateLocationTaskById(int id, String title, String description, String locationAddress,
                                        double latitude, double longitude, int notificationRadius,
                                        boolean notificationEnabled, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOC_TASK_COL_2, title);
        contentValues.put(LOC_TASK_COL_3, description);
        contentValues.put(LOC_TASK_COL_4, locationAddress);
        contentValues.put(LOC_TASK_COL_5, latitude);
        contentValues.put(LOC_TASK_COL_6, longitude);
        contentValues.put(LOC_TASK_COL_7, notificationRadius);
        contentValues.put(LOC_TASK_COL_8, notificationEnabled ? 1 : 0);
        int result = db.update(LOCATION_TASKS_TABLE_NAME, contentValues, "ID = ? AND USER_ID = ?",
                              new String[]{String.valueOf(id), userId});
        return result > 0;
    }
}
