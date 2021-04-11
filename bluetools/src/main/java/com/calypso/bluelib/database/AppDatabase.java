package com.calypso.bluelib.database;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

//需要手动修改version 配合Migration方式升级
@Database(entities = {Student.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase appdatabase;

    public static AppDatabase getInstance(Context context) {
        if (appdatabase == null) {
            appdatabase = Room.databaseBuilder(context
                    , AppDatabase.class
                    , "jettDB2")
                    // 可以强制在主线程运行数据库操作
                    .allowMainThreadQueries()
                    //强制升级 数据库数据丢失
//                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return appdatabase;
    }

    public abstract StudentDao studentDao();

    //进行数据库升级
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //用sql脚本完成数据变化
            database.execSQL("alter table student add column flag integer not null default 1");

        }
    };

    //进行数据库升级
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("create table student_temp (uid integer primary key not null,name text,pwd text,addressid text)");
            database.execSQL("insert into student_temp (uid,name,pwd,addressid)"+"select uid,name,pwd,addressid from student");
            database.execSQL("drop table student");
            database.execSQL("alter table student_temp rename to student");

        }
    };
}
