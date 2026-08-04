package com.memoriabox.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupArchive {
    private val magic = byteArrayOf(0x4d, 0x42, 0x4f, 0x58, 0x42, 0x4b, 0x50, 0x32)
    private const val formatVersion = 2
    private const val encryptedFlag = 1
    private const val iterations = 210_000

    fun inspect(input: InputStream): Header {
        val actual = ByteArray(magic.size)
        if (input.read(actual) != actual.size || !actual.contentEquals(magic)) return Header(encrypted = true, legacy = true)
        val data = DataInputStream(input)
        val version = data.readInt()
        require(version in 1..formatVersion) { "备份格式版本 $version 暂不受支持" }
        return Header(encrypted = data.readUnsignedByte() and encryptedFlag != 0)
    }

    fun openOutput(output: OutputStream, password: String): OutputStream {
        val data = DataOutputStream(output)
        data.write(magic)
        data.writeInt(formatVersion)
        val encrypted = password.isNotEmpty()
        data.writeByte(if (encrypted) encryptedFlag else 0)
        if (!encrypted) {
            data.writeByte(0)
            data.writeByte(0)
            data.flush()
            return output
        }

        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        data.writeByte(salt.size)
        data.writeByte(iv.size)
        data.write(salt)
        data.write(iv)
        data.flush()
        return CipherOutputStream(output, cipher(Cipher.ENCRYPT_MODE, password, salt, iv))
    }

    fun openInput(input: InputStream, password: String): InputStream {
        val actual = ByteArray(magic.size)
        DataInputStream(input).readFully(actual)
        require(actual.contentEquals(magic)) { "备份文件头无效" }
        val data = DataInputStream(input)
        val version = data.readInt()
        require(version in 1..formatVersion) { "备份格式版本 $version 暂不受支持" }
        val encrypted = data.readUnsignedByte() and encryptedFlag != 0
        val saltLength = data.readUnsignedByte()
        val ivLength = data.readUnsignedByte()
        if (!encrypted) {
            require(saltLength == 0 && ivLength == 0) { "明文备份文件头无效" }
            return input
        }
        require(password.isNotEmpty()) { "此备份已加密，请输入备份密码" }
        require(saltLength in 16..32 && ivLength == 12) { "加密备份参数无效" }
        val salt = ByteArray(saltLength).also(data::readFully)
        val iv = ByteArray(ivLength).also(data::readFully)
        return CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, password, salt, iv))
    }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count: Int
        while (input.read(buffer).also { count = it } >= 0) {
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun cipher(mode: Int, password: String, salt: ByteArray, iv: ByteArray): Cipher {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        }
    }
}

data class Header(val encrypted: Boolean, val legacy: Boolean = false)
