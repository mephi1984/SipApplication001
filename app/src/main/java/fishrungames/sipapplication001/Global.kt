package fishrungames.sipapplication001

import android.os.Handler
import android.os.Message
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallInfo
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter
import org.pjsip.pjsua2.OnCallMediaEventParam
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.VideoPreview
import org.pjsip.pjsua2.pjmedia_dir
import org.pjsip.pjsua2.pjmedia_event_type
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_status_code
import org.pjsip.pjsua2.pjsua2
import org.pjsip.pjsua2.pjsua_call_media_status

object SipConfig {
    // Account ID
    const val ACC_DOMAIN = "localphone.com"
    const val ACC_USER = ""
    const val ACC_PASSWD = ""
    val ACC_ID_URI = "SipTestApp <sip:$ACC_USER@$ACC_DOMAIN>"
    const val ACC_REGISTRAR = "sip:localphone.com;transport=udp"
    const val ACC_PROXY = "sip:proxy.localphone.com;lr;transport=udp"

    // Peer to call
    //const val CALL_DST_URI = "sip:77716924641@localphone.com;user=phone"
    //const val CALL_DST_URI = "sip:996773610568@localphone.com;user=phone"
    const val CALL_DST_URI = "sip:447537174394@localphone.com;user=phone"

    // Camera ID
    const val VIDEO_CAPTURE_DEVICE_ID = -1

    // SIP transport listening port
    const val SIP_LISTENING_PORT = 6000

    // Message constants
    const val MSG_UPDATE_CALL_INFO = 1
    const val MSG_SHOW_REMOTE_VIDEO = 2
    const val MSG_SHOW_LOCAL_VIDEO = 3
}

class MyLogWriter : LogWriter() {
    override fun write(entry: LogEntry) {
        println(entry.msg)
    }
}


fun getCallInfo(call: Call): CallInfo? {
    var ci : CallInfo? = null
    try {
        ci = call.info
    } catch (e: Exception) {
        println("Failed getting call info: $e")
    }
    return ci
}

/* Call implementation */
class MyCall(acc: Account, call_id: Int) : Call(acc, call_id) {

    override fun onCallState(prm: OnCallStateParam?) {
        val ci : CallInfo = getCallInfo(this) ?: return

        SipGlobal.ep.utilLogWrite(3, "MyCall", "Call state changed to: " + ci.stateText)
        val m = Message.obtain(SipGlobal.uiHandler, SipConfig.MSG_UPDATE_CALL_INFO, ci)
        m.sendToTarget()

        if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
            SipGlobal.call = null
            SipGlobal.ep.utilLogWrite(3, "MyCall", this.dump(true, ""))
        }
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam?) {
        val ci : CallInfo = getCallInfo(this) ?: return
        val cmiv = ci.media
        for (i in cmiv.indices) {
            val cmi = cmiv[i]
            if (cmi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                (cmi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                        cmi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD))
            {
                /* Connect ports */
                try {
                    val am = getAudioMedia(i)
                    SipGlobal.ep.audDevManager().captureDevMedia.startTransmit(am)
                    am.startTransmit(SipGlobal.ep.audDevManager().playbackDevMedia)
                } catch (e: Exception) {
                    println("Failed connecting media ports" + e.message)
                }
            } else if ((cmi.type == pjmedia_type.PJMEDIA_TYPE_VIDEO) &&
                (cmi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) &&
                (cmi.dir and pjmedia_dir.PJMEDIA_DIR_ENCODING) != 0)
            {
                val m = Message.obtain(SipGlobal.uiHandler, SipConfig.MSG_SHOW_LOCAL_VIDEO, cmi)
                m.sendToTarget()
            }
        }
    }

    override fun onCallMediaEvent(prm: OnCallMediaEventParam?) {
        if (prm!!.ev.type == pjmedia_event_type.PJMEDIA_EVENT_FMT_CHANGED) {
            val ci : CallInfo = getCallInfo(this) ?: return
            if (prm.medIdx < 0 || prm.medIdx >= ci.media.size)
                return

            /* Check if this event is from incoming video */
            val cmi = ci.media[prm.medIdx.toInt()]
            if (cmi.type != pjmedia_type.PJMEDIA_TYPE_VIDEO ||
                cmi.status != pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                cmi.videoIncomingWindowId == pjsua2.INVALID_ID)
                return

            /* Currently this is a new incoming video */
            println("Got remote video format change = " +prm.ev.data.fmtChanged.newWidth + "x" + prm.ev.data.fmtChanged.newHeight)
            val m = Message.obtain(SipGlobal.uiHandler, SipConfig.MSG_SHOW_REMOTE_VIDEO, cmi)
            m.sendToTarget()
        }
    }

}

/* Account implementation */
class MyAccount : Account() {
    override fun onIncomingCall(prm: OnIncomingCallParam) {
        /* Auto answer with 200 for incoming calls  */
        val call = MyCall(SipGlobal.acc, prm.callId)
        val ansPrm = CallOpParam(true)
        ansPrm.statusCode = if (SipGlobal.call == null) pjsip_status_code.PJSIP_SC_OK else pjsip_status_code.PJSIP_SC_BUSY_HERE
        try {
            call.answer(ansPrm)
            if (SipGlobal.call == null)
                SipGlobal.call = call
        } catch (e: Exception) {
            println(e)
        }
    }
}

object SipGlobal {
    /* Maintain reference to avoid auto garbage collecting */
    lateinit var logWriter: MyLogWriter

    /* Message handler for updating UI */
    lateinit var uiHandler: Handler

    val ep = Endpoint()
    val acc = MyAccount()
    var call: MyCall? = null

    var previewStarted = false
}