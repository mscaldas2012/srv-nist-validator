package gov.cdc.ncezid.eip.validator.model

data class ErrorResponse (val code: String, val description: String,
                          val status: Int, val path: String) {

    var exeption: String? = null

    constructor (code: String, description: String, status: Int, path: String, exception: String) :
            this(code,description, status, path) {
        exeption = exception
    }
}
