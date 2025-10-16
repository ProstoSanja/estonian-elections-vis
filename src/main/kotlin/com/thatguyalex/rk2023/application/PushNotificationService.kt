package com.thatguyalex.rk2023.application

import com.interaso.webpush.VapidKeys
import com.interaso.webpush.WebPush
import com.interaso.webpush.WebPushService
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.classes.PushMessage
import com.thatguyalex.rk2023.infrastructure.classes.PushSendResult
import com.thatguyalex.rk2023.infrastructure.classes.PushSubscription
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.Base64

@Service
class PushNotificationService(
    private val subscriptionRepo: PushSubscriptionRepo,
    @Value("\${push.vapid.publicKey:}") private val vapidPublicKey: String,
    @Value("\${push.vapid.privateKey:}") private val vapidPrivateKey: String,
    @Value("\${push.vapid.subject:mailto:your-email@example.com}") private val vapidSubject: String
) {
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    // Security setup

    private val secp256r1parameterSpec: ECParameterSpec = AlgorithmParameters.getInstance("EC").run {
        init(ECGenParameterSpec("secp256r1"))
        getParameterSpec(ECParameterSpec::class.java)
    }

    private fun generatePublicKeyFromUncompressedBytes(bytes: ByteArray): ECPublicKey {
        val ecPoint = ECPoint(
            BigInteger(1, bytes.copyOfRange(1, 33)),
            BigInteger(1, bytes.copyOfRange(33, 65)),
        )

        return KeyFactory.getInstance("EC").run {
            generatePublic(ECPublicKeySpec(ecPoint, secp256r1parameterSpec)) as ECPublicKey
        }
    }

    private fun generatePrivateKeyFromUncompressedBytes(bytes: ByteArray): ECPrivateKey {
        return KeyFactory.getInstance("EC").run {
            generatePrivate(ECPrivateKeySpec(BigInteger(1, bytes), secp256r1parameterSpec)) as ECPrivateKey
        }
    }

    private val webPushService: WebPushService? = run {
        if (vapidPublicKey.isEmpty() || vapidPrivateKey.isEmpty()) {
            logger.warn("VAPID keys not configured. Push notifications will not be sent.")
            return@run null
        }
        return@run try {
            WebPushService(
                subject = vapidSubject,
                vapidKeys = VapidKeys(
                    generatePublicKeyFromUncompressedBytes(Base64.getUrlDecoder().decode(vapidPublicKey)),
                    generatePrivateKeyFromUncompressedBytes(Base64.getUrlDecoder().decode(vapidPrivateKey)),
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to initialize WebPushService", e)
            null
        }
    }

    // notification service

    fun sendNotificationTo(subscriptions: List<PushSubscription>, message: PushMessage): Int {
        return subscriptions.map { subscription ->
            sendPushNotification(subscription, message)
        }.count { it == PushSendResult.SUCCESS }
    }

    private fun sendPushNotification(
        subscription: PushSubscription,
        message: PushMessage
    ): PushSendResult {
        if (webPushService == null) {
            logger.warn("WebPushService not initialized. Skipping push notification.")
            return PushSendResult.SERVICE_DISABLED
        }
        return try {
            webPushService.send(
                payload = """{"title":"${message.title}","body":"${message.body}","url":"${message.url}"}""",
                endpoint = subscription.endpoint,
                p256dh = subscription.keys.p256dh,
                auth = subscription.keys.auth
            ).let {
                when (it) {
                    WebPush.SubscriptionState.ACTIVE -> PushSendResult.SUCCESS
                    WebPush.SubscriptionState.EXPIRED -> PushSendResult.EXPIRED.also {
                        subscriptionRepo.remove(subscription.endpoint)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to send push notification to ${subscription.endpoint}", e)
            return PushSendResult.FAILED
        }
    }
}
