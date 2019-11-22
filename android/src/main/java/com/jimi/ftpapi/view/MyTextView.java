package com.jimi.ftpapi.view;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.text.ReactTextView;
import com.jimi.ftpapi.R;

import java.util.Map;

public class MyTextView extends SimpleViewManager<TextView> {
    @Override
    public String getName() {
        return "MyTextView";
    }

    @Override
    protected TextView createViewInstance(ThemedReactContext reactContext) {
//        View view=LayoutInflater.from(reactContext).inflate(R.layout.layout_my_textview,null);
//        TextView textView=view.findViewById(R.id.myTextView);
        ReactTextView textView=new ReactTextView(reactContext);

        return textView;
    }

    @Override
    protected void addEventEmitters(final ThemedReactContext reactContext, TextView view) {
        super.addEventEmitters(reactContext, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WritableMap data = Arguments.createMap();
                data.putString("msg", "点击按钮");
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        v.getId(),
                        "nativeClick",
                        data
                );
            }
        });
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put(
                       "nativeClick",
                        MapBuilder.of(
                                "registrationName",
                                "jsClick"))
                .build();

    }

    @ReactProp(name = "text")
    public void setMytextViewText(TextView textView,String text){
        textView.setText(text);
    }


    @ReactProp(name = "text")
    public void setMytextViewSize(TextView textView,int size){
        textView.setTextSize(size);
    }


}
