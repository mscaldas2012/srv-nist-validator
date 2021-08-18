package gov.cdc.ncezid.eip.validator.controller

import gov.cdc.ncezid.eip.About
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 *
 * @Created - 3/23/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
@Controller("nist-validator/v1/info")
class InfoController(val about: About) {
    companion object {
        private val logger = LoggerFactory.getLogger(InfoController::class.java.name)
    }

    @Get("/about")
    fun about(): About {
        return about
    }
    @Get("/ping")
    fun test(): String {
        logger.info ("AUDIT - Service Ping")
        return "Hello There. You pinged me at ${Date()}"
    }

}