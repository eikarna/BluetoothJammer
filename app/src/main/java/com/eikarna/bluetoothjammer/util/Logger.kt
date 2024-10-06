package util

import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date

class Logger {
    companion object {
        // Function to append log with a timestamp to a given TextView
        fun appendLog(textView: TextView, message: String) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = Date()
            val current = formatter.format(date)

            textView.append("\n[$current] $message")
        }
    }
}
