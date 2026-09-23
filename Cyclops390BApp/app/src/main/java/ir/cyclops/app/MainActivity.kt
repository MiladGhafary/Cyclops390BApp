package ir.cyclops.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: TubeAdapter

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) startService()
        else Toast.makeText(this, "دسترسی‌ها لازم است", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppState.init(this)

        val rv = findViewById<RecyclerView>(R.id.tubeList)
        adapter = TubeAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val status = findViewById<TextView>(R.id.statusText)
        val lastTemp = findViewById<TextView>(R.id.lastTempText)
        val currentTube = findViewById<TextView>(R.id.currentTubeText)
        val currentNum = findViewById<TextView>(R.id.currentNumText)

        lifecycleScope.launch {
            AppState.status.collect { status.text = "وضعیت: $it" }
        }
        lifecycleScope.launch {
            AppState.lastTemperature.collect { t ->
                lastTemp.text = t?.let { "آخرین دما: %.1f°C".format(it) }
                    ?: "آخرین دما: —"
            }
        }
        lifecycleScope.launch {
            AppState.currentIndex.collect { idx ->
                val list = AppState.tubes.value
                adapter.submit(list, idx)
                if (idx < list.size) {
                    currentTube.text = "تیوب جاری: ${list[idx].id}"
                    currentNum.text = "${idx + 1} / ${list.size}"
                } else {
                    currentTube.text = "تمام شد"
                    currentNum.text = "${idx} / ${list.size}"
                }
            }
        }

        findViewById<Button>(R.id.btnConnect).setOnClickListener { requestPermsAndStart() }
        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            AppState.undo()
            Toast.makeText(this, "آخرین مورد حذف شد", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            AppState.reset()
            Toast.makeText(this, "ریست شد", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportCsv() }
    }

    private fun requestPermsAndStart() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isEmpty()) startService()
        else permLauncher.launch(perms.toTypedArray())
    }

    private fun startService() {
        val i = Intent(this, BluetoothService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
    }

    private fun exportCsv() {
        try {
            val f = File(getExternalFilesDir(null),
                "cyclops_