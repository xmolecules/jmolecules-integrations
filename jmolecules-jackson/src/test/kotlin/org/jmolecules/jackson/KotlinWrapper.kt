package org.jmolecules.jackson

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class KotlinWrapper(
    val id: SampleIdentifier,
    val anotherId : AnotherSampleIdentifier) {}
