package com.demo.yelp.view

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.yelp.R
import com.demo.yelp.model.ResponseModel
import com.demo.yelp.viewmodels.RecyclerActivityViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    lateinit var recyclerViewAdapter: RecyclerViewAdapter
    lateinit var viewModel: RecyclerActivityViewModel
    var radius = 500
    var page = 1
    lateinit var mLayoutManager: LinearLayoutManager

    var loading = true
    var pastVisiblesItems = 0
    var visibleItemCount:Int = 0
    var totalItemCount:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        initRecyclerView()
        createData()
        initSeekBar()
        initSwipeLayout()
    }

    private fun initSwipeLayout() {
        swipeLayout.apply {
            setOnRefreshListener {
                recyclerViewAdapter.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                page = 1
                viewModel.searchHotels(radius, page, this)
            }
        }
    }

    private fun initRecyclerView() {
        mLayoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.apply {
            layoutManager = mLayoutManager
            recyclerViewAdapter = RecyclerViewAdapter()
            adapter = recyclerViewAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    visibleItemCount = mLayoutManager.childCount
                    totalItemCount = mLayoutManager.itemCount
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition()

                    if (loading) {
                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            loading = false
                            if (totalItemCount % 15 == 0) {
                                page++
                                viewModel.searchHotels(radius, page, swipeLayout)
                            }
                        }
                    }
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
        }
    }

    private fun createData() {
    viewModel = ViewModelProvider(this).get(RecyclerActivityViewModel::class.java)
        viewModel.getRecyclerListDataObserver().observe(this, Observer<ResponseModel>{
            loading = true
            if(it != null) {
                recyclerViewAdapter.addList(it.businesses)
                recyclerViewAdapter.notifyDataSetChanged()

            } else {
                Toast.makeText(this@MainActivity, "Error in getting data from api.", Toast.LENGTH_LONG).show()
            }

        })
        viewModel.searchHotels(radius, page, swipeLayout)
    }

    private fun initSeekBar() {
        seekBar.apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    if (p0 != null) {
                        radius = p0.progress
                        selectedRadius.text = p0.progress.toString() + " KM"
                        page = 1;
                        recyclerViewAdapter.clear()
                        recyclerViewAdapter.notifyDataSetChanged()
                        viewModel.searchHotels(radius, page, swipeLayout)
                    }
                }
            })
        }
    }
}