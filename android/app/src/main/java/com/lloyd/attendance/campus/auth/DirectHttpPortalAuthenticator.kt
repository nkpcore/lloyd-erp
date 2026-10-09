package com.lloyd.attendance.campus.auth

import android.net.Network
import com.lloyd.attendance.campus.CampusNetworkProfile
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Headless, network-bound direct HTTP authenticator.
 *
 * Core Engineering Principle:
 * Binds sockets and DNS exclusively to the Android Wi-Fi [Network] object.
 * This guarantees the captive login payload routes through the campus Wi-Fi AP
 * and never leaks over cellular / 5G data.
 */
class DirectHttpPortalAuthenticator {

    /**
     * In-memory cookie jar to maintain portal session state and cookies across redirects.
     */
    private class InMemoryCookieJar : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                // Remove expired or existing cookies with matching names
                for (newCookie in cookies) {
                    list.removeAll { it.name == newCookie.name }
                    list.add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            synchronized(list) {
                list.removeAll { it.expiresAt < now }
                return list.toList()
            }
        }

        fun getAllCookiesMap(): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (cookies in cookieStore.values) {
                for (c in cookies) {
                    map[c.name] = c.value
                }
            }
            return map
        }
    }

    /**
     * Builds an OkHttpClient bound strictly to the specified Android [Network].
     */
    fun createNetworkBoundClient(network: Network): OkHttpClient {
        return OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .dns(object : okhttp3.Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    return network.getAllByName(hostname).toList()
                }
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(InMemoryCookieJar())
            .build()
    }

    /**
     * Executes the direct headless HTTP authentication flow against the captive portal.
     */
    suspend fun authenticate(
        network: Network,
        portalUrl: String,
        username: String,
        password: String,
        profile: CampusNetworkProfile = CampusNetworkProfile.DEFAULT_PROFILE
    ): PortalAuthResult {
        val client = createNetworkBoundClient(network)
        val cookieJar = client.cookieJar as? InMemoryCookieJar

        try {
            // Step 1: Fetch the portal landing page over the Wi-Fi network
            val getRequest = Request.Builder()
                .url(portalUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val landingResponse = client.newCall(getRequest).execute()
            val landingHtml = landingResponse.body?.string().orEmpty()
            val finalLandingUrl = landingResponse.request.url.toString()
            landingResponse.close()

            // If landing page contains a CAPTCHA or JS challenge that cannot be parsed headlessly
            if (landingHtml.contains("recaptcha", ignoreCase = true) ||
                landingHtml.contains("cf-turnstile", ignoreCase = true) ||
                landingHtml.contains("hcaptcha", ignoreCase = true)
            ) {
                return PortalAuthResult.RequiresWebView(
                    reason = "Portal requires interactive CAPTCHA / bot verification",
                    portalUrl = finalLandingUrl
                )
            }

            // Step 2: Extract form action and hidden inputs
            val formActionUrl = resolveFormAction(finalLandingUrl, landingHtml, profile)
            val formFields = extractFormFields(landingHtml, profile, username, password)

            // Step 3: Dispatch POST login request
            val formBodyBuilder = FormBody.Builder()
            for ((key, value) in formFields) {
                formBodyBuilder.add(key, value)
            }

            val postRequest = Request.Builder()
                .url(formActionUrl)
                .post(formBodyBuilder.build())
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Referer", finalLandingUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val postResponse = client.newCall(postRequest).execute()
            val postResponseBody = postResponse.body?.string().orEmpty()
            val postStatusCode = postResponse.code
            val postFinalUrl = postResponse.request.url.toString()
            postResponse.close()

            // Step 4: Evaluate response for explicit invalid credentials
            val lowerBody = postResponseBody.lowercase()
            for (failureText in profile.failureIndicators) {
                if (lowerBody.contains(failureText.lowercase())) {
                    return PortalAuthResult.InvalidCredentials(
                        message = "Portal rejected credentials: $failureText"
                    )
                }
            }

            // Step 5: Verification Probe (HTTP 204 generation check)
            val probeSuccess = verifyInternetProbe(client)
            if (probeSuccess) {
                return PortalAuthResult.Success(
                    message = "Authenticated successfully via direct HTTP",
                    cookies = cookieJar?.getAllCookiesMap() ?: emptyMap(),
                    landingUrl = postFinalUrl
                )
            }

            // Step 6: If probe hasn't cleared immediately, inspect success indicators
            for (successText in profile.successIndicators) {
                if (lowerBody.contains(successText.lowercase())) {
                    return PortalAuthResult.Success(
                        message = "Portal responded with success indicator",
                        cookies = cookieJar?.getAllCookiesMap() ?: emptyMap(),
                        landingUrl = postFinalUrl
                    )
                }
            }

            // If HTTP redirect occurred to an external domain or status is 200 without error
            if (postStatusCode in 200..302 && !postFinalUrl.contains("login", ignoreCase = true)) {
                return PortalAuthResult.Success(
                    message = "Redirected away from portal login page",
                    cookies = cookieJar?.getAllCookiesMap() ?: emptyMap(),
                    landingUrl = postFinalUrl
                )
            }

            return PortalAuthResult.TransientFailure(
                message = "Portal responded with HTTP $postStatusCode but internet is not yet validated",
                statusCode = postStatusCode,
                retryAfterSeconds = 2
            )

        } catch (e: IOException) {
            return PortalAuthResult.TransientFailure(
                message = "Network error during portal login: ${e.message}",
                retryAfterSeconds = 3
            )
        } catch (e: Exception) {
            return PortalAuthResult.TransientFailure(
                message = "Unexpected authentication failure: ${e.message}",
                retryAfterSeconds = 5
            )
        }
    }

    /**
     * Resolves the form submission target URL by parsing <form action="...">
     * or falling back to the configured submit endpoint.
     */
    private fun resolveFormAction(baseUrlStr: String, html: String, profile: CampusNetworkProfile): String {
        val formPattern = Pattern.compile("<form[^>]*action=[\"']?([^\"' >]+)[\"']?[^>]*>", Pattern.CASE_INSENSITIVE)
        val matcher = formPattern.matcher(html)
        if (matcher.find()) {
            val rawAction = matcher.group(1)?.trim().orEmpty()
            if (rawAction.isNotEmpty()) {
                return try {
                    val base = URL(baseUrlStr)
                    URL(base, rawAction).toString()
                } catch (e: Exception) {
                    if (rawAction.startsWith("http")) rawAction else "$baseUrlStr/$rawAction"
                }
            }
        }

        // Fallback to configured submit endpoint
        return if (profile.submitEndpoint.startsWith("http")) {
            profile.submitEndpoint
        } else {
            try {
                val base = URL(baseUrlStr)
                URL(base, profile.submitEndpoint).toString()
            } catch (e: Exception) {
                "$baseUrlStr${profile.submitEndpoint}"
            }
        }
    }

    /**
     * Extracts hidden input fields, CSRF tokens, and injects username & password.
     */
    private fun extractFormFields(
        html: String,
        profile: CampusNetworkProfile,
        username: String,
        password: String
    ): Map<String, String> {
        val fields = mutableMapOf<String, String>()

        // 1. Extract hidden inputs: <input type="hidden" name="..." value="...">
        val inputPattern = Pattern.compile("<input[^>]*type=[\"']?hidden[\"']?[^>]*>", Pattern.CASE_INSENSITIVE)
        val namePattern = Pattern.compile("name=[\"']?([^\"' >]+)[\"']?", Pattern.CASE_INSENSITIVE)
        val valuePattern = Pattern.compile("value=[\"']?([^\"' >]*)[\"']?", Pattern.CASE_INSENSITIVE)

        val inputMatcher = inputPattern.matcher(html)
        while (inputMatcher.find()) {
            val inputTag = inputMatcher.group()
            val nameMatch = namePattern.matcher(inputTag)
            val valueMatch = valuePattern.matcher(inputTag)
            if (nameMatch.find()) {
                val name = nameMatch.group(1)
                val value = if (valueMatch.find()) valueMatch.group(1) else ""
                if (!name.isNullOrBlank()) {
                    fields[name] = value.orEmpty()
                }
            }
        }

        // 2. Identify username field name from HTML if different from profile
        var userFieldName = profile.usernameField
        if (!html.contains("name=[\"']?$userFieldName[\"']?".toRegex(RegexOption.IGNORE_CASE))) {
            val commonUserNames = listOf("username", "user", "user_id", "login", "auth_user", "username_box")
            for (candidate in commonUserNames) {
                if (html.contains("name=[\"']?$candidate[\"']?".toRegex(RegexOption.IGNORE_CASE))) {
                    userFieldName = candidate
                    break
                }
            }
        }

        // 3. Identify password field name from HTML
        var passFieldName = profile.passwordField
        if (!html.contains("name=[\"']?$passFieldName[\"']?".toRegex(RegexOption.IGNORE_CASE))) {
            val commonPassNames = listOf("password", "pass", "passwd", "auth_pass", "secret")
            for (candidate in commonPassNames) {
                if (html.contains("name=[\"']?$candidate[\"']?".toRegex(RegexOption.IGNORE_CASE))) {
                    passFieldName = candidate
                    break
                }
            }
        }

        // 4. Inject student credentials
        fields[userFieldName] = username
        fields[passFieldName] = password

        // 5. Add extra fields (e.g. mode=191 for Cyberoam/Sophos, etc.)
        for ((k, v) in profile.extraPostFields) {
            fields[k] = v
        }

        return fields
    }

    /**
     * Issues a standard connectivity probe to confirm firewall release.
     */
    fun verifyInternetProbe(client: OkHttpClient): Boolean {
        return try {
            val probeRequest = Request.Builder()
                .url("http://connectivitycheck.gstatic.com/generate_204")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()
            val response = client.newCall(probeRequest).execute()
            val code = response.code
            response.close()
            code == 204
        } catch (e: Exception) {
            false
        }
    }
}
