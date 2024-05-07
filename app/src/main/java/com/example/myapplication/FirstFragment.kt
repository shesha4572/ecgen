package com.example.myapplication

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.databinding.FragmentFirstBinding
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.encoders.UTF8
import org.json.JSONObject
import java.io.StringWriter
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.util.Base64


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            genKeys()
        }
    }

    private fun genKeys(){
        val TAG = "GenKey"
        var randomGen : BigInteger = BigIntegers.createRandomBigInteger(256 , SecureRandom())
        binding.randomNum.text = "Random Number : ${randomGen.toString(16)}"
        val androidId = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.ANDROID_ID)
        binding.deviceId.text = "Device ID : $androidId"
        randomGen = randomGen.xor(BigInteger(androidId , 16))
        val ecParamSpec : ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val ecPrivKeySpec  = ECPrivateKeySpec(randomGen , ecParamSpec)
        val keyFactory: KeyFactory = KeyFactory.getInstance("EC" , "BC")
        val privateKey : PrivateKey = keyFactory.generatePrivate(ecPrivKeySpec)
        val Q: ECPoint = ecParamSpec.g.multiply(randomGen)
        val publicKey: PublicKey = keyFactory.generatePublic(ECPublicKeySpec(Q, ecParamSpec))
        binding.publicKey.text = "Public Key : $publicKey"
        binding.privateKey.text = "Private Key : $privateKey"
        val keyPair = KeyPair(publicKey , privateKey)
        val dummy = BigIntegers.createRandomBigInteger(64 , SecureRandom()).toString(16)
        val subject = X500Name("CN=$dummy, C=IN")
        val csrBuilder = JcaPKCS10CertificationRequestBuilder(X500Name.getInstance(subject) , publicKey)
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(privateKey)
        val csr = csrBuilder.build(signer)
        val sw = StringWriter()
        JcaPEMWriter(sw).use { jpw -> jpw.writeObject(csr) }
        val pem = sw.toString()
        binding.csr.text = "CSR : $pem"
        val req = JSONObject()
        req.put("csr" , pem)
        val jsonReq = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.10.193:8080/csr",
            req,
            {response ->
                val cert = response.getString("certificate")
                binding.csr.text = "CERTIFICATE : $cert"

            },
            { error -> Toast.makeText(this.context , error.message , Toast.LENGTH_LONG).show() }
        )

        // Set timeout for the request (in milliseconds)
        val timeoutMilliseconds = 10000 // 10 seconds
        jsonReq.setRetryPolicy(DefaultRetryPolicy(timeoutMilliseconds,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))

        val queue = Volley.newRequestQueue(this.context)
        queue.add(jsonReq)
        val dataToSign = (activity as MainActivity).data
        val signature = Signature.getInstance("SHA256withECDSA")

// Initialize the Signature object with your private key
        signature.initSign(privateKey)

// Convert your data to bytes
        val dataBytes = dataToSign.toByteArray()

// Update the data in the signature object
        signature.update(dataBytes)

// Sign the data
        val signedData = signature.sign()

// Convert the signature to a Base64 string if needed
        val base64Signature = Base64.getEncoder().encodeToString(signedData)
        Log.i("SignedData", "Signed Data: ${String(signedData)}")
        Log.i("SignatureBase64", "Signature (Base64): $base64Signature")


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}