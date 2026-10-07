package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.PaisaBachaoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val app = context.applicationContext as? PaisaBachaoApplication ?: return
            if (!app.securityManager.isSmsAutoScanEnabled()) {
                return
            }

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val fullBody = StringBuilder()
            var sender = ""
            var timestamp = System.currentTimeMillis()

            for (sms in messages) {
                fullBody.append(sms.messageBody)
                if (sender.isEmpty()) {
                    sender = sms.originatingAddress ?: "Unknown"
                    timestamp = sms.timestampMillis
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                app.smsReaderHelper.processSingleSms(
                    body = fullBody.toString(),
                    sender = sender,
                    timestamp = timestamp
                )
            }
        }
    }
}
