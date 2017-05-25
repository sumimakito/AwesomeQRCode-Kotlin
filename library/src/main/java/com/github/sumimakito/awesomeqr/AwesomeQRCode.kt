package com.github.sumimakito.awesomeqr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF

import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode

import java.util.Hashtable

object AwesomeQRCode {
    /**
     * For more information about QR code, refer to: https://en.wikipedia.org/wiki/QR_code
     * BYTE_EPT: Empty block
     * BYTE_DTA: Data block
     * BYTE_POS: Position block
     * BYTE_AGN: Align block
     * BYTE_TMG: Timing block
     * BYTE_PTC: Protector block, translucent layer (custom block, this is not included in QR code's standards)
     */
    private val BYTE_EPT: Byte = 0x0
    private val BYTE_DTA: Byte = 0x1
    private val BYTE_POS: Byte = 0x2
    private val BYTE_AGN: Byte = 0x3
    private val BYTE_TMG: Byte = 0x4
    private val BYTE_PTC: Byte = 0x5

    private val DEFAULT_DTA_DOT_SCALE = 0.3f
    private val DEFAULT_LOGO_SCALE = 0.2f
    private val DEFAULT_SIZE = 800
    private val DEFAULT_MARGIN = 20
    private val DEFAULT_LOGO_MARGIN = 10
    private val DEFAULT_LOGO_RADIUS = 8
    private val DEFAULT_BINARIZING_THRESHOLD = 128

    /**
     * Create a QR matrix and render it use given configs.

     * @param contents          Contents to encode.
     * *
     * @param size              Width as well as the height of the output QR code, includes margin.
     * *
     * @param margin            Margin to add around the QR code.
     * *
     * @param dataDotScale      Scale the data blocks and makes them appear smaller.
     * *
     * @param colorDark         Color of blocks. Will be OVERRIDE by autoColor. (BYTE_DTA, BYTE_POS, BYTE_AGN, BYTE_TMG)
     * *
     * @param colorLight        Color of empty space. Will be OVERRIDE by autoColor. (BYTE_EPT)
     * *
     * @param backgroundImage   The background image to embed in the QR code. If null, no background image will be embedded.
     * *
     * @param whiteMargin       If true, background image will not be drawn on the margin area.
     * *
     * @param autoColor         If true, colorDark will be set to the dominant color of backgroundImage.
     * *
     * @param binarize          If true, all images will be binarized while rendering. Default is false.
     * *
     * @param binarizeThreshold Threshold value used while binarizing. Default is 128. 0 < threshold < 255.
     * *
     * @param roundedDataDots   If true, data blocks will appear as filled circles. Default is false.
     * *
     * @param logoImage         The logo image which appears at the center of the QR code. Null to disable.
     * *
     * @param logoMargin        The margin around the logo image. 0 to disable.
     * *
     * @param logoCornerRadius  The radius of logo image's corners. 0 to disable.
     * *
     * @param logoScale         Logo's size = logoScale * innerRenderSize
     * *
     * @return Bitmap of QR code
     * *
     * @throws IllegalArgumentException Refer to the messages below.
     */
    @Throws(IllegalArgumentException::class)
    private fun create(contents: String?, size: Int, margin: Int, dataDotScale: Float, colorDark: Int,
                       colorLight: Int, backgroundImage: Bitmap?, whiteMargin: Boolean, autoColor: Boolean,
                       binarize: Boolean, binarizeThreshold: Int, roundedDataDots: Boolean,
                       logoImage: Bitmap?, logoMargin: Int, logoCornerRadius: Int, logoScale: Float): Bitmap {
        if (contents == null) {
            throw IllegalArgumentException("Error: contents is empty. (contents.isEmpty())")
        }
        if (contents.isEmpty()) {
            throw IllegalArgumentException("Error: contents is empty. (contents.isEmpty())")
        }
        if (size < 0) {
            throw IllegalArgumentException("Error: a negative size is given. (size < 0)")
        }
        if (margin < 0) {
            throw IllegalArgumentException("Error: a negative margin is given. (margin < 0)")
        }
        if (size - 2 * margin <= 0) {
            throw IllegalArgumentException("Error: there is no space left for the QRCode. (size - 2 * margin <= 0)")
        }
        val byteMatrix = getBitMatrix(contents)
        if (size - 2 * margin < byteMatrix!!.width) {
            throw IllegalArgumentException("Error: there is no space left for the QRCode. (size - 2 * margin < " + byteMatrix.width + ")")
        }
        if (dataDotScale < 0 || dataDotScale > 1) {
            throw IllegalArgumentException("Error: an illegal data dot scale is given. (dataDotScale < 0 || dataDotScale > 1)")
        }
        return render(byteMatrix, size - 2 * margin, margin, dataDotScale, colorDark, colorLight, backgroundImage,
                whiteMargin, autoColor, binarize, binarizeThreshold, roundedDataDots, logoImage, logoMargin,
                logoCornerRadius, logoScale)
    }

