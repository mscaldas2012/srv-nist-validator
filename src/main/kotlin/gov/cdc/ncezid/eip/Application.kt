package gov.cdc.ncezid.eip

import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        val ctx = Micronaut.build()
                .packages("gov.cdc.ncezid")
                .mainClass(Application.javaClass)
                .start()
    }
}