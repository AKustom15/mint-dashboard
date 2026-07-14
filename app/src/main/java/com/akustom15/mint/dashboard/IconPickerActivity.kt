package com.akustom15.mint.dashboard

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.GridView
import android.widget.BaseAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class IconPickerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resources = packageManager.getResourcesForApplication(packageName)
        val drawableFields = getIconDrawableNames()

        if (drawableFields.isEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val gridView = GridView(this).apply {
            numColumns = 4
            horizontalSpacing = 8
            verticalSpacing = 8
            setPadding(16, 16, 16, 16)
        }

        gridView.adapter = object : BaseAdapter() {
            override fun getCount() = drawableFields.size
            override fun getItem(position: Int) = drawableFields[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val imageView = (convertView as? ImageView) ?: ImageView(this@IconPickerActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(144, 144)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(8, 8, 8, 8)
                }
                val resId = resources.getIdentifier(drawableFields[position], "drawable", packageName)
                if (resId != 0) {
                    imageView.setImageResource(resId)
                }
                return imageView
            }
        }

        gridView.setOnItemClickListener { _, _, position, _ ->
            val iconName = drawableFields[position]
            val resId = resources.getIdentifier(iconName, "drawable", packageName)
            if (resId != 0) {
                val resultIntent = Intent()
                resultIntent.putExtra("icon", resId)
                resultIntent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE",
                    Intent.ShortcutIconResource.fromContext(this@IconPickerActivity, resId))
                resultIntent.data = android.net.Uri.parse("android.resource://$packageName/$resId")
                setResult(RESULT_OK, resultIntent)
            } else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }

        setContentView(gridView)
    }

    private fun getIconDrawableNames(): List<String> {
        val iconNames = mutableListOf<String>()
        try {
            val parser = resources.getXml(
                resources.getIdentifier("drawable", "xml", packageName)
            )
            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "item") {
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "drawable") {
                            parser.getAttributeValue(i)?.let { iconNames.add(it) }
                        }
                    }
                }
                eventType = parser.next()
            }
            parser.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return iconNames
    }
}
