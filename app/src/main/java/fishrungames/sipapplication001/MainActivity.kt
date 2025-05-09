package fishrungames.sipapplication001

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import fishrungames.sipapplication001.databinding.ActivityMainBinding
import fishrungames.sipapplication001.ui.home.HomeFragment
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.CallInfo
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.pj_log_decoration
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_transport_type_e
import org.pjsip.pjsua2.pjsua_state

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var dialog: AlertDialog
    private lateinit var dialogTextView: TextView
    private lateinit var dialogTextView2: TextView
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            dialogTextView2.text = "Seconds passed: $seconds"
            handler.postDelayed(this, 1000)
        }
    }

    inner class SipHandler : Handler.Callback {
        override fun handleMessage(m: Message): Boolean {
            runOnUiThread {
                if (m.what == SipConfig.MSG_UPDATE_CALL_INFO) {
                    val ci = m.obj as CallInfo

                    //val buttonCall = getHomeFragment()?.btnCall
                    if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
                    {
                        dialog.setMessage("Calling...")
                    }
                    else if (ci.state > pjsip_inv_state.PJSIP_INV_STATE_NULL) {
                        dialog.setMessage("Hangup")
                    }

                    dialogTextView.text = ci.stateText
                }
            }
            return true
        }
    }

    private fun getHomeFragment(): HomeFragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it is HomeFragment } as? HomeFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        SipGlobal.uiHandler = Handler(this.mainLooper, SipHandler())

        startSip()
        //showCallDialog()

    }

    fun showCallDialog() {
        seconds = 0
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.call_dialog_layout, null)

        dialogTextView = dialogView.findViewById(R.id.dialog_text)
        dialogTextView2 = dialogView.findViewById(R.id.dialog_text2)
        val closeButton = dialogView.findViewById<TextView>(R.id.close_button)

        builder.setView(dialogView)
        dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()

        handler.post(timerRunnable)

        closeButton.setOnClickListener {
            try {
                hideCallDialog()
                getHomeFragment()?.btnCall?.isEnabled = true
                if (SipGlobal.call != null) {
                    SipGlobal.ep.hangupAllCalls()
                }
            } catch (e: Exception) {
                println(e)
            }
            SipGlobal.call = null
        }
    }

    fun hideCallDialog()
    {
        handler.removeCallbacks(timerRunnable)
        dialog.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSip()
        handler.removeCallbacks(timerRunnable)
    }

    fun startSip()
    {
        if (SipGlobal.ep.libGetState() > pjsua_state.PJSUA_STATE_NULL)
            return

        val epConfig = EpConfig()

        val logCfg = epConfig.logConfig
        SipGlobal.logWriter = MyLogWriter()
        logCfg.writer = SipGlobal.logWriter
        logCfg.decor = logCfg.decor and
                (pj_log_decoration.PJ_LOG_HAS_CR or
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE).inv().toLong()

        try {
            SipGlobal.ep.libCreate()
            SipGlobal.ep.libInit(epConfig)
        } catch (e: Exception) {
            println(e)
        }

        try {
            val sipTpConfig = TransportConfig()
            sipTpConfig.port = SipConfig.SIP_LISTENING_PORT.toLong()
            SipGlobal.ep.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                sipTpConfig)

            SipGlobal.ep.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_TLS,
                TransportConfig()
            )

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

        getHomeFragment()?.textView?.text = "PJSUA2 started successfully. Enter number to call:"

        try {
            SipGlobal.ep.codecSetPriority("AMR-WB", 255)
            SipGlobal.ep.codecSetPriority("AMR/8000", 254)
        } catch (e: Exception) {
            println(e)
        }

    }

    fun stopSip()
    {
        if (SipGlobal.ep.libGetState() == pjsua_state.PJSUA_STATE_NULL)
            return
        try {
            SipGlobal.ep.hangupAllCalls()
            SipGlobal.ep.libDestroy()
        } catch (e: Exception) {
            println(e)
        }

        getHomeFragment()?.textView?.text = "PJSUA2 stopped. Calls are not available"
    }


}