package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro de un producto analizado (texto detectado + nivel de riesgo).
 * POO: atributos privados y métodos de acceso.
 */
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
) {

    fun getId(): Long = id

    fun getCreatedAtMs(): Long = createdAtMs

    fun getDetectedText(): String = detectedText

    fun getRiskLevel(): String = riskLevel

    companion object {
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
