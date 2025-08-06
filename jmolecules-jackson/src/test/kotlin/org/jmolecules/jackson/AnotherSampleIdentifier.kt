package org.jmolecules.jackson

import kotlin.jvm.JvmStatic
import org.jmolecules.ddd.types.Identifier
import java.util.UUID

data class AnotherSampleIdentifier(val id : UUID) : Identifier {}
