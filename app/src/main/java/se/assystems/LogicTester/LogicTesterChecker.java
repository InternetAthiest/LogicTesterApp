package se.assystems.LogicTester;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class LogicTesterChecker extends Thread{

    private byte recievedData;
    private boolean waitForCMD;
    private Context context;
    LogicTesterHandler handler;
    private byte[] out, expected;

    public LogicTesterChecker(Context context, byte[] out, byte[] expected, LogicTesterHandler handler) {
        this.context = context;
        this.out = out;
        this.expected = expected;
        this.handler = handler;
    }

    @Override
    public void run() {
        int fails = 0;
        waitForCMD = true;
        Object lock = new Object();

        for(int i = 0; i < out.length; i++)
        {
            Log.d("LogicTesterTag", "RunTestCase " + Integer.toString(i));
            waitForCMD = true;
            handler.addCommand(new LogicTesterCommand(LogicTesterCommand.LogicTesterCommandType.SET_OUTPUT, out[i], null));
            handler.addCommand(new LogicTesterCommand(LogicTesterCommand.LogicTesterCommandType.GET_INPUT, (byte) (0), new LogicTesterCommand.LogicTesterCommandEventListener() {
                @Override
                public void onCommandFinished(byte data) {
                    recievedData = data;
                    Log.d("LogicTesterTag", "Setting cmd to false");
                    waitForCMD = false;
                    synchronized (lock){
                        lock.notify();
                    }

                }
            }));
            Log.d("LogicTesterTag", "Waiting for cmd...");
            while(waitForCMD)
            {
                try {
                    synchronized (lock){
                        lock.wait();
                    }
                } catch (Exception e) {
                    Log.d("LogicTestLog", e.getMessage());
                }
            }

            Log.d("LogicTesterTag", "Checking solution...");
            if(recievedData != expected[i])
            {
                fails++;
            }
        }

        if(fails > 0)
        {
            Toast.makeText(context, "Test case failed with " + Integer.toString(fails) + " failures", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(context, "Test case successful!", Toast.LENGTH_SHORT).show();
        }
    }
}
