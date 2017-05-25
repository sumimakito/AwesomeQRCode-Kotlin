package com.github.sumimakito.awesomeqrsamplekt

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import com.github.sumimakito.awesomeqr.AwesomeQRCode

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val BKG_IMAGE = 822
    private val LOGO_IMAGE = 379

    private var qrCodeImageView: ImageView? = null
    private var etColorLight: EditText? = null
    private var etColorDark: EditText? = null
    private var etContents: EditText? = null
    private var etMargin: EditText? = null
    private var etSize: EditText? = null
    private var btGenerate: Button? = null
    private var btSelectBG: Button? = null
    private var btRemoveBackgroundImage: Button? = null
    private var ckbWhiteMargin: CheckBox? = null
    private var backgroundImage: Bitmap? = null
    private var progressDialog: ProgressDialog? = null
    private var generating = false
    private var ckbAutoColor: CheckBox? = null
    private var tvAuthorHint: TextView? = null
    private var scrollView: ScrollView? = null
    private var etDotScale: EditText? = null
    private var tvJSHint: TextView? = null
    private var ckbBinarize: CheckBox? = null
    private var ckbRoundedDataDots: CheckBox? = null
    private var etBinarizeThreshold: EditText? = null
    private val qrBitmap: Bitmap? = null
    private var btOpen: Button? = null
    private var etLogoMargin: EditText? = null
    private var etLogoScale: EditText? = null
    private var etLogoCornerRadius: EditText? = null
    private var btRemoveLogoImage: Button? = null
    private var btSelectLogo: Button? = null
    private var logoImage: Bitmap? = null
    private var configViewContainer: ViewGroup? = null
    private var resultViewContainer: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configViewContainer = findViewById(R.id.configViewContainer) as ViewGroup
        resultViewContainer = findViewById(R.id.resultViewContainer) as ViewGroup

        scrollView = findViewById(R.id.scrollView) as ScrollView
        scrollView = findViewById(R.id.scrollView) as ScrollView
        tvAuthorHint = findViewById(R.id.authorHint) as TextView
        tvJSHint = findViewById(R.id.jsHint) as TextView
        qrCodeImageView = findViewById(R.id.qrcode) as ImageView
        etColorLight = findViewById(R.id.colorLight) as EditText
        etColorDark = findViewById(R.id.colorDark) as EditText
        etContents = findViewById(R.id.contents) as EditText
        etSize = findViewById(R.id.size) as EditText
        etMargin = findViewById(R.id.margin) as EditText
        etDotScale = findViewById(R.id.dotScale) as EditText
        btSelectBG = findViewById(R.id.backgroundImage) as Button
        btSelectLogo = findViewById(R.id.logoImage) as Button
        btRemoveBackgroundImage = findViewById(R.id.removeBackgroundImage) as Button
        btRemoveLogoImage = findViewById(R.id.removeLogoImage) as Button
        btGenerate = findViewById(R.id.generate) as Button
        btOpen = findViewById(R.id.open) as Button
        ckbWhiteMargin = findViewById(R.id.whiteMargin) as CheckBox
        ckbAutoColor = findViewById(R.id.autoColor) as CheckBox
        ckbBinarize = findViewById(R.id.binarize) as CheckBox
        ckbRoundedDataDots = findViewById(R.id.rounded) as CheckBox
        etBinarizeThreshold = findViewById(R.id.binarizeThreshold) as EditText
        etLogoMargin = findViewById(R.id.logoMargin) as EditText
        etLogoScale = findViewById(R.id.logoScale) as EditText
        etLogoCornerRadius = findViewById(R.id.logoRadius) as EditText
        etBinarizeThreshold = findViewById(R.id.binarizeThreshold) as EditText

        ckbAutoColor!!.setOnCheckedChangeListener { buttonView, isChecked ->
            etColorLight!!.isEnabled = !isChecked
            etColorDark!!.isEnabled = !isChecked
        }

        ckbBinarize!!.setOnCheckedChangeListener { buttonView, isChecked -> etBinarizeThreshold!!.isEnabled = isChecked }

        btSelectBG!!.setOnClickListener {
            val intent: Intent
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                intent = Intent(Intent.ACTION_GET_CONTENT)
            } else {
                intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
            }
            intent.type = "image/*"
            startActivityForResult(intent, BKG_IMAGE)
        }

        btSelectLogo!!.setOnClickListener {
            val intent: Intent
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                intent = Intent(Intent.ACTION_GET_CONTENT)
            } else {
                intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
            }
            intent.type = "image/*"
            startActivityForResult(intent, LOGO_IMAGE)
        }

        btOpen!!.setOnClickListener { if (qrBitmap != null) saveBitmap(qrBitmap) }

        btRemoveBackgroundImage!!.setOnClickListener {
            backgroundImage = null
            Toast.makeText(this@MainActivity, "Background image removed.", Toast.LENGTH_SHORT).show()
        }

        btRemoveLogoImage!!.setOnClickListener {
            logoImage = null
            Toast.makeText(this@MainActivity, "Logo image removed.", Toast.LENGTH_SHORT).show()
        }

        btGenerate!!.setOnClickListener {
            try {
                generate(if (etContents!!.text.isEmpty()) "Makito loves Kafuu Chino." else etContents!!.text.toString(),
                        if (etSize!!.text.isEmpty()) 800 else Integer.parseInt(etSize!!.text.toString()),
                        if (etMargin!!.text.isEmpty()) 20 else Integer.parseInt(etMargin!!.text.toString()),
                        if (etDotScale!!.text.isEmpty()) 0.3f else java.lang.Float.parseFloat(etDotScale!!.text.toString()),
                        if (ckbAutoColor!!.isChecked) Color.BLACK else Color.parseColor(etColorDark!!.text.toString()),
                        if (ckbAutoColor!!.isChecked) Color.WHITE else Color.parseColor(etColorLight!!.text.toString()),
                        backgroundImage,
                        ckbWhiteMargin!!.isChecked,
                        ckbAutoColor!!.isChecked,
                        ckbBinarize!!.isChecked,
                        if (etBinarizeThreshold!!.text.isEmpty()) 128 else Integer.parseInt(etBinarizeThreshold!!.text.toString()),
                        ckbRoundedDataDots!!.isChecked,
                        logoImage,
                        if (etLogoMargin!!.text.isEmpty()) 10 else Integer.parseInt(etLogoMargin!!.text.toString()),
                        if (etLogoCornerRadius!!.text.isEmpty()) 8 else Integer.parseInt(etLogoCornerRadius!!.text.toString()),
                        if (etLogoScale!!.text.isEmpty()) 10f else java.lang.Float.parseFloat(etLogoScale!!.text.toString())
                )
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error occurred, please check your configs.", Toast.LENGTH_LONG).show()
            }
        }

        tvAuthorHint!!.setOnClickListener {
            val url = "https://github.com/SumiMakito/AwesomeQRCode"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        tvJSHint!!.setOnClickListener {
            val url = "https://github.com/SumiMakito/Awesome-qr.js"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

    }

    override fun onResume() {
        super.onResume()
        acquireStoragePermissions()
    }

    private fun acquireStoragePermissions() {
        val permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == RESULT_OK && data.data != null) {
            try {
                val imageUri = data.data
                if (requestCode == BKG_IMAGE) {
                    backgroundImage = BitmapFactory.decodeFile(ContentHelper.absolutePathFromUri(this, imageUri))
                    Toast.makeText(this, "Background image added.", Toast.LENGTH_SHORT).show()
                } else if (requestCode == LOGO_IMAGE) {
                    logoImage = BitmapFactory.decodeFile(ContentHelper.absolutePathFromUri(this, imageUri))
                    Toast.makeText(this, "Logo image added.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (requestCode == BKG_IMAGE) {
                    Toast.makeText(this, "Failed to add the background image.", Toast.LENGTH_SHORT).show()
                } else if (requestCode == LOGO_IMAGE) {
                    Toast.makeText(this, "Failed to add the logo image.", Toast.LENGTH_SHORT).show()
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun generate(contents: String?, size: Int, margin: Int, dotScale: Float,
                         colorDark: Int, colorLight: Int, background: Bitmap?, whiteMargin: Boolean,
                         autoColor: Boolean, binarize: Boolean, binarizeThreshold: Int, roundedDD: Boolean,
                         logoImage: Bitmap?, logoMargin: Int, logoCornerRadius: Int, logoScale: Float) {
        if (generating) return
        generating = true
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Generating...")
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
        AwesomeQRCode.Renderer().contents(contents)g
                .size(size).margin(margin).dotScale(dotScale)
                .colorDark(colorDark).colorLight(colorLight)
                .background(background).whiteMargin(whiteMargin)
                .autoColor(autoColor).roundedDots(roundedDD)
                .binarize(binarize).binarizeThreshold(binarizeThreshold)
                .logo(logoImage).logoMargin(logoMargin)
                .logoRadius(logoCornerRadius).logoScale(logoScale)
                .renderAsync(object : AwesomeQRCode.Callback {
                    override fun onRendered(renderer: AwesomeQRCode.Renderer, bitmap: Bitmap) {
                        runOnUiThread {
                            qrCodeImageView!!.setImageBitmap(bitmap)
                            configViewContainer!!.visibility = View.GONE
                            resultViewContainer!!.visibility = View.VISIBLE
                            if (progressDialog != null) progressDialog!!.dismiss()
                            generating = false
                        }
                    }

                    override fun onError(renderer: AwesomeQRCode.Renderer, e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            if (progressDialog != null) progressDialog!!.dismiss()
                            configViewContainer!!.visibility = View.VISIBLE
                            resultViewContainer!!.visibility = View.GONE
                            generating = false
                        }
                    }
                })
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val fos: FileOutputStream?
        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val outputFile = File(publicContainer, System.currentTimeMillis().toString() + ".png")
            fos = FileOutputStream(outputFile)
            fos.write(byteArray)
            fos.close()
            Toast.makeText(this, "Image saved to " + outputFile.absolutePath, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save the image.", Toast.LENGTH_LONG).show()
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (configViewContainer!!.visibility != View.VISIBLE) {
                configViewContainer!!.visibility = View.VISIBLE
                resultViewContainer!!.visibility = View.GONE
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val publicContainer: File
            get() {
                val musicContainer = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val aqr = File(musicContainer, "AwesomeQR")
                if (aqr.exists() && !aqr.isDirectory) {
                    aqr.delete()
                }
                aqr.mkdirs()
                return aqr
            }
    }
}
