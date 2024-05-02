package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.databinding.FragmentSecondBinding
import org.json.JSONObject


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            verify(binding.editviewName.text.toString(),
                    binding.editviewEmail.text.toString(),
                    binding.editviewMobile.text.toString(),
                    binding.editviewAadhar.text.toString())
            //findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    private fun verify(name : String , email : String , mobile : String , aadhar : String) {
        val queue = Volley.newRequestQueue(this.context)
        val url = "http://192.168.93.193:8000/verify/$aadhar"
        val req = StringRequest(
            Request.Method.GET, url,
            { _ ->
                register(name, email, mobile, aadhar)
            },
            {Toast.makeText(this.context , "Aadhar not valid" , Toast.LENGTH_LONG).show()})
        queue.add(req)
    }

    private fun register(name : String , email : String , mobile : String , aadhar : String){
        val queue = Volley.newRequestQueue(this.context)
            val url = "http://192.168.93.193:8080/personalinfo"
            val jsonBody = JSONObject()
            jsonBody.put("name", name)
            jsonBody.put("email", email)
            jsonBody.put("aadhar", aadhar)
            jsonBody.put("mobile", mobile)
            val mRequestBody = jsonBody.toString()
            val request = object : StringRequest(
                Method.POST, url,
                Response.Listener { response ->
                    if (response == "200") {
                        Log.i("Register", "Successful Registeration")
                        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
                    }
                },
                Response.ErrorListener { error ->
                    Log.i("Register", error.toString())
                }) {

                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                override fun getBody(): ByteArray {
                    return mRequestBody.toByteArray(Charsets.UTF_8)
                }

                override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                    var responseString = "";
                    if (response != null) {

                        responseString = response.statusCode.toString();

                    }
                    return Response.success(
                        responseString,
                        HttpHeaderParser.parseCacheHeaders(response)
                    );
                }
            }
            VolleyLog.DEBUG = true
            queue.add(request)
        }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}