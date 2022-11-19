package com.demo.yelp.ViewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.demo.yelp.Models.ResponseModel
import com.demo.yelp.Network.API_KEY
import com.demo.yelp.Network.DEMO_CITY
import com.demo.yelp.Network.RetroInstance
import com.demo.yelp.Network.RetroService
import retrofit2.Call
import retrofit2.Response

class RecyclerActivityViewModel: ViewModel() {

    /**
     * this mutable live data type will be set when it receives data from API
     */
    var recyclerListData: MutableLiveData<ResponseModel>

    init {
        recyclerListData = MutableLiveData()
    }


    /**
     * this method will return mutable live data to main activity
     */
    fun getRecyclerListDataObserver(): MutableLiveData<ResponseModel> {
        return recyclerListData
    }

    /**
     * this is actual api call inside method which will set result in mutable live data and send to activity
     * if location object is null then api will return results based on latitude and longitude and vice versa
     *
     * sorting is done by distance by default
     */
    fun searchHotels(radius: Int, page: Int, swipeRefreshLayout: SwipeRefreshLayout, location: String?, latitude: String?, longitude: String?) {
        val retroInstance = RetroInstance.getRetroInstance().create(RetroService::class.java)
        val call = retroInstance.search(
            API_KEY,
            location,
            latitude,
            longitude,
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