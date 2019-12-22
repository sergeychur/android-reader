package ru.tp_project.androidreader.view


import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import ru.tp_project.androidreader.R
import ru.tp_project.androidreader.model.data_models.Book
import ru.tp_project.androidreader.view_models.BookShelveViewModel
import java.util.ArrayList
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_book_shelve.*
import kotlinx.android.synthetic.main.fragment_tasks_list.*
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import ru.tp_project.androidreader.databinding.ShelveOneBookBinding
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import ru.tp_project.androidreader.BR
import ru.tp_project.androidreader.databinding.FragmentBookShelveBinding
import ru.tp_project.androidreader.databinding.FragmentTasksListBinding
import ru.tp_project.androidreader.model.data_models.Task
import ru.tp_project.androidreader.model.xml.BookXML
import ru.tp_project.androidreader.view.book_viewer.BookViewer
import ru.tp_project.androidreader.view.tasks_list.TasksListAdapter
import ru.tp_project.androidreader.view.tasks_list.TasksListViewModel
import ru.tp_project.androidreader.view_models.BooksShelveViewModel
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory



class BookShelfFragment : Fragment() {
    private lateinit var viewDataBinding: FragmentBookShelveBinding
    private lateinit var adapter: ListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = FragmentBookShelveBinding.inflate(inflater, container, false).apply {
            viewmodel = ViewModelProviders.of(this@BookShelfFragment).get(BooksShelveViewModel::class.java)
            lifecycleOwner = viewLifecycleOwner
        }
        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = getActivity()?.getApplicationContext()
        val viewModel = viewDataBinding.viewmodel

        viewModel?.let {
            context?.let {  viewModel.getAll(context) }
            setupViews()
            setupAdapter(viewModel)
            setupObservers(viewModel)
        }
    }

    private fun setupViews() {
        addBook.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                showFileChooser()
            }
        })
    }

    private fun setupObservers(viewModel : BooksShelveViewModel) {
        viewModel.data.observe(viewLifecycleOwner, Observer {
            adapter.updateTasksList(it)
        })
    }

    private fun setupAdapter(viewModel : BooksShelveViewModel) {
        adapter = ListAdapter(viewModel)
        val layoutManager = LinearLayoutManager(activity)
        listRecyclerView.layoutManager = layoutManager
        listRecyclerView.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))
        listRecyclerView.adapter = adapter
    }

    fun showFileChooser() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)


        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {

            val input: InputStream? = getActivity()!!.getContentResolver().openInputStream(data!!.data!!)

            val inputAsString = input!!.bufferedReader().use { it.readText() }
            val book = loadBookXML(inputAsString!!)
            if (book == null) {

            } else {
                val intent = Intent(this.context, BookViewer::class.java)
                // To pass any data to next activity

                intent.putStringArrayListExtra("book", ArrayList(book.body.section))
                intent.putExtra("pages_count", book.body.section.size)
                intent.putExtra("pages_current", 2)
                intent.putExtra("image", book.binary)
                startActivity(intent)
            }
        }
    }

    fun getContent(xml : String) : String? {
        val start = xml.indexOf("<section>")+"<section>".length
        val end = xml.indexOf("</section>")
        if (start >= end) {
            return null
        }
        return xml.substring(start..end)
    }

    // addContentToModel add rows to BookXML. If no rows, add one row "No content"
    fun addContentToModel(book : BookXML, content : String?) : BookXML {
        var rows : MutableList<String> = mutableListOf<String>()
        if (content != null) {
            var strs = content.split("</p>")
            val s = 4
            for (str in strs) {
                if (str.length < s) {
                    continue
                }
                rows.add(str.substring(s))
            }
        }
        if (rows.size > 0) {
            book.body.section = rows
        } else {
            book.body.section = listOf<String>("No content")
        }
        return book
    }

    fun loadBookXML(xml : String) : BookXML? {
        val reader = StringReader(xml)
        val serializer = Persister()
        var book : BookXML
        try {
            book = serializer.read(BookXML::class.java, reader, false)
        } catch (e: Exception) {
            //Log.e("SimpleTest", "ddd")
            Log.e("Wrong book", e.message!!)
            return null
        }

        val content = getContent(xml)
        book = addContentToModel(book, content)

        for (str in book.body.section) {
            Log.e("texttexttext: ", str)
        }


        Log.d("SimpleTest", "success "+book!!.body.title.p)
        Log.d("SimpleTest", "mars "+book!!.body.section.size)
        return book
    }
}

class ListAdapter(private val books: BooksShelveViewModel) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {
    var booksList: List<Book> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.ListViewHolder {
        val inflatter = LayoutInflater.from(parent.getContext())
        val binding =  ShelveOneBookBinding.inflate(inflatter,parent, false)
        return ListViewHolder(binding.root, binding, parent.getContext() )
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
       holder.setup(booksList[position])
    }

    fun updateTasksList(booksList: List<Book>) {
        this.booksList = booksList
        notifyDataSetChanged()
    }

    override fun getItemCount() = booksList.size

    @BindingAdapter("bind:imageUrl")
    fun loadImage( imageView : ImageView, v:String) {
        Picasso.with(imageView.getContext()).load(v).into(imageView);
    }

    class ListViewHolder(itemView: View,
                         private val dataBinding: ViewDataBinding,
                         val context: Context
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        fun setup(itemData: Book) {
            dataBinding.setVariable(BR.book, itemData)
            dataBinding.executePendingBindings()
        }

        override fun onClick(v: View) {

        }
    }
}
