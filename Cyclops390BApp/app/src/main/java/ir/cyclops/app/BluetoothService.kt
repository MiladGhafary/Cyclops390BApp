handleLine(line: String) {
        val temp = TemperatureParser.parse(line) ?: return
        val now = System.currentTimeMillis()
        if (now < ignoreDataUntil) return

        when (state) {
            State.IDLE -> {
                state = State.STREAMING
                lastTemperature = temp
                lastLineTime = now
                AppState.lastTemperature.value = temp
                updateStatus("در حال اندازه‌گیری...")
            }
            State.STREAMING -> {
                lastTemperature = temp
                lastLineTime = now
                AppState.lastTemperature.value = temp
            }
            State.WAIT_CONFIRM -> {
                val t = lastTemperature
                if (t != null) {
                    if (AppState.confirmCurrent(t)) {
                        val idx = AppState.currentIndex.value
                        speak("تأیید. تیوب $idx")
                        updateStatus("تأیید شد: $t درجه")
                    } else {
                        speak("همه تیوب‌ها پر شد")
                        updateStatus("تمام شد")
                    }
                }
                state = State.IDLE
                lastTemperature = null
                AppState.lastTemperature.value = null
                ignoreDataUntil = now + COOLDOWN_MS
            }
        }
    }

    private fun startTicker() {
        tickJob = scope.launch {
            while (isActive) {
                delay(120)
                val now = System.currentTimeMillis()
                when (state) {
                    State.STREAMING -> {
                        if (now - lastLineTime > STREAM_STOP_MS) {
                            state = State.WAIT_CONFIRM
                            confirmStartTime = now
                            updateStatus("برای تأیید، ماشه را دوباره بزنید")
                            speak("تأیید")
                        }
                    }
                    State.WAIT_CONFIRM -> {
                        if (now - confirmStartTime > CONFIRM_WINDOW_MS) {
                            state = State.IDLE
                            lastTemperature = null
                            AppState.lastTemperature.value = null
                            updateStatus("رد شد. دوباره بگیرید")
                            speak("رد شد")
                        }
                    }
                    State.IDLE -> {}
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun updateStatus(s: String) {
        AppState.status.value = s
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIF_ID, buildNotification(s))
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cyclops")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Cyclops Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        try { socket?.close() } catch (_: Exception) {}
        tts?.shutdown()
        super.onDestroy()
    }
}