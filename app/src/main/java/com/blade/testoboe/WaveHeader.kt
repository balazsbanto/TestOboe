package com.blade.testoboe

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * This class represents the header of a WAVE format audio file, which usually
 * have a .wav suffix.  The following integer valued fields are contained:
 *
 *  *  format - usually PCM, ALAW or ULAW.
 *  *  numChannels - 1 for mono, 2 for stereo.
 *  *  sampleRate - usually 8000, 11025, 16000, 22050, or 44100 hz.
 *  *  bitsPerSample - usually 16 for PCM, 8 for ALAW, or 8 for ULAW.
 *  *  numBytes - size of audio data after this header, in bytes.
 *
 *
 */
class WaveHeader {
    /**
     * Get the format field.
     * @return format field,
     * one of [.FORMAT_PCM], [.FORMAT_ULAW], or [.FORMAT_ALAW].
     */
    var format: Short = 0
        private set

    /**
     * Get the number of channels.
     * @return number of channels, 1 for mono, 2 for stereo.
     */
    var numChannels: Short = 0
        private set

    /**
     * Get the sample rate.
     * @return sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     */
    var sampleRate = 0
        private set

    /**
     * Get the number of bits per sample.
     * @return number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     */
    var bitsPerSample: Short = 0
        private set

    /**
     * Get the size of audio data after this header, in bytes.
     * @return size of audio data after this header, in bytes.
     */
    var numBytes = 0
        private set

    /**
     * Construct a WaveHeader, with all fields defaulting to zero.
     */
    constructor() {}

    /**
     * Construct a WaveHeader, with fields initialized.
     * @param format format of audio data,
     * one of [.FORMAT_PCM], [.FORMAT_ULAW], or [.FORMAT_ALAW].
     * @param numChannels 1 for mono, 2 for stereo.
     * @param sampleRate typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @param bitsPerSample usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     * @param numBytes size of audio data after this header, in bytes.
     */
    constructor(
        format: Short,
        numChannels: Short,
        sampleRate: Int,
        bitsPerSample: Short,
        numBytes: Int
    ) {
        this.format = format
        this.sampleRate = sampleRate
        this.numChannels = numChannels
        this.bitsPerSample = bitsPerSample
        this.numBytes = numBytes
    }

    /**
     * Set the format field.
     * @param format
     * one of [.FORMAT_PCM], [.FORMAT_ULAW], or [.FORMAT_ALAW].
     * @return reference to this WaveHeader instance.
     */
    fun setFormat(format: Short): WaveHeader {
        this.format = format
        return this
    }

    /**
     * Set the number of channels.
     * @param numChannels 1 for mono, 2 for stereo.
     * @return reference to this WaveHeader instance.
     */
    fun setNumChannels(numChannels: Short): WaveHeader {
        this.numChannels = numChannels
        return this
    }

    /**
     * Set the sample rate.
     * @param sampleRate sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @return reference to this WaveHeader instance.
     */
    fun setSampleRate(sampleRate: Int): WaveHeader {
        this.sampleRate = sampleRate
        return this
    }

    /**
     * Set the number of bits per sample.
     * @param bitsPerSample number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     * @return reference to this WaveHeader instance.
     */
    fun setBitsPerSample(bitsPerSample: Short): WaveHeader {
        this.bitsPerSample = bitsPerSample
        return this
    }

    /**
     * Set the size of audio data after this header, in bytes.
     * @param numBytes size of audio data after this header, in bytes.
     * @return reference to this WaveHeader instance.
     */
    fun setNumBytes(numBytes: Int): WaveHeader {
        this.numBytes = numBytes
        return this
    }

    /**
     * Read and initialize a WaveHeader.
     * @param in [InputStream] to read from.
     * @return number of bytes consumed.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun read(`in`: InputStream): Int {
        /* RIFF header */
        readId(`in`, "RIFF")
        val numBytes = readInt(`in`) - 36
        readId(`in`, "WAVE")

        /* fmt chunk */readId(`in`, "fmt ")
        if (16 != readInt(`in`)) throw IOException("fmt chunk length not 16")
        format = readShort(`in`)
        numChannels = readShort(`in`)
        sampleRate = readInt(`in`)
        val byteRate = readInt(`in`)
        val blockAlign = readShort(`in`)
        bitsPerSample = readShort(`in`)
        if (byteRate != numChannels * sampleRate * bitsPerSample / 8) {
            throw IOException("fmt.ByteRate field inconsistent")
        }
        if (blockAlign.toInt() != numChannels * bitsPerSample / 8) {
            throw IOException("fmt.BlockAlign field inconsistent")
        }

        /* data chunk */readId(`in`, "data")
        this.numBytes = readInt(`in`)
        return HEADER_LENGTH
    }

    /**
     * Write a WAVE file header.
     * @param out [OutputStream] to receive the header.
     * @return number of bytes written.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(out: OutputStream): Int {
        /* RIFF header */
        writeId(out, "RIFF")
        writeInt(out, 36 + numBytes)
        writeId(out, "WAVE")

        /* fmt chunk */writeId(out, "fmt ")
        writeInt(out, 16)
        writeShort(out, format)
        writeShort(out, numChannels)
        writeInt(out, sampleRate)
        writeInt(out, numChannels * sampleRate * bitsPerSample / 8)
        writeShort(out, (numChannels * bitsPerSample / 8).toShort())
        writeShort(out, bitsPerSample)

        /* data chunk */writeId(out, "data")
        writeInt(out, numBytes)
        return HEADER_LENGTH
    }

    override fun toString(): String {
        return String.format(
            "WaveHeader format=%d numChannels=%d sampleRate=%d bitsPerSample=%d numBytes=%d",
            format, numChannels, sampleRate, bitsPerSample, numBytes
        )
    }

    companion object {
        // follows WAVE format in http://ccrma.stanford.edu/courses/422/projects/WaveFormat
        private const val TAG = "WaveHeader"
        private const val HEADER_LENGTH = 44

        /** Indicates PCM format.  */
        const val FORMAT_PCM: Short = 1

        /** Indicates ALAW format.  */
        const val FORMAT_ALAW: Short = 6

        /** Indicates ULAW format.  */
        const val FORMAT_ULAW: Short = 7
        @Throws(IOException::class)
        private fun readId(`in`: InputStream, id: String) {
            for (i in 0 until id.length) {
                if (id[i].toInt() != `in`.read()) throw IOException("$id tag not present")
            }
        }

        @Throws(IOException::class)
        private fun readInt(`in`: InputStream): Int {
            return `in`.read() or (`in`.read() shl 8) or (`in`.read() shl 16) or (`in`.read() shl 24)
        }

        @Throws(IOException::class)
        private fun readShort(`in`: InputStream): Short {
            return (`in`.read() or (`in`.read() shl 8)).toShort()
        }

        @Throws(IOException::class)
        private fun writeId(out: OutputStream, id: String) {
            for (i in 0 until id.length) out.write(id[i].toInt())
        }

        @Throws(IOException::class)
        private fun writeInt(out: OutputStream, `val`: Int) {
            out.write(`val`)
            out.write(`val` shr 8)
            out.write(`val` shr 16)
            out.write(`val` shr 24)
        }

        @Throws(IOException::class)
        private fun writeShort(out: OutputStream, `val`: Short) {
            out.write(`val`.toInt())
            out.write(`val`.toInt() shr 8)
        }
    }
}