package com.rajpawardotin.kosh.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

object Bip39Utils {

    @Volatile
    private var cachedWordList: List<String>? = null

    fun loadWordList(context: Context): List<String> {
        return cachedWordList ?: synchronized(this) {
            cachedWordList ?: context.assets.open("bip39_english.txt").bufferedReader().useLines { lines ->
                val list = lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
                if (list.size != 2048) {
                    throw IllegalStateException("BIP39 English wordlist must contain exactly 2048 words, found ${list.size}")
                }
                list
            }.also { cachedWordList = it }
        }
    }

    fun generateMnemonic(context: Context): Pair<String, ByteArray> {
        val wordList = loadWordList(context)
        val entropy = ByteArray(16) // 128 bits
        SecureRandom().nextBytes(entropy)

        // Calculate SHA-256 hash of the entropy
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        val firstHashByte = hash[0].toInt() and 0xFF

        // Convert entropy to bits
        val bits = StringBuilder()
        for (b in entropy) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bits.append(if ((byteVal and (1 shl i)) != 0) '1' else '0')
            }
        }

        // Add 4 bits of checksum from the first hash byte (first 4 MSBs)
        for (i in 7 downTo 4) {
            bits.append(if ((firstHashByte and (1 shl i)) != 0) '1' else '0')
        }

        val bitString = bits.toString()
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val chunk = bitString.substring(i * 11, (i + 1) * 11)
            val index = chunk.toInt(2)
            words.add(wordList[index])
        }

        return Pair(words.joinToString(" "), entropy)
    }

    fun mnemonicToEntropy(mnemonic: String, context: Context): ByteArray {
        val wordList = loadWordList(context)
        val words = mnemonic.trim().split("\\s+".toRegex())
        if (words.size != 12) {
            throw IllegalArgumentException("Mnemonic must consist of exactly 12 words")
        }

        val bits = StringBuilder()
        for (word in words) {
            val index = wordList.indexOf(word.lowercase())
            if (index == -1) {
                throw IllegalArgumentException("Invalid BIP39 word in mnemonic: $word")
            }
            val binary = index.toString(2).padStart(11, '0')
            bits.append(binary)
        }

        val bitString = bits.toString()
        val entropyBits = bitString.substring(0, 128)
        val checksumBits = bitString.substring(128, 132)

        val entropy = ByteArray(16)
        for (i in 0 until 16) {
            val byteStr = entropyBits.substring(i * 8, (i + 1) * 8)
            entropy[i] = byteStr.toInt(2).toByte()
        }

        // Verify checksum
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        val firstHashByte = hash[0].toInt() and 0xFF
        val calculatedChecksum = StringBuilder()
        for (i in 7 downTo 4) {
            calculatedChecksum.append(if ((firstHashByte and (1 shl i)) != 0) '1' else '0')
        }

        if (calculatedChecksum.toString() != checksumBits) {
            throw IllegalArgumentException("Mnemonic checksum validation failed")
        }

        return entropy
    }
}
