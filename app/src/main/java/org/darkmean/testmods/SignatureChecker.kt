package org.darkmean.testmods

import java.io.File
import java.security.cert.X509Certificate

// ─────────────────────────────────────────────────────────────────────
// MOD SIGNATURE VERIFICATION
// ─────────────────────────────────────────────────────────────────────
fun checkModSignature(modDir: File): ModSignatureInfo {
    val metaInf = File(modDir, "META-INF")
    val rsaFile = metaInf.listFiles()
        ?.firstOrNull { it.isFile && it.name.uppercase().endsWith(".RSA") }
        ?: return ModSignatureInfo(SignatureStatus.NOT_SIGNED)

    return runCatching {
        val cf = java.security.cert.CertificateFactory.getInstance("X.509")
        val certs: List<X509Certificate> = readCertsFromPkcs7(rsaFile, cf)

        if (certs.isEmpty()) return@runCatching ModSignatureInfo(SignatureStatus.NOT_SIGNED)

        val leafCert = certs.firstOrNull { it.basicConstraints == -1 } ?: certs.first()
        val now      = java.util.Date()
        val notAfter = leafCert.notAfter
        if (now.after(notAfter)) {
            ModSignatureInfo(SignatureStatus.EXPIRED)
        } else {
            val daysLeft = ((notAfter.time - now.time) / 86_400_000L).toInt()
            ModSignatureInfo(SignatureStatus.EXPIRES_SOON, daysLeft)
        }
    }.getOrElse {
        ModSignatureInfo(SignatureStatus.NOT_SIGNED)
    }
}

private fun readCertsFromPkcs7(
    rsaFile: File,
    cf: java.security.cert.CertificateFactory = java.security.cert.CertificateFactory.getInstance("X.509")
): List<X509Certificate> {
    val data = rsaFile.readBytes()
    runCatching {
        val certs = cf.generateCertificates(data.inputStream()).filterIsInstance<X509Certificate>()
        if (certs.isNotEmpty()) return certs
    }
    return runCatching { parseCertsFromSignedData(data, cf) }.getOrElse { emptyList() }
}

private fun parseCertsFromSignedData(
    data: ByteArray,
    cf: java.security.cert.CertificateFactory
): List<X509Certificate> {
    fun readLength(data: ByteArray, pos: Int): Pair<Int, Int> {
        val b = data[pos].toInt() and 0xFF
        return when {
            b < 0x80  -> Pair(b, 1)
            b == 0x81 -> Pair(data[pos + 1].toInt() and 0xFF, 2)
            b == 0x82 -> Pair(((data[pos+1].toInt() and 0xFF) shl 8) or (data[pos+2].toInt() and 0xFF), 3)
            b == 0x83 -> Pair(((data[pos+1].toInt() and 0xFF) shl 16) or ((data[pos+2].toInt() and 0xFF) shl 8) or (data[pos+3].toInt() and 0xFF), 4)
            else -> throw IllegalArgumentException("Unsupported length: 0x${b.toString(16)}")
        }
    }

    var i = 0
    while (i < data.size - 4) {
        if ((data[i].toInt() and 0xFF) == 0xA0) {
            runCatching {
                val (outerLen, outerLenBytes) = readLength(data, i + 1)
                val certsStart = i + 1 + outerLenBytes
                val certsEnd   = certsStart + outerLen
                if (certsEnd > data.size) { i++; return@runCatching }
                val result = mutableListOf<X509Certificate>()
                var j = certsStart
                while (j < certsEnd - 2) {
                    if ((data[j].toInt() and 0xFF) != 0x30) { j++; continue }
                    val (certLen, certLenBytes) = readLength(data, j + 1)
                    val certEnd = j + 1 + certLenBytes + certLen
                    if (certEnd > certsEnd) break
                    runCatching {
                        result.add(cf.generateCertificate(data.copyOfRange(j, certEnd).inputStream()) as X509Certificate)
                    }
                    j = certEnd
                }
                if (result.isNotEmpty()) return result
            }
        }
        i++
    }
    return emptyList()
}