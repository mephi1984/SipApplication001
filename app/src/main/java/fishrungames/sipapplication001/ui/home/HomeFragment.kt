package fishrungames.sipapplication001.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import fishrungames.sipapplication001.MainActivity
import fishrungames.sipapplication001.MyCall
import fishrungames.sipapplication001.MyLogWriter
import fishrungames.sipapplication001.databinding.FragmentHomeBinding
import org.pjsip.pjsua2.*
import fishrungames.sipapplication001.SipConfig
import fishrungames.sipapplication001.SipGlobal

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    lateinit var btnCall: Button

    lateinit var textView : TextView

    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableSipCall()
        } else {
            showPermissionDeniedMessage()
            disableSipCall()
        }
    }


    private fun checkAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableSipCall()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionExplanationDialog()
                disableSipCall()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Need microphone access")
            .setMessage("To make call, you need to allow the app to use microphone")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Cancel") { _, _ ->
                showPermissionDeniedMessage()
                disableSipCall()
            }
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            requireContext(),
            "Calls are impossible without microphone access",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun enableSipCall() {
        btnCall.isEnabled = true
    }

    private fun disableSipCall() {
        btnCall.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        textView = binding.textView

        textView.text = "Enter number to call:"

        val etNumber = binding.editTextPhone

        binding.button1.setOnClickListener {
            etNumber.text.append("1")
        }
        binding.button2.setOnClickListener {
            etNumber.text.append("2")
        }
        binding.button3.setOnClickListener {
            etNumber.text.append("3")
        }
        binding.button4.setOnClickListener {
            etNumber.text.append("4")
        }
        binding.button5.setOnClickListener {
            etNumber.text.append("5")
        }
        binding.button6.setOnClickListener {
            etNumber.text.append("6")
        }
        binding.button7.setOnClickListener {
            etNumber.text.append("7")
        }
        binding.button8.setOnClickListener {
            etNumber.text.append("8")
        }
        binding.button9.setOnClickListener {
            etNumber.text.append("9")
        }
        binding.button0.setOnClickListener {
            etNumber.text.append("0")
        }
        binding.buttonPlus.setOnClickListener {
            etNumber.text.append("+")
        }
        binding.buttonClear.setOnClickListener {
            if (etNumber.text.isNotEmpty()) {
                etNumber.text.delete(etNumber.text.length - 1, etNumber.text.length) // Удаляем последний символ
            }
        }

        btnCall =  binding.buttonCall

        btnCall.setOnClickListener {

            if (SipGlobal.ep.libGetState() != pjsua_state.PJSUA_STATE_RUNNING)
                return@setOnClickListener

            if (SipGlobal.call == null) {
                try {
                    val audDevManager = SipGlobal.ep.audDevManager()

                    audDevManager.setPlaybackDev(audDevManager.getPlaybackDev())
                    audDevManager.setCaptureDev(audDevManager.getCaptureDev())

                    val call = MyCall(SipGlobal.acc, -1)
                    val prm = CallOpParam(true)

                    val destination = "sip:" + etNumber.text + "@localphone.com;user=phone"
                    call.makeCall(destination, prm)
                    SipGlobal.call = call

                    this.btnCall.isEnabled = false
                    (requireActivity() as MainActivity).showCallDialog()
                } catch (e: Exception) {
                    println(e)
                }
            }/* else {
                try {
                    (requireActivity() as MainActivity).hideCallDialog()
                    this.btnCall.isEnabled = true
                    SipGlobal.ep.hangupAllCalls()
                } catch (e: Exception) {
                    println(e)
                }
            }*/
        }

        checkAudioPermission()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}