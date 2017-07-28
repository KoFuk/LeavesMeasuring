package com.chronoscoper.android.leasure

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.azeesoft.lib.colorpicker.ColorPickerDialog
import com.chronoscoper.android.leasure.widget.GridImageView
import kotterknife.bindView


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_FILE_PICK = 2
    }

    private val menu by bindView<View>(R.id.menu)
    private val gestureDetector by lazy { GestureDetector(this, gestureListener) }

    private val gridScaleSeekBar by bindView<SeekBar>(R.id.grid_scale)
    private val calculateButton by bindView<Button>(R.id.calculate)
    private val resultText by bindView<TextView>(R.id.result)
    private val colorRangeStartButton by bindView<Button>(R.id.color_range_start)
    private val colorRangeEndButton by bindView<Button>(R.id.color_range_end)

    private val preference by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var colorRangeStart = -1
        set(value) {
            preference.edit().putInt("color_range_start", value).apply()
            field = value
        }
        get() {
            if (field < 0) {
                field = preference.getInt("color_range_start", 0x000000)
            }
            return field
        }

    private var colorRangeEnd = -1
        set(value) {
            preference.edit().putInt("color_range_end", value).apply()
            field = value
        }
        get() {
            if (field < 0) {
                field = preference.getInt("color_range_end", 0x000000)
            }
            return field
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menu.setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }

        gridScaleSeekBar.progress = preference.getInt("grid_scale", 100)
        gridScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar?, progress: Int, p2: Boolean) {
                if (progress != 0) {
                    preview.gridScale = progress
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        calculateButton.setOnClickListener {
            object : AsyncTask<Unit, Unit, Bitmap>() {
                override fun onPreExecute() {
                    super.onPreExecute()
                    calculateButton.isEnabled = false
                }

                override fun doInBackground(vararg p0: Unit?): Bitmap? {
                    if (bitmap == null) return null
                    val bitmap = bitmap!!
                    val startR = Color.red(colorRangeStart)
                    val startG = Color.green(colorRangeStart)
                    val startB = Color.blue(colorRangeStart)
                    val endR = Color.red(colorRangeEnd)
                    val endG = Color.green(colorRangeEnd)
                    val endB = Color.blue(colorRangeEnd)

                    val h = bitmap.height
                    val w = bitmap.width
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                    val pixelCount = h * w
                    detectedPixels = 0
                    for (x in 0..w - 1) {
                        for (y in 0..h - 1) {
                            val pixel = bitmap.getPixel(x, y)
                            val pixelR = Color.red(pixel)
                            val pixelG = Color.green(pixel)
                            val pixedB = Color.blue(pixel)
                            if ((pixelR in startR..endR || pixelR in endR..startR)
                                    && (pixelG in startG..endG || pixelG in endG..startG)
                                    && (pixedB in startB..endB || pixedB in endB..startB)) {
                                detectedPixels++
                                result.setPixel(x, y, colorRangeEnd)
                            } else {
                                result.setPixel(x, y, Color.WHITE)
                            }
                        }
                    }

                    val fraction = detectedPixels.toFloat() / pixelCount.toFloat()
                    totalPixels = w * h
                    totalMeasure = getActualLength(w) * getActualLength(h)
                    percentage = fraction * 100f
                    squareMeasure = totalMeasure * fraction
                    return result
                }

                private var totalPixels = 0
                private var detectedPixels = 0
                private var totalMeasure = 0f
                private var percentage = 0f
                private var squareMeasure = 0f

                override fun onPostExecute(result: Bitmap?) {
                    super.onPostExecute(result)
                    calculateButton.isEnabled = true
                    resultText.text = getString(R.string.result,
                            totalPixels, detectedPixels, totalMeasure, squareMeasure, percentage)
                    if (result != null) {
                        preview.setImageBitmap(result)
                    }
                }

                private fun getActualLength(pixelLength: Int): Float
                        = pixelLength.toFloat() / preview.gridScale.toFloat()

            }.execute()
        }

        colorRangeStartButton.setBackgroundColor(colorRangeStart)
        colorRangeStartButton.setOnClickListener {
            ColorPickerDialog.createColorPickerDialog(this)
                    .apply {
                        setHexaDecimalTextColor(colorRangeStart)
                        setOnColorPickedListener { color, _ ->
                            colorRangeStart = color
                            it.setBackgroundColor(color)
                        }
                    }
                    .show()
        }
        colorRangeEndButton.setBackgroundColor(colorRangeEnd)
        colorRangeEndButton.setOnClickListener {
            ColorPickerDialog.createColorPickerDialog(this)
                    .apply {
                        setHexaDecimalTextColor(colorRangeEnd)
                        setOnColorPickedListener { color, _ ->
                            colorRangeEnd = color
                            it.setBackgroundColor(color)
                        }
                    }
                    .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.pick_image) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_FILE_PICK)

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val preview by bindView<GridImageView>(R.id.preview)
    private var bitmap: Bitmap? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_PICK
                && resultCode == Activity.RESULT_OK
                && data != null) {
            val stream = contentResolver.openInputStream(data.data)
            bitmap = BitmapFactory.decodeStream(stream)

            preview.setImageBitmap(bitmap)
        }
    }

    private val gestureListener = object : GestureDetector.OnGestureListener {
        override fun onShowPress(p0: MotionEvent?) {}

        override fun onSingleTapUp(p0: MotionEvent?): Boolean = false

        override fun onDown(p0: MotionEvent?): Boolean = false

        override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float)
                : Boolean = false

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, x: Float, y: Float): Boolean {
            menu.y = menu.y - y
            return true
        }

        override fun onLongPress(p0: MotionEvent?) {}
    }
}
