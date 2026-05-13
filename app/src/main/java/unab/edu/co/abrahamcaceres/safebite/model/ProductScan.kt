package unab.edu.co.abrahamcaceres.safebite.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Registro de un producto analizado (texto detectado + nivel de riesgo).
 * POO: atributos privados y métodos de acceso.
 * @Parcelize permite pasar el objeto por SafeArgs entre Fragmentos.
 */
@Parcelize
@Entity(tableName = "product_scans")
class ProductScan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    @ColumnInfo(name = "created_at_ms")
    private var createdAtMs: Long = 0L,
    /** Texto completo reconocido en la etiqueta (OCR / ML Kit). */
    @ColumnInfo(name = "detected_text")
    private var detectedText: String = "",
    @ColumnInfo(name = "risk_level")
    private var riskLevel: String = ScanRisk.SAFE
) : Parcelable {

    fun getId(): Long = id

    fun getCreatedAtMs(): Long = createdAtMs

    fun getDetectedText(): String = detectedText

    fun getRiskLevel(): String = riskLevel

    /**
     * Deriva un nombre corto para la UI a partir del texto OCR.
     * Si no hay un título claro, devuelve un fallback amigable.
     */
    fun getProductName(): String {
        val firstLine = detectedText
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        if (firstLine.isBlank()) return DEFAULT_PRODUCT_NAME

        return if (firstLine.lowercase().startsWith("ingredientes")) {
            DEFAULT_PRODUCT_NAME
        } else {
            firstLine.take(MAX_PRODUCT_NAME_LENGTH)
        }
    }

    companion object {
        private const val DEFAULT_PRODUCT_NAME = "Producto escaneado"
        private const val MAX_PRODUCT_NAME_LENGTH = 60

        fun crear(
            textoDetectado: String,
            nivelRiesgo: String = ScanRisk.SAFE,
            fechaMs: Long = System.currentTimeMillis()
        ): ProductScan {
            return ProductScan(
                id = 0L,
                createdAtMs = fechaMs,
                detectedText = textoDetectado.trim(),
                riskLevel = nivelRiesgo
            )
        }
    }
}


