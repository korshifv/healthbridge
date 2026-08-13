package dev.healthbridge

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var writer: HealthConnectWriter
    private lateinit var status: TextView
    private lateinit var linkField: EditText
    private lateinit var nameField: EditText
    private lateinit var kcalField: EditText
    private lateinit var proteinField: EditText
    private lateinit var fatField: EditText
    private lateinit var carbsField: EditText
    private lateinit var sugarField: EditText
    private lateinit var mealSpinner: Spinner

    private var pendingPayload: NutritionPayload? = null

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (writer.writePermission in granted) {
            status.text = "Разрешение получено"
            pendingPayload?.let { payload ->
                pendingPayload = null
                write(payload)
            }
        } else {
            status.text = "Нет разрешения WRITE_NUTRITION"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            setContentView(TextView(this).apply {
                text = "Health Connect недоступен или требует обновления (status=$sdkStatus)"
                setPadding(32, 64, 32, 32)
            })
            return
        }

        writer = HealthConnectWriter(this)
        buildUi()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 40)
        }

        status = TextView(this).apply { text = "HealthBridge" }
        root.addView(status, matchWrap())

        linkField = EditText(this).apply {
            hint = "Ссылка из ChatGPT: healthbridge://nutrition?..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
        }
        root.addView(linkField, matchWrap())

        root.addView(Button(this).apply {
            text = "Вставить из буфера и распарсить"
            setOnClickListener { pasteAndParse() }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "Распарсить ссылку"
            setOnClickListener { parseIncomingText(linkField.text.toString()) }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "Дать доступ к питанию"
            setOnClickListener { requestPermissions.launch(setOf(writer.writePermission)) }
        }, matchWrap())

        nameField = field("Название")
        kcalField = numberField("ккал")
        proteinField = numberField("Белки, г")
        fatField = numberField("Жиры, г")
        carbsField = numberField("Углеводы, г")
        sugarField = numberField("Сахар, г (необязательно)")

        listOf(nameField, kcalField, proteinField, fatField, carbsField, sugarField)
            .forEach { root.addView(it, matchWrap()) }

        mealSpinner = Spinner(this)
        mealSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("unknown", "breakfast", "lunch", "dinner", "snack")
        )
        root.addView(mealSpinner, matchWrap())

        root.addView(Button(this).apply {
            text = "Записать в Health Connect"
            setOnClickListener { saveFromForm() }
        }, matchWrap())

        setContentView(root)
    }

    private fun field(hint: String) = EditText(this).apply { this.hint = hint }

    private fun numberField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun pasteAndParse() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            .orEmpty()

        if (text.isBlank()) {
            Toast.makeText(this, "Буфер пуст", Toast.LENGTH_SHORT).show()
            return
        }

        linkField.setText(text)
        parseIncomingText(text)
    }

    private fun handleIntent(intent: Intent) {
        val payload = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let(NutritionPayload::fromUri)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(NutritionPayload::fromText)
            else -> intent.data?.let(NutritionPayload::fromUri)
        } ?: return

        applyIncomingPayload(payload)
    }

    private fun parseIncomingText(text: String) {
        val payload = NutritionPayload.fromText(text)
        if (payload == null) {
            status.text = "Не нашёл HealthBridge-ссылку"
            Toast.makeText(this, "В тексте нет корректной healthbridge://nutrition ссылки", Toast.LENGTH_SHORT).show()
            return
        }
        applyIncomingPayload(payload)
    }

    private fun applyIncomingPayload(payload: NutritionPayload) {
        fillForm(payload)
        status.text = "Получено: ${payload.name}, ${payload.kcal.clean()} ккал"

        if (payload.autocommit) {
            saveOrRequest(payload)
        }
    }

    private fun fillForm(payload: NutritionPayload) {
        nameField.setText(payload.name)
        kcalField.setText(payload.kcal.clean())
        proteinField.setText(payload.protein?.clean().orEmpty())
        fatField.setText(payload.fat?.clean().orEmpty())
        carbsField.setText(payload.carbs?.clean().orEmpty())
        sugarField.setText(payload.sugar?.clean().orEmpty())
        mealSpinner.setSelection(
            listOf("unknown", "breakfast", "lunch", "dinner", "snack")
                .indexOf(payload.meal).coerceAtLeast(0)
        )
    }

    private fun saveFromForm() {
        val name = nameField.text.toString().trim()
        val kcal = kcalField.text.toString().toDoubleOrNull()
        if (name.isBlank() || kcal == null) {
            Toast.makeText(this, "Нужны название и ккал", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = NutritionPayload(
            id = UUID.randomUUID().toString(),
            name = name,
            meal = mealSpinner.selectedItem.toString(),
            kcal = kcal,
            protein = proteinField.text.toString().toDoubleOrNull(),
            fat = fatField.text.toString().toDoubleOrNull(),
            carbs = carbsField.text.toString().toDoubleOrNull(),
            sugar = sugarField.text.toString().toDoubleOrNull(),
            at = Instant.now(),
            autocommit = true,
        )
        saveOrRequest(payload)
    }

    private fun saveOrRequest(payload: NutritionPayload) {
        scope.launch {
            if (writer.hasWritePermission()) {
                write(payload)
            } else {
                pendingPayload = payload
                requestPermissions.launch(setOf(writer.writePermission))
            }
        }
    }

    private fun write(payload: NutritionPayload) {
        scope.launch {
            runCatching { writer.insert(payload) }
                .onSuccess {
                    status.text = "✓ Записано: ${payload.name} — ${payload.kcal.clean()} ккал"
                    Toast.makeText(this@MainActivity, "Записано в Health Connect", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    status.text = "Ошибка: ${it.message}"
                    Toast.makeText(this@MainActivity, it.toString(), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun Double.clean(): String =
        if (this % 1.0 == 0.0) toLong().toString() else toString()
}
