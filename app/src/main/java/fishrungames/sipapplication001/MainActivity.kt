package fishrungames.sipapplication001

import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import fishrungames.sipapplication001.databinding.ActivityMainBinding
import fishrungames.sipapplication001.ui.home.HomeFragment
import org.pjsip.pjsua2.CallInfo
import org.pjsip.pjsua2.pjsip_inv_state

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //private val TAG = "SipApplication001"

    inner class SipHandler : Handler.Callback {
        override fun handleMessage(m: Message): Boolean {
            runOnUiThread {
                if (m.what == SipConfig.MSG_UPDATE_CALL_INFO) {
                    val ci = m.obj as CallInfo

                    /* Update button text */
                    val buttonCall = getHomeFragment()?.btnCall
                    if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
                        buttonCall?.text = "Call"
                    else if (ci.state > pjsip_inv_state.PJSIP_INV_STATE_NULL)
                        buttonCall?.text = "Hangup"

                    /* Update call state text */
                    val textInfo = getHomeFragment()?.textView
                    textInfo?.text = ci.stateText

                    /* Hide video windows upon disconnection */
                    if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                        /*val svLocalVideo = findViewById<View>(R.id.svLocalVideo) as SurfaceView
                        val svRemoteVideo = findViewById<View>(R.id.svRemoteVideo) as SurfaceView
                        svLocalVideo.visibility = View.INVISIBLE
                        svRemoteVideo.visibility = View.INVISIBLE*/
                    }
                    //getHomeFragment()?.textView?.text += ci.
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
        //loadNativeLibraries()
        //checkPermissions()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        SipGlobal.uiHandler = Handler(this.mainLooper, SipHandler())

    }
/*
    private fun loadNativeLibraries() {
        try {
            System.loadLibrary("c++_shared")
            //Logger.debug(TAG, "libc++_shared loaded")
        } catch (error: UnsatisfiedLinkError) {
            //Logger.error(TAG, "Error while loading libc++_shared native library", error)
            throw RuntimeException(error)
        }
        try {
            System.loadLibrary("openh264")
            //Logger.debug(TAG, "OpenH264 loaded")
        } catch (error: UnsatisfiedLinkError) {
            //Logger.error(TAG, "Error while loading OpenH264 native library", error)
            throw RuntimeException(error)
        }
        try {
            System.loadLibrary("pjsua2")
            //Logger.debug(TAG, "PJSIP pjsua2 loaded")
        } catch (error: UnsatisfiedLinkError) {
            //Logger.error(TAG, "Error while loading PJSIP pjsua2 native library", error)
            throw RuntimeException(error)
        }
    }*/
}