/**
 * Created by "Mohamad Abuzaid" on 12/13/2022.
 */

package uk.org.cgatechnologies.wideya.common.camera.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class OverlayView : View {
    private var mPaint: Paint? = null
    private var mStrokePaint: Paint? = null
    private val mPath: Path = Path()

    constructor(context: Context?) : super(context) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initPaints()
    }

    private fun initPaints() {
        mPaint = Paint()
        mPaint?.color = Color.parseColor("#A6000000")
        mStrokePaint = Paint()
        mStrokePaint?.color = Color.WHITE
        mStrokePaint?.strokeWidth = 2F
        mStrokePaint?.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPath.reset()
        val left: Float = (width / 3).toFloat()
        val top: Float = (height / 6).toFloat()
        val right: Float = (left * 2)
        val bottom: Float = (height / 2).toFloat()
        val strokeWidth: Float = ((height - width) / 2).toFloat()

        mPaint?.strokeWidth = strokeWidth
        mPath.addOval(
            left,
            top,
            right,
            bottom,
            Path.Direction.CW
        )
        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        mStrokePaint?.let {
            canvas.drawOval(
                left,
                top,
                right,
                bottom, it
            )
        }
        mPaint?.let { canvas.drawPath(mPath, it) }
    }
}