package gov.cdc.ncezid.eip.validator.service

import gov.cdc.ncezid.cloud.storage.CloudStorage
import gov.cdc.nist.validator.InvalidFileException
import gov.cdc.nist.validator.NistReport
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.VALID_PROFILE_CONFIGS
import io.micronaut.context.annotation.Context
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.slf4j.ext.XLoggerFactory

@Context
class ProfileCacheManager(private val storageProxy: CloudStorage, private val cloudFetcher: CloudProfileFetcher) {
    companion object {
        private val logger = XLoggerFactory.getXLogger(ProfileCacheManager::class.java)
    }

    val profileCache: MutableMap<String, ProfileManager> = mutableMapOf()

    init {
        storageProxy.listFolders().let {
            logger.info("Initialize ProfileCacheManager with profiles: $it")
            it.forEach { p -> getResource(p) }
        }
    }

    private fun getResource(profile: String): ProfileManager? {
        logger.debug("AUDIT:: Retrieving Profile for {}", profile)
        if (!profileCache.containsKey(profile)) {
            try {
                val newValidator: ProfileManager = ProfileManagerIgnoreZLR(cloudFetcher, profile)
                profileCache[profile] = newValidator
            } catch (e: Exception) {
                logger.warn("Ignoring profile {} - System was unable to load it. Error: {}", profile, e)
            }
        }
        return profileCache[profile]
    }

    @Throws(Exception::class)
    fun validate(profile: String, message: String?): NistReport {
        val validator = profileCache[profile]
        return validator!!.validate(message)
    }

    @Throws(InvalidFileException::class)
    fun uploadResource(group: String, fileName: String, content: String) {
        uploadResourceNoCacheRefresh(group, VALID_PROFILE_CONFIGS.valueOf(fileName.toUpperCase()), content)
        profileCache.remove(group)
        getResource(group)
    }

    private fun uploadResourceNoCacheRefresh(group: String, fileName: VALID_PROFILE_CONFIGS, content: String) {
        logger.debug("AUDIT:: Uploading Filename {} - Resource Group {} - Content: {}", fileName, group, content)
        storageProxy.saveFile(group.toUpperCase() + "/" + fileName + ".xml", content)
    }

    @Throws(InvalidFileException::class)
    fun uploadZip(profileName: String, file: File): List<String?> {
        logger.debug("AUDIT:: Uploading Profile set (zip file) for {} - File: {}", profileName, file)
        val result: MutableList<String?> = mutableListOf()
        try {
            ZipFile(file).use { zf ->
                logger.debug(String.format("Inspecting contents of: %s\n {}", zf.name))
                val zipEntries = zf.entries()
                val iterator: Iterator<*> = zipEntries.asIterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next() as ZipEntry
                    try {
                        uploadResourceNoCacheRefresh(
                            profileName,
                            VALID_PROFILE_CONFIGS.valueOf(
                                entry.name.substring(0, entry.name.lastIndexOf(".")).toUpperCase()
                            ),
                            getContents(zf.getInputStream(entry))
                        )
                        result.add("OK:   File " + entry.name + " successfully loaded.")
                    } catch (e: InvalidFileException) {
                        logger.warn("File within ZIP is not a valid NIST configuration profile File: {}", entry.name)
                        logger.catching(e)
                        result.add("WARN: File " + entry.name + " ignored.")
                    }
                }
                profileCache.remove(profileName)
                getResource(profileName)
                return result
            }
        } catch (e: IOException) {
            logger.catching(e)
            throw InvalidFileException("IOException: Unable to read Zip file " + file.name)
        }
    }

    @Throws(IOException::class)
    private fun getContents(inputStream: InputStream): String {
        return inputStream.bufferedReader().use(BufferedReader::readText)
    }

    fun deleteProfile(profileName: String) {
        val configFiles = getConfigurationFiles(profileName)
        configFiles.forEach { storageProxy.deleteFile(it) }
        profileCache.remove(profileName)
    }

    @Throws(InvalidFileException::class)
    fun deleteConfigFile(profileName: String, configName: String) {
        try {
            val validFile = VALID_PROFILE_CONFIGS.valueOf(configName.toUpperCase())
            storageProxy.deleteFile("$profileName/$validFile.xml")
            profileCache.remove(profileName)
            getResource(profileName)
        } catch (e: IllegalArgumentException) {
            logger.catching(e)
            throw InvalidFileException("Configuration $configName is Invalid")
        }
    }

    fun getConfigurationFiles(profile: String): List<String> {
        return storageProxy.list(100, profile)
    }

    @Throws(InvalidFileException::class)
    fun getConfigFile(profileName: String, configName: String): String {
        return try {
            val validFile = VALID_PROFILE_CONFIGS.valueOf(configName.toUpperCase())
            storageProxy.getFileContent("$profileName/$validFile.xml")
        } catch (e: IllegalArgumentException) {
            logger.catching(e)
            throw InvalidFileException("Configuration $configName is Invalid")
        }
    }
}