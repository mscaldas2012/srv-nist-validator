package gov.cdc.ncezid.eip.validator.controller

import gov.cdc.ncezid.eip.validator.model.ErrorResponse
import gov.cdc.ncezid.eip.validator.service.ProfileCacheManager
import gov.cdc.ncezid.rest.security.S2SAuth
import gov.cdc.ncezid.rest.security.ServiceNotAuthorizedException
import gov.cdc.nist.validator.InvalidFileException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import org.slf4j.ext.XLoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.util.*

@Controller("nist-validator/v1/profiles")
class ProfileController(val profileManager: ProfileCacheManager, val s2sAuth: S2SAuth) {
    private val logger = XLoggerFactory.getXLogger(ProfileController::class.java)

    @Get("/")
    fun getListOfProfiles(): List<String> {
        logger.debug("AUDIT:: Listing all profiles")
        return profileManager.profileCache.keys.toList()
    }


    @Get("/{profileName:.+}")
    fun getProfileConfig(@PathVariable profileName: String): List<String> {
        logger.entry(profileName)
        logger.debug("AUDIT:: Getting list of configuration files for profile {}", profileName)
        return profileManager.getConfigurationFiles(profileName)
    }

    @Delete("{profileName:.+}")
    @Throws(ServiceNotAuthorizedException::class)
    fun deleteProfile(@Header( "s2s-token") token: String, @PathVariable profileName: String) {
        logger.debug("AUDIT:: Deleting profile: {} - s2s-token: {}" , profileName, token)
        /**Throws ServiceNotAuthorizedException if fails. **/
        s2sAuth.checkS2SCredentials(token)
        profileManager.deleteProfile(profileName)
    }

    @Get("{profileName:.+}/{configFile}")
    @Throws(InvalidFileException::class)
    fun getConfiguration(@PathVariable profileName: String, @PathVariable configFile: String): String {
        logger.debug("AUDIT:: Reading configuration {} - profile {}", configFile, profileName)
        return profileManager.getConfigFile(profileName, configFile)
    }

    @Delete("{profileName:.+}/{configFile}")
    @Throws(InvalidFileException::class, ServiceNotAuthorizedException::class)
    fun deleteConfiguration(@Header("s2s-token") token: String, @PathVariable profileName: String, @PathVariable configFile: String) {
        logger.debug("AUDIT::Deleting configuration {}, profile {}", configFile, profileName)
        /**Throws ServiceNotAuthorizedException if fails.**/
        s2sAuth.checkS2SCredentials(token)
        profileManager.deleteConfigFile(profileName, configFile)
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/{profileName:.+}")
    @Throws(InvalidFileException::class, ServiceNotAuthorizedException::class)
    fun saveProfile(@Header("s2s-token") token: String, @PathVariable profileName: String, @Body file: CompletedFileUpload?, req: HttpRequest<*>): HttpResponse<*> {
        logger.debug("AUDIT:: Uploading Profile SET  for {} - s2s-token: {}", profileName, token)
        /**Throws ServiceNotAuthorizedException if fails.**/
        s2sAuth.checkS2SCredentials(token)
        if (file == null) {
            val error = ErrorResponse("MISSING_PARAM", "Missing file input parameter", 400, req.path)
            return HttpResponse.badRequest(error)
        }
        return try {
            /**It's a zipped file with all files to be loaded...**/
            if (!file.filename.endsWith(".zip")) {
                val error = ErrorResponse("Invalid File", "Invalid File Uploaded. Expecting ZIP file with configuration xml profiles", 400, req.path)
                return HttpResponse.badRequest(error)
            }
            val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp")
            //Save uploaded file to temp file...
            val buffer = ByteArray(file.inputStream.available())
            file.inputStream.read(buffer)
            val outStream: OutputStream = FileOutputStream(tempFile)
            outStream.write(buffer)
            val result = profileManager.uploadZip(profileName, tempFile)
            tempFile.delete()
            HttpResponse.ok(result)
        } catch (e: IOException) {
            logger.error("Unable to upload file {} - Exception: {}", file.filename, e)
            val error = ErrorResponse("BAD_REQUEST", e.message!!, 400, req.path)
            HttpResponse.badRequest(error)
        }
    }

    @Consumes(MediaType.APPLICATION_XML)
    @Post("/{profileName:.+}/{configFile}")
    @Throws(InvalidFileException::class, ServiceNotAuthorizedException::class)
    fun updateConfigFile(@Header( "s2s-token") token: String, @PathVariable profileName: String, @PathVariable configFile: String, @Body content: String): String {
        logger.debug("AUDIT:: Uploading Configuration file for profile {} - s2s-token: {}", profileName, token)
        /**Throws ServiceNotAuthorizedException if fails.**/
        s2sAuth.checkS2SCredentials(token)
        profileManager.uploadResource(profileName, configFile, content)
        return "upload success"
    }

    @Consumes(MediaType.TEXT_PLAIN)
    @Post("/{profileName:.+}/\$validate")
    fun validate(@PathVariable profileName: String, @Body hl7Message: String, request: HttpRequest<*>): MutableHttpResponse<*> {
        logger.debug("AUDIT:: Validating message for profile {}", profileName)
        return try {
            val report = profileManager.validate(profileName, hl7Message)
            HttpResponse.ok(report)
        } catch (e: Exception) {
            logger.error("ERROR validating profile for group: {} - Exception: {}", profileName, e)
            val error = ErrorResponse("INVALID PROFILE", "Profile for $profileName does not Exists. Please use a valid Profile for validation", 400, request.path)
            HttpResponse.badRequest(error)
        }
    }

    @Error(InvalidFileException::class)
    protected fun handleInvalidFileError(e: Exception, request: HttpRequest<*>): ErrorResponse {
        logger.catching(e)
        return ErrorResponse("BAD Request", e.toString()!!, 400, request.path)
    }

    @Error(ServiceNotAuthorizedException::class)
    protected fun handleUnauthorizedError(e: ServiceNotAuthorizedException?): MutableHttpResponse<Any> {
        return HttpResponse.unauthorized()
    }
}