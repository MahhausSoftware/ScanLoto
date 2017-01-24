package com.mahhaus.scanloto;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by josias on 18/01/17.
 */

public class Util {
    public static void sendMessage(Context context, String Text){
        Toast.makeText(context, Text, Toast.LENGTH_LONG).show();
    }
}
