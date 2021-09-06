package com.example.pricerecorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.pricerecorder.database.Product

/*Adapta los datos usados a la recyclerView*/
//private var dataset: LiveData<List<Product>>
class ProductAdapter():
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var dataset :List<Product> = ArrayList<Product>()
    /*Crea los distintos viewholders usados, sera llamado cuando no haya viewholders existentes para reutilizar*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProductViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_product,parent,false)
        )
    }

    /*Metodo llamado para vincular las vistas no usadas con nuevos objetos y reciclarlas*/
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is ProductViewHolder -> {
                holder.bind(dataset[position])
            }
        }
    }

    /*Devolvemos la cantidad de elementos de la lista, la cual puede estar vacia*/
    override fun getItemCount(): Int {
        return dataset.size
    }

    fun submitList(list: List<Product>){
        dataset = list
    }

    /*La clase extiende de RecyclerView.ViewHolder ya que ese es el tipo de viewholder que se le especifico al adapter.*/
    class ProductViewHolder constructor(itemView:View): RecyclerView.ViewHolder(itemView){
        val description:TextView = itemView.findViewById(R.id.product_description_text)
        val purchasePlace:TextView = itemView.findViewById(R.id.purchase_place_text)
        val price:TextView = itemView.findViewById(R.id.product_price_text)
        val updateDate:TextView = itemView.findViewById(R.id.update_date_text)
        val productImage:ImageView = itemView.findViewById(R.id.product_image)

        /*Responsable de unir cada objeto Producto a las vistas del layout*/
        fun bind(product: Product){
            description.text = product.description
            purchasePlace.text = product.placeOfPurchase
            price.text = product.price.toString()
            updateDate.text = product.updateDate
            productImage.setImageResource(R.drawable.ic_broken_image)
        }
    }
}