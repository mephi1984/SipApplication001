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

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено - можно начать звонок
            //startSipCall()
            btnStart.isEnabled = true
            btnStop.isEnabled = true
        } else {
            // Пользователь отказал
            showPermissionDeniedMessage()
        }
    }


    private fun checkAudioPermission() {
        when {
            // Разрешение уже есть
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSipCall()
            }

            // Нужно показать объяснение (для Android 11+)
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionExplanationDialog()
            }

            // Запрашиваем разрешение
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Требуется доступ к микрофону")
            .setMessage("Для совершения звонков необходимо разрешение на запись аудио")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Отмена") { _, _ ->
                showPermissionDeniedMessage()
            }
            .show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            requireContext(),
            "Без разрешения звонки невозможны",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startSipCall() {
        // Здесь ваш код для инициализации SIP-звонка
        Toast.makeText(requireContext(), "Микрофон доступен!", Toast.LENGTH_SHORT).show()
        btnStart.isEnabled = true
        btnStop.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        textView = binding.textView
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        btnStart = binding.button
        val etNumber = binding.editTextPhone

        btnStart.setOnClickListener {
            if (SipGlobal.ep.libGetState() > pjsua_state.PJSUA_STATE_NULL)
                return@setOnClickListener

            val epConfig = EpConfig()

            /* Setup our log writer */
            val logCfg = epConfig.logConfig
            SipGlobal.logWriter = MyLogWriter()
            logCfg.writer = SipGlobal.logWriter
            logCfg.decor = logCfg.decor and
                    (pj_log_decoration.PJ_LOG_HAS_CR or
                            pj_log_decoration.PJ_LOG_HAS_NEWLINE).inv().toLong()

            /* Create & init PJSUA2 */
            try {
                SipGlobal.ep.libCreate()
                SipGlobal.ep.libInit(epConfig)
            } catch (e: Exception) {
                println(e)
            }

            /* Create transports and account. */
            try {
                val sipTpConfig = TransportConfig()
                sipTpConfig.port = SipConfig.SIP_LISTENING_PORT.toLong()
                SipGlobal.ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                    sipTpConfig)

                SipGlobal.ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS,
                    TransportConfig())

                val accCfg = AccountConfig()
                accCfg.idUri = SipConfig.ACC_ID_URI
                accCfg.regConfig.registrarUri = SipConfig.ACC_REGISTRAR
                accCfg.sipConfig.authCreds.add(
                    AuthCredInfo("Digest", "*", SipConfig.ACC_USER, 0,
                        SipConfig.ACC_PASSWD)
                )
                accCfg.sipConfig.proxies.add( SipConfig.ACC_PROXY )

                accCfg.videoConfig.autoShowIncoming = false
                accCfg.videoConfig.autoTransmitOutgoing = false
                accCfg.videoConfig.defaultCaptureDevice = SipConfig.VIDEO_CAPTURE_DEVICE_ID
                SipGlobal.acc.create(accCfg, true)
            } catch (e: Exception) {
                println(e)
            }

            /* Start PJSUA2 */
            try {
                SipGlobal.ep.libStart()
            } catch (e: Exception) {
                println(e)
            }
            //findViewById<TextView>(R.id.text_info).text = "Library started"
            textView.text = "Library started"

            /* Prioritize AMR-WB */
            try {
                SipGlobal.ep.codecSetPriority("AMR-WB", 255)
                SipGlobal.ep.codecSetPriority("AMR/8000", 254)
            } catch (e: Exception) {
                println(e)
            }

            /* Fix camera orientation to portrait mode (for front camera) */
            /*
            try {
                SipGlobal.ep.vidDevManager().setCaptureOrient(SipConfig.VIDEO_CAPTURE_DEVICE_ID,
                    pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG, true)

                /* Also adjust size in H264 encoding param */
                var codecs = SipGlobal.ep.videoCodecEnum2()
                var codecId = "H264/"
                for (c in codecs) {
                    if (c.codecId.startsWith(codecId)) {
                        codecId = c.codecId
                        break
                    }
                }
                var vcp = SipGlobal.ep.getVideoCodecParam(codecId)
                vcp.encFmt.width = 480
                vcp.encFmt.height = 640
                vcp.encFmt.avgBps = 1024000
                vcp.encFmt.maxBps = 5000000
                SipGlobal.ep.setVideoCodecParam(codecId, vcp)
            } catch (e: Exception) {
                println(e)
            }*/

        }

        btnStop = binding.button2
        btnStop.setOnClickListener {
            if (SipGlobal.ep.libGetState() == pjsua_state.PJSUA_STATE_NULL)
                return@setOnClickListener

            /* Destroy PJSUA2 */
            try {
                SipGlobal.ep.hangupAllCalls()
                SipGlobal.ep.libDestroy()
            } catch (e: Exception) {
                println(e)
            }
            //findViewById<TextView>(R.id.text_info).text = "Library stopped"
            textView.text = "Library stopped"
        }

        btnCall =  binding.button3

        btnCall.setOnClickListener {

            if (SipGlobal.ep.libGetState() != pjsua_state.PJSUA_STATE_RUNNING)
                return@setOnClickListener

            if (SipGlobal.call == null) {
                try {
                    /* Setup null audio (good for emulator) */
                    //SipGlobal.ep.audDevManager().setNullDev()

                    val audDevManager = SipGlobal.ep.audDevManager()

                    // Получаем список доступных устройств
                    val devices = audDevManager.enumDev2()
                    println("Available audio devices: $devices")

                    // Устанавливаем устройства по умолчанию (или выбираем конкретные)
                    audDevManager.setPlaybackDev(audDevManager.getPlaybackDev())
                    audDevManager.setCaptureDev(audDevManager.getCaptureDev())

                    /* Make call (to itself) */
                    val call = MyCall(SipGlobal.acc, -1)
                    val prm = CallOpParam(true)
                    call.makeCall(SipConfig.CALL_DST_URI, prm)
                    SipGlobal.call = call
                } catch (e: Exception) {
                    println(e)
                }
            } else {
                try {
                    SipGlobal.ep.hangupAllCalls()
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        checkAudioPermission()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}