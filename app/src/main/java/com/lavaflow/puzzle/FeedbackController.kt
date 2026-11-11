package com.lavaflow.puzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import com.lavaflow.puzzle.core.FeedbackSettings

enum class FeedbackEvent { UI, ROTATE, FLOW, INVALID, SELECT, HAMMER, PURCHASE, WIN, LOSE, ACHIEVEMENT }

class FeedbackController(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded = mutableSetOf<Int>()
    init { soundPool.setOnLoadCompleteListener { _, sample, status -> if (status == 0) loaded.add(sample) } }
    private val sounds = mapOf(FeedbackEvent.UI to R.raw.sfx_ui, FeedbackEvent.ROTATE to R.raw.sfx_rotate,
        FeedbackEvent.FLOW to R.raw.sfx_flow, FeedbackEvent.INVALID to R.raw.sfx_invalid, FeedbackEvent.SELECT to R.raw.sfx_select,
        FeedbackEvent.HAMMER to R.raw.sfx_hammer, FeedbackEvent.PURCHASE to R.raw.sfx_purchase, FeedbackEvent.WIN to R.raw.sfx_win,
        FeedbackEvent.LOSE to R.raw.sfx_lose, FeedbackEvent.ACHIEVEMENT to R.raw.sfx_achievement).mapValues { soundPool.load(context, it.value, 1) }
    private val streams = ArrayDeque<Int>()
    private val lastPlayed = mutableMapOf<FeedbackEvent, Long>()
    private var music: MediaPlayer? = null
    private var settings = FeedbackSettings()
    private var resumed = false
    private var focusGranted = false
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener { change ->
            focusGranted = change == AudioManager.AUDIOFOCUS_GAIN
            if (!focusGranted) music?.pause() else reconcile()
        }.build()

    fun update(value: FeedbackSettings) {
        settings = value
        if (!settings.sound) stopEffects()
        if (!settings.vibration) vibrator?.cancel()
        reconcile()
    }
    fun resume() { resumed = true; reconcile() }
    fun pause() {
        resumed = false
        music?.pause()
        stopEffects()
        vibrator?.cancel()
        audioManager.abandonAudioFocusRequest(focus)
        focusGranted = false
    }
    private fun stopEffects() { streams.forEach(soundPool::stop); streams.clear() }
    fun click(purchase: Boolean = false) = play(if (purchase) FeedbackEvent.PURCHASE else FeedbackEvent.UI)
    fun play(event: FeedbackEvent) {
        if (!resumed) return
        val now = SystemClock.elapsedRealtime()
        if (now - (lastPlayed[event] ?: 0) < if (event == FeedbackEvent.FLOW) 180 else 45) return
        lastPlayed[event] = now
        val sound = sounds.getValue(event)
        if (settings.sound && sound in loaded) {
            val volume = if (event == FeedbackEvent.FLOW) .25f else .55f
            streams.addLast(soundPool.play(sound, volume, volume, 1, 0, 1f))
            while (streams.size > 8) streams.removeFirst()
        }
        val duration = when (event) {
            FeedbackEvent.FLOW -> 0L; FeedbackEvent.HAMMER -> 50L
            FeedbackEvent.WIN, FeedbackEvent.ACHIEVEMENT -> 65L; FeedbackEvent.PURCHASE -> 40L; else -> 15L
        }
        if (duration > 0 && settings.vibration && vibrator?.hasVibrator() == true)
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    private fun reconcile() {
        if (!resumed || !settings.music) {
            music?.pause()
            if (focusGranted) { audioManager.abandonAudioFocusRequest(focus); focusGranted = false }
            return
        }
        if (!focusGranted) focusGranted = audioManager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!focusGranted) return
        if (music == null) music = MediaPlayer.create(context, R.raw.lava_ambience)?.apply { isLooping = true; setVolume(0.22f, 0.22f) }
        if (music?.isPlaying == false) music?.start()
    }
    fun release() { pause(); music?.release(); music = null; soundPool.release() }
}