    private fun render(byteMatrix: ByteMatrix, innerRenderedSize: Int, margin: Int, dataDotScale: Float,
                       colorDark: Int, colorLight: Int, backgroundImage: Bitmap?, whiteMargin: Boolean,
                       autoColor: Boolean, binarize: Boolean, binarizeThreshold: Int, roundedDataDots: Boolean,
                       logoImage: Bitmap?, logoMargin: Int, logoCornerRadius: Int, logoScale: Float): Bitmap {
        var colorDark = colorDark
        var colorLight = colorLight
        var logoMargin = logoMargin
        var logoCornerRadius = logoCornerRadius
        var logoScale = logoScale
        val nCount = byteMatrix.width
        val nWidth = innerRenderedSize.toFloat() / nCount
        val nHeight = innerRenderedSize.toFloat() / nCount

        val backgroundImageScaled = Bitmap.createBitmap(
                innerRenderedSize + if (whiteMargin) 0 else margin * 2,
                innerRenderedSize + if (whiteMargin) 0 else margin * 2,
                Bitmap.Config.ARGB_8888)
        if (backgroundImage != null) {
            scaleBitmap(backgroundImage, backgroundImageScaled)
        }

        val renderedBitmap = Bitmap.createBitmap(innerRenderedSize + margin * 2, innerRenderedSize + margin * 2, Bitmap.Config.ARGB_8888)

        if (autoColor && backgroundImage != null) {
            colorDark = getDominantColor(backgroundImage)
        }

        var binThreshold = DEFAULT_BINARIZING_THRESHOLD
        if (binarize) {
            if (binarizeThreshold > 0 && binarizeThreshold < 255) {
                binThreshold = binarizeThreshold
            }
            colorDark = Color.BLACK
            colorLight = Color.WHITE
            if (backgroundImage != null)
                binarize(backgroundImageScaled, binThreshold)
        }

        val paint = Paint()
        paint.isAntiAlias = true
        val paintDark = Paint()
        paintDark.color = colorDark
        paintDark.isAntiAlias = true
        paintDark.style = Paint.Style.FILL
        val paintLight = Paint()
        paintLight.color = colorLight
        paintLight.isAntiAlias = true
        paintLight.style = Paint.Style.FILL
        val paintProtector = Paint()
        paintProtector.color = Color.argb(120, 255, 255, 255)
        paintProtector.isAntiAlias = true
        paintProtector.style = Paint.Style.FILL

        val canvas = Canvas(renderedBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(backgroundImageScaled, (if (whiteMargin) margin else 0).toFloat(), (if (whiteMargin) margin else 0).toFloat(), paint)


        for (row in 0..byteMatrix.height - 1) {
            for (col in 0..byteMatrix.width - 1) {
                when (byteMatrix.get(col, row)) {
                    BYTE_AGN, BYTE_POS, BYTE_TMG -> canvas.drawRect(
                            margin + col * nWidth,
                            margin + row * nHeight,
                            margin + (col + 1.0f) * nWidth,
                            margin + (row + 1.0f) * nHeight,
                            paintDark
                    )
                    BYTE_DTA -> if (roundedDataDots) {
                        canvas.drawCircle(
                                margin + (col + 0.5f) * nWidth,
                                margin + (row + 0.5f) * nHeight,
                                dataDotScale * nHeight * 0.5f,
                                paintDark
                        )
                    } else {
                        canvas.drawRect(
                                margin + (col + 0.5f * (1 - dataDotScale)) * nWidth,
                                margin + (row + 0.5f * (1 - dataDotScale)) * nHeight,
                                margin + (col + 0.5f * (1 + dataDotScale)) * nWidth,
                                margin + (row + 0.5f * (1 + dataDotScale)) * nHeight,
                                paintDark
                        )
                    }
                    BYTE_PTC -> canvas.drawRect(
                            margin + col * nWidth,
                            margin + row * nHeight,
                            margin + (col + 1.0f) * nWidth,
                            margin + (row + 1.0f) * nHeight,
                            paintProtector
                    )
                    BYTE_EPT -> if (roundedDataDots) {
                        canvas.drawCircle(
                                margin + (col + 0.5f) * nWidth,
                                margin + (row + 0.5f) * nHeight,
                                dataDotScale * nHeight * 0.5f,
                                paintLight
                        )
                    } else {
                        canvas.drawRect(
                                margin + (col + 0.5f * (1 - dataDotScale)) * nWidth,
                                margin + (row + 0.5f * (1 - dataDotScale)) * nHeight,
                                margin + (col + 0.5f * (1 + dataDotScale)) * nWidth,
                                margin + (row + 0.5f * (1 + dataDotScale)) * nHeight,
                                paintLight
                        )
                    }
                }
            }
        }

        if (logoImage != null) {
            if (logoScale <= 0 || logoScale >= 1) {
                logoScale = DEFAULT_LOGO_SCALE
            }
            if (logoMargin < 0 || logoMargin * 2 >= innerRenderedSize) {
                logoMargin = DEFAULT_LOGO_MARGIN
            }
            val logoScaledSize = (innerRenderedSize * logoScale).toInt()

            if (logoCornerRadius < 0) logoCornerRadius = 0
            if (logoCornerRadius * 2 > logoScaledSize)
                logoCornerRadius = (logoScaledSize * 0.5).toInt()

            val logoScaled = Bitmap.createScaledBitmap(logoImage, logoScaledSize, logoScaledSize, true)
            val logoOpt = Bitmap.createBitmap(logoScaled.width, logoScaled.height, Bitmap.Config.ARGB_8888)
            val logoCanvas = Canvas(logoOpt)
            val logoRect = Rect(0, 0, logoScaled.width, logoScaled.height)
            val logoRectF = RectF(logoRect)
            val logoPaint = Paint()
            logoPaint.isAntiAlias = true
            logoPaint.color = 0xFFFFFFFF.toInt()
            logoPaint.style = Paint.Style.FILL
            logoCanvas.drawARGB(0, 0, 0, 0)
            logoCanvas.drawRoundRect(logoRectF, logoCornerRadius.toFloat(), logoCornerRadius.toFloat(), logoPaint)
            logoPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            logoCanvas.drawBitmap(logoScaled, logoRect, logoRect, logoPaint)
            logoPaint.color = colorLight
            logoPaint.style = Paint.Style.STROKE
            logoPaint.strokeWidth = logoMargin.toFloat()
            logoCanvas.drawRoundRect(logoRectF, logoCornerRadius.toFloat(), logoCornerRadius.toFloat(), logoPaint)

            if (binarize)
                binarize(logoOpt, binThreshold)

            canvas.drawBitmap(logoOpt, (0.5 * (renderedBitmap.width - logoOpt.width)).toInt().toFloat(),
                    (0.5 * (renderedBitmap.height - logoOpt.height)).toInt().toFloat(), paint)
        }

        return renderedBitmap
    }

    private fun getBitMatrix(contents: String): ByteMatrix? {
        try {
            val qrCode = getProtoQRCode(contents, ErrorCorrectionLevel.H)
            val agnCenter = qrCode.version.alignmentPatternCenters
            val byteMatrix = qrCode.matrix
            val matSize = byteMatrix.width
            for (row in 0..matSize - 1) {
                for (col in 0..matSize - 1) {
                    if (isTypeAGN(col, row, agnCenter, true)) {
                        if (byteMatrix.get(col, row) != BYTE_EPT) {
                            byteMatrix.set(col, row, BYTE_AGN)
                        } else {
                            byteMatrix.set(col, row, BYTE_PTC)
                        }
                    } else if (isTypePOS(col, row, matSize, true)) {
                        if (byteMatrix.get(col, row) != BYTE_EPT) {
                            byteMatrix.set(col, row, BYTE_POS)
                        } else {
                            byteMatrix.set(col, row, BYTE_PTC)
                        }
                    } else if (isTypeTMG(col, row, matSize)) {
                        if (byteMatrix.get(col, row) != BYTE_EPT) {
                            byteMatrix.set(col, row, BYTE_TMG)
                        } else {
                            byteMatrix.set(col, row, BYTE_PTC)
                        }
                    }

                    if (isTypePOS(col, row, matSize, false)) {
                        if (byteMatrix.get(col, row) == BYTE_EPT) {
                            byteMatrix.set(col, row, BYTE_PTC)
                        }
                    }
                }
            }
            return byteMatrix
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * @param contents             Contents to encode.
     * *
     * @param errorCorrectionLevel ErrorCorrectionLevel
     * *
     * @return QR code object.
     * *
     * @throws WriterException Refer to the messages below.
     */
    @Throws(WriterException::class)
    private fun getProtoQRCode(contents: String, errorCorrectionLevel: ErrorCorrectionLevel): QRCode {
        if (contents.isEmpty()) {
            throw IllegalArgumentException("Found empty contents")
        }
        val hintMap = Hashtable<EncodeHintType, Any>()
        hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8")
        hintMap.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel)
        return Encoder.encode(contents, errorCorrectionLevel, hintMap)
    }

    private fun isTypeAGN(x: Int, y: Int, agnCenter: IntArray, edgeOnly: Boolean): Boolean {
        if (agnCenter.isEmpty()) return false
        val edgeCenter = agnCenter[agnCenter.size - 1]
        for (agnY in agnCenter) {
            for (agnX in agnCenter) {
                if (edgeOnly && agnX != 6 && agnY != 6 && agnX != edgeCenter && agnY != edgeCenter)
                    continue
                if (agnX == 6 && agnY == 6 || agnX == 6 && agnY == edgeCenter || agnY == 6 && agnX == edgeCenter)
                    continue
                if (x >= agnX - 2 && x <= agnX + 2 && y >= agnY - 2 && y <= agnY + 2)
                    return true
            }
        }
        return false
    }

    private fun isTypePOS(x: Int, y: Int, size: Int, inner: Boolean): Boolean {
        if (inner) {
            return x < 7 && (y < 7 || y >= size - 7) || x >= size - 7 && y < 7
        } else {
            return x <= 7 && (y <= 7 || y >= size - 8) || x >= size - 8 && y <= 7
        }
    }

    private fun isTypeTMG(x: Int, y: Int, size: Int): Boolean {
        return y == 6 && x >= 8 && x < size - 8 || x == 6 && y >= 8 && y < size - 8
    }

    private fun scaleBitmap(src: Bitmap, dst: Bitmap) {
        val cPaint = Paint()
        cPaint.isAntiAlias = true
        cPaint.isDither = true
        cPaint.isFilterBitmap = true

        val ratioX = dst.width / src.width.toFloat()
        val ratioY = dst.height / src.height.toFloat()
        val middleX = dst.width * 0.5f
        val middleY = dst.height * 0.5f

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(dst)
        canvas.matrix = scaleMatrix
        canvas.drawBitmap(src, middleX - src.width / 2,
                middleY - src.height / 2, cPaint)
    }

    private fun getDominantColor(bitmap: Bitmap): Int {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        var red = 0
        var green = 0
        var blue = 0
        var c = 0
        var r: Int
        var g: Int
        var b: Int
        for (y in 0..newBitmap.height - 1) {
            for (x in 0..newBitmap.height - 1) {
                val color = newBitmap.getPixel(x, y)
                r = color shr 16 and 0xFF
                g = color shr 8 and 0xFF
                b = color and 0xFF
                if (r > 200 || g > 200 || b > 200) continue
                red += r
                green += g
                blue += b
                c++
            }
        }
        newBitmap.recycle()
        red = Math.max(0, Math.min(0xFF, red / c))
        green = Math.max(0, Math.min(0xFF, green / c))
        blue = Math.max(0, Math.min(0xFF, blue / c))
        return 0xFF shl 24 or (red shl 16) or (green shl 8) or blue
    }

    private fun binarize(bitmap: Bitmap, threshold: Int) {
        var r: Int
        var g: Int
        var b: Int
        for (y in 0..bitmap.height - 1) {
            for (x in 0..bitmap.height - 1) {
                val color = bitmap.getPixel(x, y)
                r = color shr 16 and 0xFF
                g = color shr 8 and 0xFF
                b = color and 0xFF
                val sum = 0.30f * r + 0.59f * g + 0.11f * b
                bitmap.setPixel(x, y, if (sum > threshold) Color.WHITE else Color.BLACK)
            }
        }
    }

    class Renderer {
        private var contents: String? = null
        private var size: Int = 0
        private var margin: Int = 0
        private var dataDotScale: Float = 0.toFloat()
        private var colorDark: Int = 0
        private var colorLight: Int = 0
        private var backgroundImage: Bitmap? = null
        private var whiteMargin: Boolean = false
        private var autoColor: Boolean = false
        private var binarize: Boolean = false
        private var binarizeThreshold: Int = 0
        private var roundedDataDots: Boolean = false
        private var logoImage: Bitmap? = null
        private var logoMargin: Int = 0
        private var logoCornerRadius: Int = 0
        private var logoScale: Float = 0.toFloat()

        init {
            size = DEFAULT_SIZE
            margin = DEFAULT_MARGIN
            dataDotScale = DEFAULT_DTA_DOT_SCALE
            colorDark = Color.BLACK
            colorLight = Color.WHITE
            colorDark = Color.BLACK
            whiteMargin = true
            autoColor = true
            binarize = false
            binarizeThreshold = DEFAULT_BINARIZING_THRESHOLD
            roundedDataDots = false
            logoMargin = DEFAULT_LOGO_MARGIN
            logoCornerRadius = DEFAULT_LOGO_RADIUS
            logoScale = DEFAULT_LOGO_SCALE
        }

        fun autoColor(autoColor: Boolean): Renderer {
            this.autoColor = autoColor
            return this
        }

        fun background(backgroundImage: Bitmap?): Renderer {
            this.backgroundImage = backgroundImage
            return this
        }

        fun binarize(binarize: Boolean): Renderer {
            this.binarize = binarize
            return this
        }

        fun binarizeThreshold(binarizeThreshold: Int): Renderer {
            this.binarizeThreshold = binarizeThreshold
            return this
        }

        fun colorDark(colorDark: Int): Renderer {
            this.colorDark = colorDark
            return this
        }

        fun colorLight(colorLight: Int): Renderer {
            this.colorLight = colorLight
            return this
        }

        fun contents(contents: String?): Renderer {
            this.contents = contents
            return this
        }

        fun dotScale(dataDotScale: Float): Renderer {
            this.dataDotScale = dataDotScale
            return this
        }

        fun logoRadius(logoCornerRadius: Int): Renderer {
            this.logoCornerRadius = logoCornerRadius
            return this
        }

        fun logo(logoImage: Bitmap?): Renderer {
            this.logoImage = logoImage
            return this
        }

        fun logoMargin(logoMargin: Int): Renderer {
            this.logoMargin = logoMargin
            return this
        }

        fun logoScale(logoScale: Float): Renderer {
            this.logoScale = logoScale
            return this
        }

        fun margin(margin: Int): Renderer {
            this.margin = margin
            return this
        }

        fun roundedDots(roundedDataDots: Boolean): Renderer {
            this.roundedDataDots = roundedDataDots
            return this
        }

        fun size(size: Int): Renderer {
            this.size = size
            return this
        }

        fun whiteMargin(whiteMargin: Boolean): Renderer {
            this.whiteMargin = whiteMargin
            return this
        }

        @Throws(IllegalArgumentException::class)
        fun render(): Bitmap {
            return create(
                    contents, size, margin, dataDotScale, colorDark, colorLight,
                    backgroundImage, whiteMargin, autoColor, binarize, binarizeThreshold,
                    roundedDataDots, logoImage, logoMargin, logoCornerRadius, logoScale
            )
        }

        @Throws(IllegalArgumentException::class)
        fun renderAsync(callback: Callback?) {
            object : Thread() {
                override fun run() {
                    super.run()
                    try {
                        val bitmap = create(
                                contents, size, margin, dataDotScale, colorDark, colorLight,
                                backgroundImage, whiteMargin, autoColor, binarize, binarizeThreshold,
                                roundedDataDots, logoImage, logoMargin, logoCornerRadius, logoScale
                        )
                        callback?.onRendered(this@Renderer, bitmap)
                    } catch (e: Exception) {
                        callback?.onError(this@Renderer, e)
                    }

                }
            }.start()
        }
    }

    interface Callback {
        fun onRendered(renderer: Renderer, bitmap: Bitmap)

        fun onError(renderer: Renderer, e: Exception)
    }
}

