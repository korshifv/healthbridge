package dev.healthbridge

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "HealthBridge использует разрешение WRITE_NUTRITION только для записи еды, которую вы явно передали приложению. Приложение не запрашивает чтение медицинских или фитнес-данных."
            setPadding(32, 64, 32, 32)
        })
    }
}
