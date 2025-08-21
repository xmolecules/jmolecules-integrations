package org.jmolecules.jackson3

import kotlin.jvm.JvmStatic
import org.jmolecules.ddd.types.Identifier
import java.util.UUID

data class SampleIdentifier(val id : UUID) : Identifier {

    companion object {

        // @JvmStatic
        fun of(id: UUID): SampleIdentifier {
            return SampleIdentifier(id)
        }
    }
}
