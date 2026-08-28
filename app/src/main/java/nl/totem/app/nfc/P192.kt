package nl.totem.app.nfc

import java.math.BigInteger
import java.security.MessageDigest

/**
 * ECDSA-verificatie op NIST P-192 (secp192r1), met de hand uitgerekend.
 *
 * Waarom niet gewoon `java.security.Signature`: geen enkele cryptoprovider op
 * Android kent secp192r1 nog. Android leunt op BoringSSL, en daar zijn de oude
 * korte curves uit gesloopt. `KeyFactory.getInstance("EC")` weigert de sleutel
 * dan al bij het inlezen, ongeacht welke provider je kiest.
 *
 * Verifieren is puur rekenwerk met grote getallen, dus dat kan ook zonder
 * bibliotheek. Alleen verifieren -- ondertekenen gebeurt bij de productie, en
 * dat vraagt een geheime sleutel en een veilige toevalsgenerator. Die staan
 * hier bewust niet in.
 *
 * De curveparameters komen uit FIPS 186-4, appendix D.1.2.1.
 */
object P192 {

    /** Priemgetal van het veld: 2^192 - 2^64 - 1 */
    private val P = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFF", 16)

    /** Curve: y^2 = x^3 + ax + b, met a = p - 3 */
    private val A = P - BigInteger.valueOf(3)

    /** Orde van het basispunt */
    private val N = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFF99DEF836146BC9B1B4D22831", 16)

    /** Basispunt G */
    private val GX = BigInteger("188DA80EB03090F67CBF20EB43A18800F4FF0AFD82FF1012", 16)
    private val GY = BigInteger("07192B95FFC8DA78631011ED6B24CDD573F977A11E794811", 16)

    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)

    /** Byte-lengte van r, s en de coordinaten. */
    const val COORDINATE_BYTES = 24

    /** Een punt op de curve. `null` staat voor het punt op oneindig. */
    private data class Point(val x: BigInteger, val y: BigInteger)

    private fun add(p1: Point?, p2: Point?): Point? {
        if (p1 == null) return p2
        if (p2 == null) return p1

        // Tegengestelde punten heffen elkaar op.
        if (p1.x == p2.x && (p1.y + p2.y).mod(P) == BigInteger.ZERO) return null

        val slope: BigInteger = if (p1 == p2) {
            val numerator = (THREE * p1.x * p1.x + A).mod(P)
            val denominator = (TWO * p1.y).mod(P).modInverse(P)
            (numerator * denominator).mod(P)
        } else {
            val numerator = (p2.y - p1.y).mod(P)
            val denominator = (p2.x - p1.x).mod(P).modInverse(P)
            (numerator * denominator).mod(P)
        }

        val x3 = (slope * slope - p1.x - p2.x).mod(P)
        val y3 = (slope * (p1.x - x3) - p1.y).mod(P)
        return Point(x3, y3)
    }

    /** Double-and-add. */
    private fun multiply(k: BigInteger, point: Point): Point? {
        var result: Point? = null
        var addend: Point? = point
        for (i in 0 until k.bitLength()) {
            if (k.testBit(i)) result = add(result, addend)
            addend = add(addend, addend)
        }
        return result
    }

    /** Controleert of het punt daadwerkelijk op de curve ligt. */
    fun isOnCurve(qx: BigInteger, qy: BigInteger): Boolean {
        val left = (qy * qy).mod(P)
        val b = BigInteger("64210519E59C80E70FA7E9AB72243049FEB8DEECC146B9B1", 16)
        val right = (qx * qx * qx + A * qx + b).mod(P)
        return left == right
    }

    /**
     * Controleert een ECDSA-handtekening.
     *
     * @param message de ondertekende bytes
     * @param signature rauw r||s, 48 bytes
     * @param qx x-coordinaat van de publieke sleutel
     * @param qy y-coordinaat van de publieke sleutel
     */
    fun verify(
        message: ByteArray,
        signature: ByteArray,
        qx: BigInteger,
        qy: BigInteger,
    ): Boolean {
        if (signature.size != COORDINATE_BYTES * 2) return false

        val r = BigInteger(1, signature.copyOfRange(0, COORDINATE_BYTES))
        val s = BigInteger(1, signature.copyOfRange(COORDINATE_BYTES, signature.size))
        if (r < BigInteger.ONE || r >= N) return false
        if (s < BigInteger.ONE || s >= N) return false

        // SHA-256 levert 32 bytes; ECDSA gebruikt de linker 192 bits daarvan,
        // want langer dan de orde van de curve heeft geen zin.
        val digest = MessageDigest.getInstance("SHA-256").digest(message)
        val e = BigInteger(1, digest.copyOfRange(0, COORDINATE_BYTES))

        val w = s.modInverse(N)
        val u1 = (e * w).mod(N)
        val u2 = (r * w).mod(N)

        val point = add(multiply(u1, Point(GX, GY)), multiply(u2, Point(qx, qy)))
            ?: return false

        return point.x.mod(N) == r
    }
}
