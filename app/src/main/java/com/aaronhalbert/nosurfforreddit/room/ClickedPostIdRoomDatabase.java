package com.aaronhalbert.nosurfforreddit.room;

import android.app.Application;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@SuppressWarnings("ALL")
@Database(entities = {ClickedPostId.class}, version = 1)
public abstract class ClickedPostIdRoomDatabase extends RoomDatabase {

    public abstract ClickedPostIdDao clickedPostIdDao();
    private static volatile ClickedPostIdRoomDatabase INSTANCE;

    //prevent having multiple instances of the database opened at the same time
    public static ClickedPostIdRoomDatabase getDatabase(final Application application) {
        if (INSTANCE == null) {
            synchronized (ClickedPostIdRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(application,
                            ClickedPostIdRoomDatabase.class,
                            "clicked_post_id_database")
                            // Wipes and rebuilds instead of migrating if no Migration object.
                            .fallbackToDestructiveMigration()
                            //.addCallback(clickedPostIdRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // called once when the DB is first created, if addCallback is called above
    private static final RoomDatabase.Callback clickedPostIdRoomDatabaseCallback = new RoomDatabase.Callback(){
        @Override
        public void onCreate (SupportSQLiteDatabase db){
            super.onCreate(db);
            //do work on background thread here
        }
    };
}