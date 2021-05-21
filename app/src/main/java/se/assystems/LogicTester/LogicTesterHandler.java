package se.assystems.LogicTester;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class LogicTesterHandler {

    private LogicTesterEventListener listener;
    private CommandProcessor commandProcessor;
    private Thread commandProcessorThread;
    private Context parentContext;
    private ArrayList<LogicTesterCommand> commandQueue;

    public LogicTesterHandler(Context context, LogicTesterEventListener listener)
    {
        this.parentContext = context;
        this.listener = listener;
        this.commandQueue = new ArrayList<>();
        this.commandProcessor = null;
        this.commandProcessorThread = null;
    }

    public boolean isConnected() {
        if(this.commandProcessorThread != null)
        {
            if(this.commandProcessorThread.isAlive())
            {
                return true;
            }
        }
        return false;
    }

    public void addCommand(LogicTesterCommand cmd) {
        this.commandQueue.add(cmd);
    }

    public void setOutput(byte outputValue) {
        if (this.commandProcessorThread.isAlive()) {
            this.commandQueue.add(new LogicTesterCommand(LogicTesterCommand.LogicTesterCommandType.SET_OUTPUT, outputValue, null));
        }
    }

    public void updateInput() {
        if(this.commandProcessorThread.isAlive()) {
            this.commandQueue.add(new LogicTesterCommand(LogicTesterCommand.LogicTesterCommandType.GET_INPUT, (byte)(0), new LogicTesterCommand.LogicTesterCommandEventListener() {
                @Override
                public void onCommandFinished(byte data) {
                    listener.onInputUpdate(data);
                }
            }));
        }
    }

    public void connect(BluetoothDevice device){
        if(this.commandProcessorThread != null) {
            if (this.commandProcessorThread.isAlive()) {
                this.commandProcessor.Shutdown();
                try {
                    this.commandProcessorThread.join();
                } catch (Exception e) {
                    Log.d("LogicTestLog", e.getMessage());
                }
            }
        }
        this.commandProcessor = new CommandProcessor(device);
        this.commandProcessorThread = new Thread(this.commandProcessor);
        this.commandProcessorThread.start();
        Log.d("LogicTesterTag", "trying to start thread");
    }

    public void disconnect(){
        if(this.commandProcessorThread.isAlive())
        {
            this.commandProcessor.Shutdown();
        }
    }

    public interface LogicTesterEventListener{
        public void onInputUpdate(byte data);
    }

    class CommandProcessor implements Runnable {

        private boolean shutdown;
        public BluetoothDevice targetDevice;
        public BluetoothSocket socket;
        public InputStream btInputStream;
        public OutputStream btOutputStream;

        public CommandProcessor(BluetoothDevice targetDevice) {
            this.targetDevice = targetDevice;
            this.socket = null;
            this.btInputStream = null;
            this.btOutputStream = null;
        }

        public void Shutdown(){
            this.shutdown = true;
        }

        @Override
        public void run() {
            // Reset control variable
            shutdown = false;

            // Reset command queue
            commandQueue = new ArrayList<LogicTesterCommand>();

            // Try to connect to targetDevice.
            try{
                socket = targetDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket.connect();

                // Retrieve streams from socket when we get connected.
                btInputStream = socket.getInputStream();
                btOutputStream = socket.getOutputStream();
            } catch (Exception e) {
                // Either an actual error or user didn't press allow on pairing request.
                Log.d("LogicTestLog", e.getMessage());
                try
                {
                    // Make sure to close the socket.
                    btInputStream.close();
                    btOutputStream.close();
                    socket.close();
                }
                catch (Exception e2)
                {
                    Log.d("LogicTestLog", e.getMessage());
                }
                // And stop the thread.
                return;
            }


            Log.d("LogicTesterTag", "Connected");
            // Run command processing in here
            while(!shutdown)
            {
                try {
                    // Command processing runs in here.

                    // Run command if there is any in the queue.
                    if(commandQueue.size() > 0)
                    {
                        Log.d("LogicTesterTag", "LOOOP");
                        switch(commandQueue.get(0).commandType){
                            case GET_INPUT:
                                // Send GET_INPUT command to device.
                                btOutputStream.write('G');
                                btOutputStream.flush();

                                // Wait for return.
                                while(!(btInputStream.available() > 0)){

                                }

                                // Read single bit of data
                                byte data = (byte) btInputStream.read();
                                Log.d("LogicTesterTag", "Read input...");
                                // Call callback event.
                                commandQueue.get(0).listener.onCommandFinished(data);

                                Log.d("LogicTesterTag", "Called event");
                                // Command finished, delete it from the queue.
                                commandQueue.remove(0);
                                break;

                            case SET_OUTPUT:
                                Log.d("LogicTesterTag", "LOOOP SET");
                                btOutputStream.write('S');
                                btOutputStream.write(commandQueue.get(0).sendByte);
                                btOutputStream.flush();
                                // Command finished, delete it from the queue.
                                commandQueue.remove(0);
                                break;

                            default:
                                break;
                        }
                    }

                    if(btInputStream.available() > 0) {
                        Log.d("LogicTesterTag", "BLE EmptyInStream");
                        while (btInputStream.available() > 0) {
                            btInputStream.read();
                        }
                    }
                } catch (Exception e) {
                    Log.d("LogicTestLog", e.getMessage());
                }

            }

            // Close the connection before ending Thread.
            try{
                btInputStream.close();
                btOutputStream.close();
                socket.close();
            } catch (Exception e){
                Log.d("LogicTestLog", e.getMessage());
            }

        }
    }

}
