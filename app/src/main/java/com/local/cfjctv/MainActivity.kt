package com.local.cfjctv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = false
        }
        setContentView(playerView)
    }

    override fun onStart() {
        super.onStart()
        scope.launch {
            val m3u8Url = withContext(Dispatchers.IO) {
                try {
                    val html = URL("https://cfjctoday.com").readText()
                    Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""").find(html)?.value
                } catch (e: Exception) { null }
            }
            if (m3u8Url != null) {
                player = ExoPlayer.Builder(this@MainActivity).build().also { exo ->
                    playerView.player = exo
                    exo.setMediaItem(MediaItem.fromUri(m3u8Url))
                    exo.playWhenReady = true
                    exo.prepare()
                }
            } else {
                Toast.makeText(this@MainActivity, "CFJC Live Stream is offline or unreachable.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            player?.let { if (it.isPlaying) it.pause() else it.play(); playerView.showController() }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
