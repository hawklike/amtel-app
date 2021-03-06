package cz.prague.cvut.fit.steuejan.amtelapp

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

class App : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        mContext = WeakReference(this)
    }

    companion object
    {
        private var mContext: WeakReference<Context>? = null
        val context: Context
            get() = mContext?.get()!!

        fun toast(text: String, context: Context = this.context, length: Int = Toast.LENGTH_SHORT)
        {
            Toast.makeText(context, text, length).show()
        }

        fun toast(@StringRes textRes: Int, context: Context = this.context, length: Int = Toast.LENGTH_SHORT)
        {
            Toast.makeText(context, context.getString(textRes), length).show()
        }

        fun getColor(@ColorRes color: Int): Int
        {
            return ContextCompat.getColor(context, color)
        }

        const val POINTS_WIN = 2
        const val POINTS_LOOSE = 1
        const val POINTS_DEFAULT_LOSS = 0
    }
}