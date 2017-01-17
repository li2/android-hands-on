package me.li2.android.photogallery.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.ui.basic.VisibleFragment;

public class PhotoPageFragment extends VisibleFragment {

    private String mUrl;
    private WebView mWebView;    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        
        mUrl = getActivity().getIntent().getData().toString();
    }
    
    @SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface" })
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);
        
        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        progressBar.setMax(100); // WebChromeClient reports in range 0-100.
        final TextView titleTextView = (TextView) v.findViewById(R.id.titleTextView);
        
        mWebView = (WebView) v.findViewById(R.id.webView);
        
        // getSettings()获取WebSettings实例，它是修改WebView配置的三种途径之一；
        // 启用JavaScript，因为Flickr网站需要；但担心跨网站的脚本攻击，所以提示警告；加注解取消警告。
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        // WebViewClient是一个响应各种渲染事件的接口。比如，
        // 可检测渲染器何时从特定URL加载图片，或决定是否需要向服务器重新提交Post请求。
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 返回true意味着离开当前WebView，不处理这个Url；返回false将由WebView加载Url.
                return false;
            }
        });
        
        // WebChromeClient是一个响应改变浏览器中装饰元素的事件接口，包括
        // JavaScript警告信息、网页图标、进度条、当前网页标题。
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                    titleTextView.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(progress);                    
                }
            }
            
            @Override
            public void onReceivedTitle(WebView view, String title) {
                titleTextView.setText(title);
            }
        });
        
        // 通过注入任意JavaScript对象到WebView本身包含的文档中，还可以做更多事情。
        mWebView.addJavascriptInterface(new Object() {
            @SuppressWarnings("unused")
            public void send(String message) {
                Log.d(TAG, "Received message: " + message);
            }
        }, "androidObject");
        
        mWebView.loadUrl(mUrl);
        
        return v;
    }
}
