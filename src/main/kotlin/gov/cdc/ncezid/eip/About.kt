package gov.cdc.ncezid.eip

import io.micronaut.context.annotation.ConfigurationProperties


@ConfigurationProperties("about")
class About {
    lateinit var summary: String
    lateinit var contacts: List<ContactInfo>
    lateinit var versions: List<String>
    lateinit var docs: String
}

class ContactInfo( val name: String,  val email: String,  val role: String)