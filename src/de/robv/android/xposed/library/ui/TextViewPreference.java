package de.robv.android.xposed.library.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextViewPreference extends Preference {
	private TextView textView = null;
	private int padding = 7;
	private int textSize = -1;
	private boolean bold = false;
	private boolean italic = false;
	
    public TextViewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
		if (attrs != null) {
			textSize = attrs.getAttributeIntValue(null, "textSize", textSize);
			padding = attrs.getAttributeIntValue(null, "padding", padding);
			bold = attrs.getAttributeBooleanValue(null, "bold", bold);
			italic = attrs.getAttributeBooleanValue(null, "italic", italic);
		}
    }

    public TextViewPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public TextViewPreference(Context context) {
        this(context, null);
    }

	@Override
	protected View onCreateView(ViewGroup parent) {
		return getTextView();
	}
	
	public TextView getTextView() {
		if (textView == null) {
			textView = new TextView(getContext());
			textView.setId(android.R.id.title);
			textView.setPadding(padding,padding,padding,padding);
			
			if (textSize > 0)
				textView.setTextSize(textSize);
			
			if (bold && italic)
				textView.setTypeface(null, Typeface.BOLD_ITALIC);
			else if (bold)
				textView.setTypeface(null, Typeface.BOLD);
			else if (italic)
				textView.setTypeface(null, Typeface.ITALIC);
		}
		return textView;
	}
}
