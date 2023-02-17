package com.example.sms_app_proj;

public final class BlockedNumbers {

    // Define the table schema
    public static final String TABLE_NAME = "blocked_numbers";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NUMBER = "number";

    // Define the SQL statements for creating and deleting the table
    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NUMBER + " TEXT)";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}