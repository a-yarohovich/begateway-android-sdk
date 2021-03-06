package com.begateway.mobilepayments.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.begateway.mobilepayments.R;
import com.begateway.mobilepayments.utils.DateValidator;

import java.lang.reflect.Method;


public class ExpirationDateEditText extends ErrorEditText implements TextWatcher, View.OnClickListener {

    private boolean mChangeWasAddition;
    private OnClickListener mClickListener;
    private boolean mUseExpirationDateDialog = false;
    private ExpirationDateDialog mExpirationDateDialog;

    public ExpirationDateEditText(Context context) {
        super(context);
        init();
    }

    public ExpirationDateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpirationDateEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = { new LengthFilter(6) };
        setFilters(filters);
        addTextChangedListener(this);
        setShowKeyboardOnFocus(!mUseExpirationDateDialog);
        setCursorVisible(!mUseExpirationDateDialog);
        super.setOnClickListener(this);
    }

    public void useDialogForExpirationDateEntry(Activity activity, boolean useDialog) {
        mExpirationDateDialog = ExpirationDateDialog.create(activity, this);
        mUseExpirationDateDialog = useDialog;
        setShowKeyboardOnFocus(!mUseExpirationDateDialog);
        setCursorVisible(!mUseExpirationDateDialog);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
    }

    @Override
    public void onClick(View v) {
        if (mUseExpirationDateDialog) {
            closeSoftKeyboard();
            mExpirationDateDialog.show();
        }

        if (mClickListener != null) {
            mClickListener.onClick(v);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (mExpirationDateDialog == null) {
            return;
        }

        if (focused && mUseExpirationDateDialog) {
            closeSoftKeyboard();
            mExpirationDateDialog.show();
        } else if (mUseExpirationDateDialog) {
            mExpirationDateDialog.dismiss();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mExpirationDateDialog != null && mExpirationDateDialog.isShowing()) {
            mExpirationDateDialog.dismiss();
        }
    }

    public String getMonth() {
        String string = getString();
        if (string.length() < 2) {
            return "";
        }
        return getString().substring(0,2);
    }

    public String getYear() {
        String string = getString();
        if (string.length() == 4 || string.length() == 6) {
            return getString().substring(2);
        }
        return "";
    }

    @Override
    public boolean isValid() {
        return isOptional() || DateValidator.isValid(getMonth(), getYear());
    }

    @Override
    public String getErrorMessage() {
        if (TextUtils.isEmpty(getText())) {
            return getContext().getString(R.string.begateway_expiration_required);
        } else {
            return getContext().getString(R.string.begateway_expiration_invalid);
        }
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        mChangeWasAddition = lengthAfter > lengthBefore;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mChangeWasAddition) {
            if (editable.length() == 1 && Character.getNumericValue(editable.charAt(0)) >= 2) {
                prependLeadingZero(editable);
            }
        }

        Object[] paddingSpans = editable.getSpans(0, editable.length(), SlashSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        addDateSlash(editable);

        if (((getSelectionStart() == 4 && !editable.toString().endsWith("20")) || getSelectionStart() == 6)
                && isValid()) {
            focusNextView();
        }
    }

    private void setShowKeyboardOnFocus(boolean showKeyboardOnFocus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setShowSoftInputOnFocus(showKeyboardOnFocus);
        } else {
            try {
                // API 16-21
                final Method method = EditText.class.getMethod("setShowSoftInputOnFocus", boolean.class);
                method.setAccessible(true);
                method.invoke(this, showKeyboardOnFocus);
            } catch (Exception e) {
                try {
                    // API 15
                    final Method method = EditText.class.getMethod("setSoftInputShownOnFocus", boolean.class);
                    method.setAccessible(true);
                    method.invoke(this, showKeyboardOnFocus);
                } catch (Exception e1) {
                    mUseExpirationDateDialog = false;
                }
            }
        }
    }

    private void prependLeadingZero(Editable editable) {
        char firstChar = editable.charAt(0);
        editable.replace(0, 1, "0").append(firstChar);
    }

    private void addDateSlash(Editable editable) {
        final int index = 2;
        final int length = editable.length();
        if (index <= length) {
            editable.setSpan(new SlashSpan(), index - 1, index,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private String getString() {
        Editable editable = getText();
        return editable != null ? editable.toString() : "";
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
}
