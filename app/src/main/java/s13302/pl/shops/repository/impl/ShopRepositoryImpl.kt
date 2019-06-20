package s13302.pl.shops.repository.impl

import android.util.Log
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.data.Shop
import s13302.pl.shops.repository.DataListener
import s13302.pl.shops.repository.ShopRepository

class ShopRepositoryImpl(userId: String): ShopRepository, ValueEventListener {

    companion object {
        private const val TAG = "ShopRepositoryImpl"
    }

    private val database = FirebaseDatabase.getInstance()
    private val dataListeners = mutableListOf<DataListener<Shop>>()
    private val userShopsPath = "${ProjectConstants.SHOPS_NODE_NAME}$userId"

    init {
        val dataReference = database.getReference(userShopsPath)

        dataReference.addValueEventListener(this)
    }

    override fun registerDataListener(dataListener: DataListener<Shop>) {
        if (! dataListeners.contains(dataListener)) {
            dataListeners.add(dataListener)
        }
    }

    override fun unregisterDataListener(dataListener: DataListener<Shop>) {
        if (dataListeners.contains(dataListener)) {
            dataListeners.remove(dataListener)
        }
    }

    override fun addShop(shop: Shop, successListener: OnSuccessListener<Void>) {
        val reference = database.getReference("$userShopsPath/${shop.shopId}")
        reference.setValue(shop).addOnSuccessListener(successListener)
    }

    override fun removeShop(shop: Shop, successListener: OnSuccessListener<Void>) {
        val reference = database.getReference("$userShopsPath/${shop.shopId}")
        reference.removeValue().addOnSuccessListener(successListener)
    }

    override fun onCancelled(p0: DatabaseError) {}

    override fun onDataChange(p0: DataSnapshot) {
        val shops = p0.children.mapNotNull {
            it.getValue(Shop::class.java)
        }
        fireOnDataChangeEvent(shops)
    }

    private fun fireOnDataChangeEvent(data: List<Shop>) {
        Log.d(TAG, "Fired onDataChange event")
        dataListeners.onEach {
            it.onDataChange(data)
        }
    }

}