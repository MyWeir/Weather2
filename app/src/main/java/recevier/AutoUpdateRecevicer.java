package recevier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import service.AutoUpdateService;

/**
 * Created by yls on 2017/6/8.
 */

public class AutoUpdateRecevicer extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i=new Intent(context, AutoUpdateService.class);
        context.startService(i);
    }
}
