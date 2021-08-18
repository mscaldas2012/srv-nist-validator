package gov.cdc.ncezid.eip.validator.service

import gov.cdc.ncezid.cloud.storage.CloudStorage
import gov.cdc.nist.validator.InvalidFileException
import gov.cdc.nist.validator.ProfileFetcher
import java.io.InputStream
import javax.inject.Singleton
import org.slf4j.ext.XLoggerFactory

@Singleton
class CloudProfileFetcher(private val storageProxy: CloudStorage): ProfileFetcher {
    private val logger = XLoggerFactory.getXLogger(CloudProfileFetcher::class.java)

    override fun getFile(file: String): String {
        TODO("Not yet implemented")
    }

    override fun getFileAsInputStream(file: String): InputStream {
        logger.entry(file)
        try {
            return storageProxy.getFileContentAsInputStream(file)
        } catch (e: Exception) {
            logger.error("Error: {}", e.toString())
            throw InvalidFileException("Unable to load profile $file")
        }
    }
}