package unab.edu.co.abrahamcaceres.safebite.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Ingrediente o sustancia que el usuario marca como alérgeno (lista negra).
 * POO: atributos privados y métodos de acceso (requisito académico).
 */
@Entity(
    tableName = "allergens",
    indices = [Index(value = ["normalized_name"], unique = true)]
)
class Allergen(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private var id: Long = 0L,
    /** Texto tal como lo ingresó el usuario (recortado). */
    @ColumnInfo(name = "display_name")
    private var displayName: String = "",
    /** Clave única para evitar duplicados ignorando mayúsculas/minúsculas. */
    @ColumnInfo(name = "normalized_name")
    private var normalizedName: String = ""
) {

    fun getId(): Long = id

    fun getDisplayName(): String = displayName

    fun getNormalizedName(): String = normalizedName

    companion object {
        /** Crea una entidad lista para insertar a partir del texto del formulario. */
        fun desdeTextoPlano(texto: String): Allergen {
            val nombre = texto.trim()
            return Allergen(
                id = 0L,
                displayName = nombre,
                normalizedName = nombre.lowercase(Locale.getDefault())
            )
        }
    }
}
