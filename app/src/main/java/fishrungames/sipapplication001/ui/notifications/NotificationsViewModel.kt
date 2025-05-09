package fishrungames.sipapplication001.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This application was written by Vladislav Khorev"
    }
    val text: LiveData<String> = _text
}