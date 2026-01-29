package com.game.reschange

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.slider.Slider
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.DataOutputStream
import java.util.Locale
import androidx.core.content.edit
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var toggleModified: SwitchMaterial
    private lateinit var allApps: List<AppInfo>
    private var showOnlyModified = false
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.myToolbar))

        recyclerView = findViewById(R.id.appList)
        toggleModified = findViewById(R.id.toggleModified)
        recyclerView.layoutManager = LinearLayoutManager(this)

        allApps = getUserInstalledApps()
        adapter = AppListAdapter(allApps) { appInfo ->
            showResolutionDialog(appInfo.packageName)
        }
        recyclerView.adapter = adapter

        toggleModified.setOnCheckedChangeListener { _, isChecked ->
            showOnlyModified = isChecked
            filterAppList()
        }

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            val packages = ScalePrefs.getAllPackages(this)
            for (packageName in packages) {
                runAsRoot("am force-stop $packageName")
            }
            getSharedPreferences("scale_prefs", Context.MODE_PRIVATE).edit { clear() }
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Tüm ayarlar sıfırlandı.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                filterAppList()
                return true
            }
        })
        return true
    }

    private fun filterAppList() {
        var tempList = allApps
        if (showOnlyModified) {
            tempList = tempList.filter { app -> ScalePrefs.getScale(this, app.packageName) != 1.0f }
        }
        if (currentQuery.isNotEmpty()) {
            val lowerQ = currentQuery.lowercase(Locale.getDefault())
            tempList = tempList.filter { app ->
                app.name.lowercase(Locale.getDefault()).contains(lowerQ) ||
                app.packageName.lowercase(Locale.getDefault()).contains(lowerQ)
            }
        }
        adapter.submitList(tempList)
    }

    private fun getUserInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .distinctBy { it.packageName }
            .map { AppInfo(name = pm.getApplicationLabel(it).toString(), packageName = it.packageName, icon = pm.getApplicationIcon(it)) }
            .sortedBy { it.name.lowercase() }
    }

    private fun showResolutionDialog(packageName: String) {
        val savedScale = ScalePrefs.getScale(this, packageName)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val scaleText = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 20)
            text = "Scale: ${(savedScale * 100).toInt()}%"
        }
        val slider = Slider(this).apply {
            valueFrom = 0.3f
            valueTo = 1.0f
            stepSize = 0.05f
            value = savedScale
        }
        slider.addOnChangeListener { _, value, _ -> scaleText.text = "Scale: ${(value * 100).toInt()}%" }
        layout.addView(scaleText)
        layout.addView(slider)

        MaterialAlertDialogBuilder(this, R.style.MyRoundedDialog)
            .setTitle("Çözünürlük Ayarı")
            .setView(layout)
            .setPositiveButton("Uygula") { _, _ ->
                val scale = String.format(Locale.US, "%.2f", slider.value).toFloat()
                ScalePrefs.saveScale(this, packageName, scale)
                adapter.notifyDataSetChanged()
                
                // Sadece uygulamayı durduruyoruz, kanca açılışta çalışacak
                runAsRoot("am force-stop $packageName")
                Toast.makeText(this, "Ayarlar kaydedildi. Uygulayı açın.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .setNeutralButton("Sıfırla") { _, _ ->
                ScalePrefs.saveScale(this, packageName, 1.0f)
                runAsRoot("am force-stop $packageName")
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Varsayılana dönüldü.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun runAsRoot(command: String) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            Toast.makeText(this, "Root hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
