package com.example.myapplication

import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom


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
        val subject = X500Name("CN=Dummy$androidId, C=IN")
        val csrBuilder = JcaPKCS10CertificationRequestBuilder(X500Name.getInstance(subject) , publicKey)
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(privateKey)
        val csr = csrBuilder.build(signer)
        val sw = StringWriter()
        JcaPEMWriter(sw).use { jpw -> jpw.writeObject(csr) }
        val pem = sw.toString()
        binding.csr.text = "CSR : $pem"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}