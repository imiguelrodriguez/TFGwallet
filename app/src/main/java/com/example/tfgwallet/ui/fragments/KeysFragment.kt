package com.example.tfgwallet.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfgwallet.databinding.FragmentKeysBinding
import com.example.tfgwallet.model.KeyItem
import com.example.tfgwallet.model.MasterKeyItem
import com.example.tfgwallet.ui.MasterKeyItemAdapter

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
        setKeysDataAdapter()
        setupRecyclerView()
        // Inflate the layout for this fragment using data binding
        return binding.root
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.masterRecycler
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = mAdapter
    }

    private fun setKeysDataAdapter() {
        val keys: MutableList<MasterKeyItem> = ArrayList()
        val scHash = context?.getSharedPreferences("user_$user", Context.MODE_PRIVATE)
            ?.getString("user_${user}_contract", "").toString()
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
