package ru.tp_project.androidreader.model

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.tp_project.androidreader.R
import ru.tp_project.androidreader.model.dao.BookDao
import ru.tp_project.androidreader.model.dao.TaskDao
import ru.tp_project.androidreader.model.dao.UserStatisticDao
import ru.tp_project.androidreader.model.data_models.Book
import ru.tp_project.androidreader.model.data_models.Task
import ru.tp_project.androidreader.model.data_models.User
import ru.tp_project.androidreader.model.data_models.*

@Database(
    entities = [User::class, Task::class, BookTaskStat::class,
        Book::class, TaskStatDB::class, TaskBook::class], version = 1
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun userStatisticDao(): UserStatisticDao
    abstract fun taskDao(): TaskDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDb? = null

        @Synchronized
        fun getInstance(context: Context): AppDb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "app_database"
            ).addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT INTO user VALUES(?, 0, 0, 0, 0, 0, 0, 0, 0, 0)",
                            intArrayOf(context.resources.getInteger(R.integer.single_user_id)).toTypedArray()
                        )
                        // remove in future
                        db.execSQL("INSERT INTO task VALUES(1, 'task#1', 'vip task', 1, 181881, 21212, 1, 2, 3)")

                        db.execSQL("INSERT INTO task_stat VALUES(1, 1, 1, 1, 1, 'false')")
                        db.execSQL(
                            "INSERT INTO book VALUES( 1, \"Война и мир\", \"no\",\n" +
                                    "                            \"Лев Николаевич Толстой\", " +
                                    "\"15.12.2012\", " + "\"kotlin\", " + "\"android\", " +
                                    "\"23.3kb\", " + "\"FB2\", 0.3," +
                                    "\"nopath\",  \"Это какой то текст\", 1, 8)")
                        //intArrayOf(R.integer.single_user_id).toTypedArray()
                    }
                }
            ).build()
            INSTANCE = instance
            return instance
        }
    }
}


@Dao
interface BooksDao {
    @Query("SELECT * FROM book")
    suspend fun getAll(): List<Book>
    @Insert
    suspend fun addBook(book: Book)

    @Query("DElETE FROM book WHERE id = :id")
    suspend fun deleteBook(id: Int)
    @Update
    suspend fun updateBook(book: Book)
}

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(book: Book)

    @Query("SELECT * FROM book WHERE id = :bookId")
    fun load(bookId: String): Book
}

@Database(entities = [Book::class], version = 1)
abstract class BookDb : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun booksDao(): BooksDao

    companion object {
        @Volatile
        private var INSTANCE: BookDb? = null

        fun getInstance(context: Context): BookDb {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(BookDb::class) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookDb::class.java,
                    "book_database"
                ).addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL(
                                "INSERT INTO book VALUES( 1, \"Война и мир\", \"no\",\n" +
                                        "                            \"Лев Николаевич Толстой\", " +
                                        "\"15.12.2012\", " + "\"kotlin\", " + "\"android\", " +
                                        "\"23.3kb\", " + "\"FB2\", 0.3," +
                                        "\"nopath\",  \"Это какой то текст\", 1, 8)"
                                //intArrayOf(R.integer.single_user_id).toTypedArray()
                            )
                        }
                    }
                )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}