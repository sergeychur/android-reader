package ru.tp_project.androidreader.viewModels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import ru.tp_project.androidreader.R
import ru.tp_project.androidreader.models.Book
import ru.tp_project.androidreader.repository.BookRepository
import javax.inject.Inject

// https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate
// https://www.fandroid.info/viewmodels-with-saved-state/
class BookShelveViewModel @Inject constructor(val context: Context): ViewModel() {
    var data = MutableLiveData<Book>()// LiveData<Book> = repository.getBook(userId!!)
    var repository = BookRepository()
    var fail = MutableLiveData<Boolean>().apply { value = false }

    fun refresh() {
        repository.getBook(context) { isSuccess, book ->
            if (isSuccess) {
                Log.d("we get book", "book:"+ book)
                data.postValue(book)
            } else {
                Log.d("we dont get book", "book:"+ book)
                fail.postValue(true)
            }
        }
    }
}