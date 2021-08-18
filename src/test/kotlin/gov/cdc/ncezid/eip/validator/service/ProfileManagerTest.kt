package gov.cdc.ncezid.eip.validator.service

import gov.cdc.nist.validator.VALID_PROFILE_CONFIGS
import io.micronaut.test.extensions.junit5.annotation.MicronautTest

import org.junit.jupiter.api.Test

import javax.inject.Inject


@MicronautTest
class ProfileManagerTest {
    companion object {
        private val TEST_PROFILE  = "TEST_MN"
    }
    @Inject
    lateinit var profiles: ProfileCacheManager

    @Test
    fun testGetProfiles() {
        val list: Set<String> = profiles.profileCache.keys
        list.forEach { v: String -> println("v = $v") }
    }

    @Test
    fun testGetProfilesFiles() {
        val list = profiles.getConfigurationFiles(TEST_PROFILE)
        list.forEach { println(it)}
        assert(list.contains("$TEST_PROFILE/${VALID_PROFILE_CONFIGS.PROFILE}.xml"))
        assert(list.contains("$TEST_PROFILE/${VALID_PROFILE_CONFIGS.CONSTRAINTS}.xml"))
        assert(list.contains("$TEST_PROFILE/${VALID_PROFILE_CONFIGS.VALUESETS}.xml"))
    }
    @Test
    fun tetGetProfileContent() {
        val profile = profiles.getConfigFile(TEST_PROFILE, "profile")
        assert(profile.length > 0 && profile.startsWith("<?xml version=\"1.0\"?>\n" +
                "<ConformanceProfile"))
    }

}