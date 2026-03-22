package com.thatguyalex.monitoring.infrastructure.rest

import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

@Service
class AriregisterClient(
    private val props: AriregisterProperties,
    restTemplateBuilder: RestTemplateBuilder,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = restTemplateBuilder.build()

    fun fetchDetails(registrikood: String): Document {
        val body = buildSoapRequest(registrikood)
        val headers = HttpHeaders().apply {
            contentType = MediaType.TEXT_XML
        }
        log.info("Fetching ariregister details for registrikood={}", registrikood)
        val response = restTemplate.postForEntity(
            props.url,
            HttpEntity(body, headers),
            String::class.java,
        )
        if (!response.statusCode.is2xxSuccessful || response.body.isNullOrBlank()) {
            throw IllegalStateException(
                "Ariregister API returned status=${response.statusCode} for registrikood=$registrikood"
            )
        }
        val doc = parseXml(response.body!!)
        val count = textContent(doc, "leitud_ettevotjate_arv")?.toIntOrNull() ?: 0
        if (count == 0) {
            throw IllegalStateException("No companies found for registrikood=$registrikood")
        }
        return doc
    }

    private fun buildSoapRequest(registrikood: String): String = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:prod="http://arireg.x-road.eu/producer/">
            <soapenv:Body>
                <prod:detailandmed_v2>
                    <prod:keha>
                        <prod:ariregister_kasutajanimi>${props.username}</prod:ariregister_kasutajanimi>
                        <prod:ariregister_parool>${props.password}</prod:ariregister_parool>
                        <prod:ariregistri_kood>$registrikood</prod:ariregistri_kood>
                        <prod:yandmed>1</prod:yandmed>
                        <prod:iandmed>1</prod:iandmed>
                        <prod:kandmed>0</prod:kandmed>
                        <prod:dandmed>0</prod:dandmed>
                        <prod:maarused>0</prod:maarused>
                    </prod:keha>
                </prod:detailandmed_v2>
            </soapenv:Body>
        </soapenv:Envelope>
    """.trimIndent()

    companion object {
        const val NS = "http://arireg.x-road.eu/producer/"

        fun parseXml(xml: String): Document {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            return factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        }

        fun textContent(parent: Any, localName: String): String? {
            val elements = when (parent) {
                is Document -> parent.getElementsByTagNameNS(NS, localName)
                is Element -> parent.getElementsByTagNameNS(NS, localName)
                else -> return null
            }
            if (elements.length == 0) return null
            val text = elements.item(0).textContent?.trim()
            return if (text.isNullOrEmpty()) null else text
        }

        fun elements(parent: Element, localName: String): List<Element> {
            val nodeList: NodeList = parent.getElementsByTagNameNS(NS, localName)
            return (0 until nodeList.length)
                .map { nodeList.item(it) }
                .filterIsInstance<Element>()
        }

        fun directChildElements(parent: Element, localName: String): List<Element> {
            val result = mutableListOf<Element>()
            var child = parent.firstChild
            while (child != null) {
                if (child is Element && child.localName == localName && child.namespaceURI == NS) {
                    result.add(child)
                }
                child = child.nextSibling
            }
            return result
        }
    }
}
