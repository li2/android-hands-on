package me.li2.android.runtracker;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

/*
使用Json文件存储简单少量的数据，使用SQLite存储大量数据。
为RunTracker应用创建数据存储机制，以利用数据库存储旅行数据以及地理位置信息。
SQLiteOpenHelper类封装了一些存储应用数据的常用数据库操作，如创建、打开、以及更新数据库等。

在RunTracker应用中，以SQLiteOpenHelper为父类，我们将创建一个RunDatabaseHelper类。
通过引用一个私有的RunDatabaseHelper类实例， 
RunManager类将为应用其他部分提供统一API，用于数据的插入、查询以及其他一些数据管理操作。
而RunDatabaseHelper类会提供各种方法供RunManager调用，以实现其定义的大部分API。

设计应用的数据库存储 API 时，我们通常是为需创建和管理的各类数据库创建一个SQLiteOpenHelper子类。
然后，再为需要访问的不同SQLite数据库文件创建一个子类实例。
包括RunTracker在内，大多应用只需要一个SQLiteOpenHelper子类，并与其余应用组件共享一个类实例。

要创建数据库，首先应考虑它的结构。面向对象编程语言中，最常见的数据库设计模式是为应用数据模型层的每个类都提供一张数据库表。
RunTracker应用中有两个类需要存储： Run和Location类。

因此我们需创建两张表： run和 location表。一个旅程（ Run）可包含多个地理位置信息（ Location），
所以location表将包含一个run_id外键与run表的_id列相关联。
*/

public class RunDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "run.sqlite";
    private static final int VERSION = 1;
    
    private static final String TABLE_RUN = "run";
    private static final String COLUMN_RUN_ID = "_id";
    private static final String COLUMN_RUN_START_DATE = "start_date";
    
    private static final String TABLE_LOCATION = "location";
    private static final String COLUMN_LOCATION_LATITUDE = "latitude";
    private static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
    private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_PROVIDER = "provider";
    private static final String COLUMN_LOCATION_RUN_ID = "run_id";
    
    public RunDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 在onCreate(...) 方法中，应为新建数据库创建表结构。
        // Create the "run" table
        db.execSQL("create table run ("
                + "_id integer primary key autoincrement, start_date integer)");
        // Create the "location" table
        db.execSQL("create table location ("
                + "timestamp integer, latitude real, longitude real, altitude real, "
                + "provider varchar(100), run_id integer references run(_id))");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 在onUpgrade(...) 方法中，可执行迁移代码，实现不同版本间的数据库结构升级或转换。
        // Implement schema changes and data migration here when upgrading
    }
    
    public long insertRun(Run run) {
        // 在run数据表中插入一条新纪录并返回其ID。run表只有旅行开始日期一个数据字段，
        // 此处通过ContentValues对象表示的栏位名与值的“名值对”，将long类型的开始日期存入到数据库中。
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_RUN_START_DATE, run.getStartDate().getTime());
        return getWritableDatabase().insert(TABLE_RUN, null, cv);
    }
    
    public long insertLocation(long runId, Location location) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_LOCATION_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LOCATION_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_LOCATION_ALTITUDE, location.getAltitude());
        cv.put(COLUMN_LOCATION_TIMESTAMP, location.getTime());
        cv.put(COLUMN_LOCATION_PROVIDER, location.getProvider());
        cv.put(COLUMN_LOCATION_RUN_ID, runId);
        return getWritableDatabase().insert(TABLE_LOCATION, null, cv);
    }
    
    public RunCursor queryRuns() {
        // 查询SQLiteDatabase返回一个Cursor实例，
        // Cursor将结果集看做是一系列的数据行和数据列，但仅支持String以及原始数据类型的值。
        // 而通过Cursor可以列出按日期排序的旅程列表；
        // Equivalent to "select * from run order by start_date asc"
        Cursor wrapped = getReadableDatabase().query(TABLE_RUN,
                null, null, null, null, null, COLUMN_RUN_START_DATE + " asc");
        return new RunCursor(wrapped);
    }
    
    // 我们习惯于使用对象来封装模型层数据，如Run和Location对象。
    // 既然我们已经有了代表对象的数据库表，如果能从Cursor中取得这些对象的实例就再好不过了。
    // 为实现以上目标，我们将继承类CursorWrapper（用于封装当前的Cursor类， 并转发所有的方法调用给它）：
    // A convenience class to wrap a cursor that returns rows from the "run" table.
    // The {@link getRun()} method will give you a Run instance representing the current row.
    public static class RunCursor extends CursorWrapper {
        public RunCursor(Cursor cursor) {
            super(cursor);
        }
        
        // Returns a Run object configured for the current row,
        // or null if the current row is invalid.
        public Run getRun() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }
            long runId = getLong(getColumnIndex(COLUMN_RUN_ID));
            long startDate = getLong(getColumnIndex(COLUMN_RUN_START_DATE));
            Run run = new Run();
            run.setId(runId);
            run.setStartDate(new Date(startDate));
            return run;
        }
    }
}
/*
SQLite教程 http://www.runoob.com/sqlite/sqlite-tutorial.html
CREATE TABLE database_name.table_name(
        column1 datatype  PRIMARY KEY(one or more columns),
        column2 datatype,
        column3 datatype,
        .....
        columnN datatype,
     );
*/
