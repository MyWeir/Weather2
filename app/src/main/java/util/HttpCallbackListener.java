package util;

/**
 * Created by yls on 2017/6/7.
 */
public interface HttpCallbackListener {
    void onFinish(String s);

    void onError(Exception e);
}
