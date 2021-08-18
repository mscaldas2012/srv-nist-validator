package gov.cdc.ncezid.eip.validator.service

import gov.cdc.nist.validator.ProfileFetcher
import gov.cdc.nist.validator.ProfileManager
import gov.nist.validation.report.Entry
import gov.nist.validation.report.Report
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import javax.inject.Singleton

@Singleton
class ProfileManagerIgnoreZLR(profileFetcher: ProfileFetcher?, profile: String?) : ProfileManager(profileFetcher, profile) {
    companion object {
        val ZLR_SEGMENT = "ZLR"

        private const val VALID_MESSAGE_STATUS = "VALID_MESSAGE"
        private const val STRUCTURE_ERRORS_STATUS = "STRUCTURE_ERRORS"
        private const val CONTENT_ERRORS_STATUS = "CONTENT_ERRORS"
        private const val ERROR_CLASSIFICATION = "Error"
        private const val WARNING_CLASSIFICATION = "Warning"

        private const val VALUE_SET_ENTRIES = "value-set"
        private const val STRUCTURE_ENTRIES = "structure"
        private const val CONTENT_ENTRIES = "content"
        private const val error_count = "error-count"
        private const val warning_count = "warning-count"
    }

    override fun filter(report: Report): Map<String, Any>? {
        val erCount: MutableMap<String?, AtomicInteger?> = mutableMapOf()
        val warCount: MutableMap<String?, AtomicInteger?> = mutableMapOf()
        val valMap = report.entries
        val filteredMap: MutableMap<String, Any> = HashMap()
        val validationResultsMap: MutableMap<String, Any> = HashMap()
        valMap.forEach { (k: String, v: List<Entry>) ->
            erCount[k] = AtomicInteger()
            warCount[k] = AtomicInteger()
            val filteredContent: MutableList<Entry> = ArrayList()
            v.forEach(Consumer { value: Entry ->
                if (!value.path.startsWith(ZLR_SEGMENT)) {
                    if (value.classification == ERROR_CLASSIFICATION || value.classification == WARNING_CLASSIFICATION) {
                        filteredContent.add(value)
                        if (value.classification == WARNING_CLASSIFICATION) warCount[k]!!.getAndIncrement()
                        if (value.classification == ERROR_CLASSIFICATION) erCount[k]!!.getAndIncrement()
                    }
                }
            })
            filteredMap[k] = filteredContent
        }
        var status = VALID_MESSAGE_STATUS
        if (erCount[STRUCTURE_ENTRIES]!!.get() > 0) {
            status = STRUCTURE_ERRORS_STATUS
        } else if (erCount[CONTENT_ENTRIES]!!.get() > 0 || erCount[VALUE_SET_ENTRIES]!!.get() > 0) {
            status = CONTENT_ERRORS_STATUS
        }
        validationResultsMap[error_count] = erCount
        validationResultsMap[warning_count] = warCount
        validationResultsMap["entries"] = filteredMap
        validationResultsMap["status"] = status
        return validationResultsMap
    }
}