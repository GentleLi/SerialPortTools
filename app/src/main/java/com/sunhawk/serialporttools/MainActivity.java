package com.sunhawk.serialporttools;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sunhawk.serialporttools.helper.MyFunc;
import com.sunhawk.serialporttools.helper.SerialHelper;
import com.sunhawk.serialporttools.model.ComBean;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private AppCompatButton btnRead;
    private DispQueueThread DispQueue;
    private SerialControl ComA;

    private boolean isHex = true;
    private AppCompatTextView tvDatas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tvDatas = findViewById(R.id.sample_text);
        tvDatas.setText(stringFromJNI());
        btnRead = findViewById(R.id.btn_read);
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        initListener();
    }

    private void initListener() {
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenComPort(ComA);
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    //----------------------------------------------------串口控制类
    private class SerialControl extends SerialHelper {

        public SerialControl(String sPort, String sBaudRate) {
            super(sPort, sBaudRate);
        }

        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            //数据接收量大或接收时弹出软键盘，界面会卡顿,可能和6410的显示性能有关
            //直接刷新显示，接收数据量大时，卡顿明显，但接收与显示同步。
            //用线程定时刷新显示可以获得较流畅的显示效果，但是接收数据速度快于显示速度时，显示会滞后。
            //最终效果差不多-_-，线程定时刷新稍好一些。
            DispQueue.AddQueue(ComRecData);//线程定时刷新显示(推荐)
			/*
			runOnUiThread(new Runnable()//直接刷新显示
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
        }
    }


    //----------------------------------------------------刷新显示线程
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
                        }
                    });
                    try {
                        Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }


    //----------------------------------------------------显示接收数据
    private void DispRecData(ComBean ComRecData) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(ComRecData.sRecTime);
        sMsg.append("[");
        sMsg.append(ComRecData.sComPort);
        sMsg.append("]");


        if (isHex) {
            sMsg.append("[Hex] ");
            sMsg.append(MyFunc.ByteArrToHex(ComRecData.bRec));
        } else {
            sMsg.append("[Txt] ");
            sMsg.append(new String(ComRecData.bRec));
        }
        sMsg.append("\r\n");
        tvDatas.setText(sMsg);

//        editTextRecDisp.append(sMsg);
//        iRecLines++;
//        editTextLines.setText(String.valueOf(iRecLines));
//        if ((iRecLines > 500) && (checkBoxAutoClear.isChecked()))//达到500项自动清除
//        {
//            editTextRecDisp.setText("");
//            editTextLines.setText("0");
//            iRecLines = 0;
//        }
    }

    //----------------------------------------------------打开串口
    private void OpenComPort(SerialHelper ComPort) {
        try {
            ComPort.open();
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, "打开串口失败:没有串口读/写权限!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "打开串口失败:未知错误!", Toast.LENGTH_SHORT).show();
        } catch (InvalidParameterException e) {
            Toast.makeText(MainActivity.this, "打开串口失败:参数错误!", Toast.LENGTH_SHORT).show();
        }
    }


}
