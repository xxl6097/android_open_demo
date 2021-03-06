package com.het.sdk.demo.ui.activity.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.het.basic.data.http.okhttp.OkHttpManager;
import com.het.basic.utils.GsonUtil;
import com.het.basic.utils.StringUtils;
import com.het.basic.utils.ToastUtil;
import com.het.open.lib.api.HetThirdCloudAuthApi;
import com.het.open.lib.callback.IHetCallback;
import com.het.sdk.demo.R;
import com.het.sdk.demo.base.BaseHetActivity;
import com.het.sdk.demo.utils.UIJsonConfig;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.het.sdk.demo.R.id.ev_input_code;


/**
 * Created by mindray on 2017/10/31.
 */

public class ThirdAuthActivity extends BaseHetActivity {

    @Bind(R.id.btn_first)
    Button btnFirst;
    @Bind(R.id.ev_input_phone)
    EditText evInputPhone;
    @Bind(R.id.btn_second)
    Button btnSecond;
    @Bind(ev_input_code)
    EditText evInputCode;
    @Bind(R.id.btn_third)
    Button btnThird;

    private AuthorizationCode authorizationCode;
    private String phoneNumber="";
    private String timestamp;

    private String openId="";

    private Result result;
    private String str="";

    @Bind(R.id.tv_show_code)
    TextView tvShowCode;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_third_auth;
    }

    @Override
    protected void initTopBarView() {
        mTitleView.setTitle(R.string.code_auth);
        mTitleView.setUpNavigateMode();
        mTitleView.setBackground(UIJsonConfig.getInstance(mContext).setNavBackground_color());
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView(Bundle savedInstanceState) {

    }
    //

    @OnClick({R.id.btn_first,R.id.btn_second,R.id.btn_third})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_first:
                phoneNumber = evInputPhone.getText().toString();
                str="";
                //如果openId等于空，认为是第一次授权，然后判断手机号码是否为空
                if(StringUtils.isNull(openId) && StringUtils.isNull(phoneNumber)){
                    ToastUtil.showShortToast(mContext, "账号和openId不能为空");
                    return;
                }
                HetThirdCloudAuthApi.getInstance().getAuthorizationCode(new IHetCallback() {
                    @Override
                    public void onSuccess(int code, String msg) {
                        if(code ==0){
                            authorizationCode = GsonUtil.getInstance().toObject(msg,AuthorizationCode.class);
                            str+=authorizationCode.getAuthorizationCode()+"\n";
                            tvShowCode.setText(str);
                        }else{
                            ToastUtil.showShortToast(mContext,msg);
                        }
                    }

                    @Override
                    public void onFailed(int code, String msg) {
                        ToastUtil.showShortToast(mContext,msg);
                    }
                },phoneNumber,openId);

                break;
            case R.id.btn_second:
                if(authorizationCode==null){
                    ToastUtil.showShortToast(mContext, "请先获取授权码");
                    return;
                }
                phoneNumber = evInputPhone.getText().toString();

                if(StringUtils.isNull(phoneNumber)){
                    ToastUtil.showShortToast(mContext, "手机号码不能为空");
                    return;
                }

                long time=System.currentTimeMillis();
                timestamp=String.valueOf(time);

                executeHttpRequest();

                break;
            case R.id.btn_third:

                if(result== null){
                    ToastUtil.showShortToast(mContext, "随机码不能为空");
                    return;
                }
                String verificationCode = evInputCode.getText().toString();

                if(StringUtils.isNull(verificationCode)){
                    ToastUtil.showShortToast(mContext, "验证码为空，请先获取验证码");
                    return;
                }

                HetThirdCloudAuthApi.getInstance().checkRandomCode(new IHetCallback() {
                    @Override
                    public void onSuccess(int code, String s) {
                        if(code==0) {
                            ToastUtil.showShortToast(mContext, "授权成功");
                            openId = s;
                            str+=openId+"\n";
                            tvShowCode.setText(str);
                        }
                    }

                    @Override
                    public void onFailed(int code, String msg) {

                        ToastUtil.showShortToast(mContext,"授权失败");
                    }
                },verificationCode,result.getData().getRandomCode());

                break;
        }
    }

    class AuthorizationCode{

        private String authorizationCode;
        private String state;

        public String getAuthorizationCode() {
            return authorizationCode;
        }

        public void setAuthorizationCode(String authorizationCode) {
            this.authorizationCode = authorizationCode;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static final String BASE_URL = "http://10.8.4.220:8100";


    interface RandomCodeService{

        @GET("/v1/cloud/auth")
        Observable<String> getRandomCode(@Query("appId") String appId, @Query("appSecret")String appSecret,@Query("authorizationCode") String authorizationCode,
                                              @Query("account") String account,@Query("timestamp") String timestamp);
    }

    class Result{
        private String code;
        private Data data;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "code='" + code + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
    class Data{
        private String randomCode;

        public String getRandomCode() {
            return randomCode;
        }

        public void setRandomCode(String randomCode) {
            this.randomCode = randomCode;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "randomCode='" + randomCode + '\'' +
                    '}';
        }
    }

    private void executeHttpRequest(){

        Retrofit retrofit = new Retrofit.Builder()
                .client(OkHttpManager.getClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build();

        RandomCodeService randomCodeService =  retrofit.create(RandomCodeService.class);
        String s= "";
        if(!StringUtils.isNull(openId)){
            s = openId;
        }else
        randomCodeService.getRandomCode(UIJsonConfig.getInstance(this).getAppId(),UIJsonConfig.getInstance(this).getAppSecret()
                ,authorizationCode.getAuthorizationCode(),s,timestamp)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }
                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.showShortToast(mContext,"获取随机码失败");
                    }
                    @Override
                    public void onNext(String randomCode) {
                        result = GsonUtil.getInstance().toObject(randomCode,Result.class);
                        if(result!=null) {
                            str+=result.getData().getRandomCode()+"\n";
                            tvShowCode.setText(str);
                        }
                    }
                });


    }
}
