package com.example.tfgwallet.ui.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgwallet.R
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.FragmentKeysBinding
import com.example.tfgwallet.model.KeyItem
import com.example.tfgwallet.model.KeyManagement
import com.example.tfgwallet.model.MasterKeyItem
import com.example.tfgwallet.model.Quadruple
import com.example.tfgwallet.ui.DAppBrowserKeys
import com.example.tfgwallet.ui.KeyItemAdapter
import com.example.tfgwallet.ui.MasterKeyItemAdapter
import com.example.tfgwallet.ui.QRCodeScannerActivity
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ARG_PARAM1 = "user"
private const val ARG_PARAM2 = "privKey"
private const val ARG_PARAM3 = "pubKey"
private const val ARG_PARAM4 = "chainCode"


class KeysFragment : Fragment() {
    private var user: String = ""
    private var pubKey: String = ""
    private var privKey: String = ""
    private var chainCode: String = ""
    private lateinit var mAdapter: MasterKeyItemAdapter
    private lateinit var bAdapter: KeyItemAdapter
    private lateinit var binding: FragmentKeysBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            user = it.getString(ARG_PARAM1).toString()
            privKey = it.getString(ARG_PARAM2).toString()
            pubKey = it.getString(ARG_PARAM3).toString()
            chainCode = it.getString(ARG_PARAM4).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKeysBinding.inflate(inflater, container, false)

        setMasterKeysDataAdapter()
        setupMasterRecyclerView()
        fetchAndProcessKeys()

        return binding.root
    }

    private fun fetchAndProcessKeys() {
        val prefs = context?.getSharedPreferences("user_$user", Context.MODE_PRIVATE)
        val plugins = prefs?.getString("pluginAddresses", "") ?: ""

        if (plugins.isNotEmpty()) {
            val keysList: ArrayList<KeyItem> = ArrayList()
            binding.browserNoKeys.text = getString(R.string.loading_keys)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val refs = plugins.split(";").map { plugin ->
                    Control.control(requireContext(), user, plugin, "user_$user")
                }

                refs.forEach { ref ->
                    val hashes = Control.getDataFromIPFS(ref, requireContext()).split("\n")
                    hashes.forEach { hash ->
                        val data = Control.getDataFromIPFS(hash, requireContext())
                        val decryptedData = KeyManagement.decryptIPFSData(data, "user_$user", requireContext())
                        if (decryptedData != null) {
                            Log.i("Decrypted data", decryptedData.toString())
                            keysList.add(decryptedDataToKeyItem(decryptedData))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    setupBrowserRecyclerView(keysList)
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptedDataToKeyItem(decryptedData: Quadruple<ByteArray, ByteArray, ByteArray, ByteArray>): KeyItem {
        return KeyItem(
            "0x" + decryptedData.first.toHexString(HexFormat.Default),
            "0x" + decryptedData.second.toHexString(HexFormat.Default),
            "0x" + decryptedData.third.toHexString(HexFormat.Default)
        )
    }

    private fun setupMasterRecyclerView() {
        val masterRecyclerView = binding.masterRecycler
        masterRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        masterRecyclerView.adapter = mAdapter
    }

    private fun setupBrowserRecyclerView(keys: List<KeyItem>) {
        binding.browserNoKeys.visibility = View.INVISIBLE
        bAdapter = KeyItemAdapter(keys)
        val browserRecyclerView = binding.browserRecycler
        browserRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        browserRecyclerView.adapter = bAdapter

        val callback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val dAppIntent = Intent(this@KeysFragment.requireContext(), DAppBrowserKeys::class.java)
                startActivity(dAppIntent)
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pastelLilac))
                    .addActionIcon(R.drawable.baseline_info_24)
                    .addSwipeLeftLabel("More info")
                    .setSwipeLeftLabelColor(R.color.binaryBlack)
                    .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
                    .addCornerRadius(TypedValue.COMPLEX_UNIT_DIP, 8)
                    .create()
                    .decorate()

                super.onChildDraw(c, recyclerView, viewHolder, dX/2, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(browserRecyclerView)
    }

    private fun setMasterKeysDataAdapter() {
        val keys: MutableList<MasterKeyItem> = ArrayList()
        val scHash = context?.getSharedPreferences("user_$user", Context.MODE_PRIVATE)
            ?.getString("contractAddress", "").toString()
        keys.add(MasterKeyItem(KeyItem("0x$privKey", "0x$pubKey", "0x$chainCode"), scHash))
        binding.masterNoKeys.visibility = View.INVISIBLE
        mAdapter = MasterKeyItemAdapter(keys)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String, param3: String, param4: String) =
            KeysFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                    putString(ARG_PARAM3, param3)
                    putString(ARG_PARAM4, param4)
                }
            }
    }
}
