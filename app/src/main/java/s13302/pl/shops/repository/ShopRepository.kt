package s13302.pl.shops.repository

import com.google.android.gms.tasks.OnSuccessListener
import s13302.pl.shops.data.Shop

interface ShopRepository {

    fun registerDataListener(dataListener: DataListener<Shop>)

    fun unregisterDataListener(dataListener: DataListener<Shop>)

    fun addShop(shop: Shop, successListener: OnSuccessListener<Void>)

    fun removeShop(shop: Shop, successListener: OnSuccessListener<Void>)

}