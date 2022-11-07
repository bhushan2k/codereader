package com.demo.yelp.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.demo.yelp.model.ResponseModel
import com.demo.yelp.network.API_KEY
import com.demo.yelp.network.DEMO_CITY
import com.demo.yelp.network.RetroInstance
import com.demo.yelp.network.RetroService
import retrofit2.Call
import retrofit2.Response

class RecyclerActivityViewModel: ViewModel() {

    var recyclerListData: MutableLiveData<ResponseModel>

    init {
        recyclerListData = MutableLiveData()
    }


    fun getRecyclerListDataObserver(): MutableLiveData<ResponseModel> {
        return recyclerListData
    }

    fun searchHotels(radius: Int, page: Int, swipeRefreshLayout: SwipeRefreshLayout) {
        val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
        val call = retroInstance.search(
            API_KEY,
            DEMO_CITY,
            "15",
            radius.toString(),
            "distance",
            "restaurants",
            page.toString())
        call.enqueue(object : retrofit2.Callback<ResponseModel>{
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }
                if(response.isSuccessful) {
                    recyclerListData.postValue(response.body())
                } else {
                    recyclerListData.postValue(null)
                }
            }

            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }
                recyclerListData.postValue(null)
            }
        })
    }
}