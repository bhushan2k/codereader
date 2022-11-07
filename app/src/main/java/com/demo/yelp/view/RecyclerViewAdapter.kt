package com.demo.yelp.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.demo.yelp.model.Business
import com.demo.yelp.R
import com.demo.yelp.model.ResponseModel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.recyclerview_row.view.*

class RecyclerViewAdapter: RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

    var items = ArrayList<Business>()

    fun setListData(data: ResponseModel) {
        this.items = data.businesses
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_row, parent, false)

        return MyViewHolder(inflater)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val business = items[position]
        holder.bind(business)
    }

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val hotelImage = view.hotel_image
        val hotelName = view.hotel_name
        val hotelAddress = view.hotel_address
        val isOpen = view.is_open
        val hotelRating = view.hotel_rating

        fun bind(data: Business) {

            hotelName.text = data.name
            hotelAddress.text = data.location.address1
            isOpen.text = if (data.isClosed) "Currently Closed" else "Currently Open"
            hotelRating.text = data.rating.toString()

            Picasso.get()
                .load(data.image_url)
                .placeholder(R.drawable.default_thumb)
                .error(R.drawable.abc_vector_test)
                .into(hotelImage)
        }

    }

    fun addList(data: ArrayList<Business>){
        if (this.items.isEmpty()) {
            this.items = data
        } else {
            this.items.addAll(data)
        }
    }

    fun clear(){
        items.clear()
    }

}