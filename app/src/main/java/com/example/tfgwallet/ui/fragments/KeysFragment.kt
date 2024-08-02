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
import android.widget.TextView
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
import com.example.tfgwallet.ui.BrowserKeyItemAdapter
import com.example.tfgwallet.ui.DAppBrowserKeys
import com.example.tfgwallet.ui.KeyItemAdapter
import com.example.tfgwallet.ui.MasterKeyItemAdapter
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.trimSubstring
import org.web3j.crypto.Credentials
import java.math.BigInteger

private const val USER_ARG = "user"
private const val PRIVKEY_ARG = "privKey"
private const val PUBKEY_ARG = "pubKey"
private const val CHAINCODE_ARG = "chainCode"


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
            user = it.getString(USER_ARG).toString()
            privKey = it.getString(PRIVKEY_ARG).toString()
            pubKey = it.getString(PUBKEY_ARG).toString()
            chainCode = it.getString(CHAINCODE_ARG).toString()
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
        val browserFiles = KeyManagement.getBrowserKeysPaths(requireContext(), user)
        val keysList: ArrayList<KeyItem> = ArrayList()
        val browserTitles: ArrayList<String> = ArrayList()
        if (browserFiles.isNotEmpty()) {
            binding.browserNoKeys.text = getString(R.string.loading_keys)

            browserFiles.forEach {
                browserFile ->
                val decryptedData = KeyManagement.decryptRSA(browserFile.substringAfter(requireContext().getDir("users", Context.MODE_PRIVATE).path), user, requireContext())
                keysList.add(decryptedDataToKeyItem(decryptedData))
                browserTitles.add(browserFile.substringBeforeLast('.').substringAfterLast('/')) // get rid of .bin
            }
            setupBrowserRecyclerView(keysList, browserTitles)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptedDataToKeyItem(decryptedData: Triple<BigInteger, BigInteger, ByteArray>?): KeyItem {
        return if (decryptedData != null) {
            KeyItem(
                "0x" + decryptedData.first.toString(16),
                "0x" + decryptedData.second.toString(16),
                "0x" + decryptedData.third.toHexString(HexFormat.Default)
            )
        } else KeyItem("0x", "0x", "0x")
    }

    private fun setupMasterRecyclerView() {
        val masterRecyclerView = binding.masterRecycler
        masterRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        masterRecyclerView.adapter = mAdapter
    }

    private fun setupBrowserRecyclerView(keys: List<KeyItem>, browserTitles: List<String>) {
        binding.browserNoKeys.visibility = View.INVISIBLE
        bAdapter = BrowserKeyItemAdapter(keys, browserTitles)
        val browserRecyclerView = binding.browserRecycler
        browserRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        browserRecyclerView.adapter = bAdapter

        val callback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val address = Credentials.create(viewHolder.itemView.findViewById<TextView>(R.id.privKey).text.toString()).address
                val dAppIntent = Intent(this@KeysFragment.requireContext(), DAppBrowserKeys::class.java)
                dAppIntent.putExtra("address", address)
                dAppIntent.putExtra("user", user)
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
                    putString(USER_ARG, param1)
                    putString(PRIVKEY_ARG, param2)
                    putString(PUBKEY_ARG, param3)
                    putString(CHAINCODE_ARG, param4)
                }
            }
    }
}
