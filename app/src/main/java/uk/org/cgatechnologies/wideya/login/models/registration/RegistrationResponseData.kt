package uk.org.cgatechnologies.wideya.login.models.registration

data class RegistrationResponseData(
    val access_token: String,
    val username: String,
    val user_id: String,
    val name: String,
    val scope_cache_id: String,
    val scope_schools: String,
    val scope_hash: String,
    val passphrase: String,
)
