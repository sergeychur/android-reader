package ru.tp_project.androidreader.view.book_viewer

import android.app.PendingIntent.getActivity
import android.os.Bundle
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.Log
import android.util.Xml
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import ru.tp_project.androidreader.R
import ru.tp_project.androidreader.model.data_models.Book
import ru.tp_project.androidreader.view_models.BookViewerViewModel

import java.util.HashMap

class BookViewer : AppCompatActivity() {
    private var mPager: ViewPager? = null
    private var mPagerAdapter: FragmentPagerAdapter? = null
    private var mPages: Map<String, String> = HashMap()
    private var mPageIndicator: LinearLayout? = null
    private var mProgressBar: ProgressBar? = null
    private var book : Book? = null
    private var mPagesAmount = 0
    private var mPagesCurrent = 0
    private var mDisplay: Display? = null
    private var viewmodel: BookViewerViewModel? = null

    private val screenWidth: Int
        get() {
            val horizontalMargin =
                getResources().getDimension(R.dimen.activity_horizontal_margin) * 2
            return (mDisplay!!.width - horizontalMargin).toInt()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.book_viewer_main)
        mProgressBar = findViewById(R.id.progress) as ProgressBar

        viewmodel = ViewModelProviders.of(this@BookViewer).get(BookViewerViewModel::class.java)

        val textviewPage = getLayoutInflater().inflate(
            R.layout.book_viewer_fragment,
            getWindow().getDecorView().findViewById(android.R.id.content) as ViewGroup,
            false
        ) as ViewGroup
        val layout = textviewPage.findViewById(R.id.mText) as LinearLayout
        val contentTextView = layout.findViewById(R.id.text) as TextView

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager) as ViewPager

        book = getFromIntent()

        // obtaining screen dimensions
        mDisplay = getWindowManager().getDefaultDisplay()

        val vp = ViewAndPaint(
            contentTextView.paint,
            textviewPage,
            screenWidth,
            getMaxLineCount(contentTextView),
            book!!.text
        )

        //initViewPager(book!!)

        val pt = PagerTask(mPager, book!!.currPage, {v -> this.onPageProcessedUpdate(v, book!!)})
        pt.execute(vp)
    }

    private fun getMaxLineCount(view: TextView): Int {

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val verticalMargin = getResources().getDimension(R.dimen.activity_vertical_margin)
        val paint = view.paint

        //Working Out How Many Lines Can Be Entered In The Screen
        val fm = paint.fontMetrics
        var textHeight = fm.top - fm.bottom
        textHeight = Math.abs(textHeight)

        var maxLineCount = ((height - verticalMargin) / textHeight).toInt()

        return maxLineCount+2
    }

    fun getFromIntent() : Book {
        return intent.getSerializableExtra("book") as Book
    }

    private fun initViewPager(book: Book) {
        mPagerAdapter = MyPagerAdapter(getSupportFragmentManager(), 1, book)
        mPager!!.setAdapter(mPagerAdapter)
        mPager!!.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                Log.d("notag", ""+position)
                showPageIndicator(position)
            }
        })
    }

    fun onPageProcessedUpdate(progress: ProgressTracker, book: Book) {
        mPages = progress.pages
        // init the pager if necessary
        Log.d("look currPage", ""+book.currPage)
        if (mPagerAdapter == null) {
            initViewPager(book)
            hideProgress()
            mPagesCurrent = book.currPage
            addPageIndicator(book.currPage)
        } else {
            (mPagerAdapter as MyPagerAdapter).incrementPageCount()
            addPageIndicator(mPagesCurrent)
            mPagesAmount =  progress.totalPages+1
        }

    }

    private fun hideProgress() {
        mProgressBar!!.visibility = View.GONE
    }

    private fun addPageIndicator(pageNumber: Int) {
        setIndicator(pageNumber)
        setPages(pageNumber)
    }

    private fun setIndicator(pageNumber: Int) {
        mPageIndicator = findViewById(R.id.pageIndicator) as LinearLayout
        val view = View(this)
        val params = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        view.layoutParams = params
        if (pageNumber == 0) {
            view.setBackgroundResource(R.drawable.current_page_indicator)
        } else {
            view.setBackgroundResource(R.drawable.indicator_background)
        }
        view.tag = pageNumber
        mPageIndicator!!.addView(view)
    }

    fun setColor(pageNumber: Int) {
        val view = View(this)
        val params = TableLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
        view.layoutParams = params
        if (pageNumber == 0) {
            view.setBackgroundResource(R.drawable.current_page_indicator)
        } else {
            view.setBackgroundResource(R.drawable.indicator_background)
        }
    }

    fun setPages(position: Int) {
        var textview = findViewById(R.id.pages) as TextView
        val pages = (position+1).toString()+" "+getString(R.string.from)+" "+mPagesAmount.toString()
        textview.setText(pages)

        book!!.currPage = position
        book!!.pages = mPagesAmount



        viewmodel!!.update(getApplicationContext(), book!!)
    }

    protected fun showPageIndicator(position: Int) {
        setPages(position)

        try {
            mPageIndicator = findViewById(R.id.pageIndicator) as LinearLayout
            val selectedIndexIndicator = mPageIndicator!!.getChildAt(position)
            selectedIndexIndicator.setBackgroundResource(R.drawable.current_page_indicator)
            // dicolorize the neighbours
            if (position > 0) {
                val leftView = mPageIndicator!!.getChildAt(position - 1)
                leftView.setBackgroundResource(R.drawable.indicator_background)
            }
            if (position < mPages.size) {
                val rightView = mPageIndicator!!.getChildAt(position + 1)
                rightView.setBackgroundResource(R.drawable.indicator_background)
            }


        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

     fun getContents(pageNumber: Int): String {
        val page = pageNumber.toString()
        val textBoundaries = mPages[page]
        if (textBoundaries != null) {
            val bounds =
                textBoundaries.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val startIndex = Integer.valueOf(bounds[0])
            val endIndex = Integer.valueOf(bounds[1])
            return book!!.text.substring(startIndex, endIndex).trim { it <= ' ' }
        }
        return ""
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    class ViewAndPaint(
        var paint: TextPaint,
        var textviewPage: ViewGroup,
        var screenWidth: Int,
        var maxLineCount: Int,
        var contentString: String
    )

    class ProgressTracker {

        var totalPages: Int = 0
        var pages: MutableMap<String, String> = HashMap()

        fun addPage(page: Int, startIndex: Int, endIndex: Int) {
            val thePage = page.toString()
            val indexMarker = "$startIndex,$endIndex"
            pages[thePage] = indexMarker
        }
    }

    companion object {

        private val TAG = "BookView"
    }
}