package se.assystems.LogicTester;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_BLUETOOTH_PERMISSION = 0,
                      REQUEST_ACCESS_FINE_LOCATION_PERMISSION = 1,
                      REQUEST_BLUETOOTH_ADMIN_PERMISSION = 2,
                      REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 3;

    private BluetoothAdapter btAdapter;
    private LogicTesterHandler ltHandler;
    private Thread testCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request all relevant permissions.
        checkOrRequestPermissions();

        // Retrieve BluetoothAdapter.
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        ltHandler = new LogicTesterHandler(this, new LogicTesterHandler.LogicTesterEventListener() {
            @Override
            public void onInputUpdate(byte data) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateInputCheckboxes(data);
                    }
                });
            }
        });
    }

    /**
     * onClick event for the disconnect button.
     * @param view The view that contains the button.
     */
    public void disconnectButtonClick(View view)
    {
        if(ltHandler.isConnected()){
            ltHandler.disconnect();
        } else {
            Toast.makeText(this, "Already disconnected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * onClick event for the connect button.
     * @param view The view that contains the button.
     */
    public void connectButtonClick(View view)
    {
        // Check if Bluetooth is turned off
        if(!btAdapter.isEnabled()) {
            // Bluetooth is disabled, request to enable Bluetooth.
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startBluetoothServiceLauncher.launch(intent);
        } else {
            //Start device selector.
            launchDeviceSelector();
        }
    }

    public void onRunTestCase(View view) {
        //openFilePicker();
        if(ltHandler.isConnected())
        {
            if(testCase != null) {
                if (testCase.isAlive()) {
                    Toast.makeText(this, "Alive", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            /**testCase = new LogicTesterChecker(this,
                    new byte[] {0x00, 0x01, 0x02, 0x03},
                    new byte[] {0x00, 0x01, 0x01, 0x02},
                    ltHandler);**/
            testCase = new LogicTesterChecker(this,
                    new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07},
                    new byte[] {0x01, 0x02, 0x01, 0x03, 0x01, 0x02, 0x01, 0x00},
                    ltHandler);
            testCase.run();
        } else {
            Toast.makeText(this, "Disconnected, try connecting first", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRetriveInputs(View view){
        if(ltHandler.isConnected()) {
            ltHandler.updateInput();
        } else {
            Toast.makeText(this, "Disconnected, try connecting first", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSendOutputs(View view)
    {
        if(ltHandler.isConnected()){
            CheckBox[] checkBoxes = {
                    findViewById(R.id.output0Box),
                    findViewById(R.id.output1Box),
                    findViewById(R.id.output2Box),
                    findViewById(R.id.output3Box),
                    findViewById(R.id.output4Box),
                    findViewById(R.id.output5Box),
                    findViewById(R.id.output6Box),
                    findViewById(R.id.output7Box),
            };

            int sum = 0;
            for(int i = 0; i < checkBoxes.length; i++)
            {
                if(checkBoxes[i].isChecked())
                {
                    Log.d("LogicTesterTag", "IsChecked" + Integer.toString(i));
                    sum += Math.pow(2,i);
                }
            }

            Log.d("LogicTesterTag", "Sum: " + Integer.toString(sum));

            ltHandler.setOutput((byte)sum);
        } else {
            Toast.makeText(this, "Disconnected, try connecting first", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateInputCheckboxes(byte data)
    {
        CheckBox[] checkBoxes = {
                findViewById(R.id.input0Box),
                findViewById(R.id.input1Box),
                findViewById(R.id.input2Box),
                findViewById(R.id.input3Box),
                findViewById(R.id.input4Box),
                findViewById(R.id.input5Box),
                findViewById(R.id.input6Box),
                findViewById(R.id.input7Box),
        };

        for(int i = 0; i < checkBoxes.length; i++)
        {
            boolean val = (data << ~i < 0);
            String msg = "I: " + i + " Val: " + val;
            Log.d("LogicTesterTag", msg);
            checkBoxes[i].setChecked(val);
        }
    }

    public void openFilePicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("file/*");
        openFilePickerLauncher.launch(intent);
    }

    /**
     * Checks if the needed permissions are granted and request the missing permissions if needed.
     */
    private void checkOrRequestPermissions()
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Request Bluetooth permission
            this.requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request Access Fine Location permission
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Request Bluetooth Admin Location permission
            requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_ADMIN_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    /**
     * Callback for when a permission request returns.
     * @param requestCode The code that indicates what/which request the result is for.
     * @param permissions The permission/permissions that were requested.
     * @param grantResults The result of the request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Run super so nothing breaks.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Switch between the different request code cases.
        switch(requestCode)
        {
            // Bluetooth permission request
            case REQUEST_BLUETOOTH_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // Permission granted.
                } else {
                    // Permission not granted, show toast of why this permission is needed.
                    Toast.makeText(this, "Bluetooth permission is needed to connect to LogicTester", Toast.LENGTH_SHORT).show();
                }
                break;

            // Access Fine Location permission request
            case REQUEST_ACCESS_FINE_LOCATION_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // Permission granted.
                } else {
                    // Permission not granted, show toast of why this permission is needed.
                    Toast.makeText(this, "Locations permission is needed to discover and connect to LogicTester", Toast.LENGTH_SHORT).show();
                }
                break;

            // Access Bluetooth Admin permission request
            case REQUEST_BLUETOOTH_ADMIN_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // Permission granted.
                } else {
                    // Permission not granted, show toast of why this permission is needed.
                    Toast.makeText(this, "Bluetooth permission is needed to pair with the LogicTester", Toast.LENGTH_SHORT).show();
                }
                break;

            //
            case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // Permission granted.
                } else {
                    // Permission not granted, show toast of why this permission is needed.
                    Toast.makeText(this, "Storage permission is needed to run test cases", Toast.LENGTH_SHORT).show();
                }
                break;

            // Default case to handle all codes that don't have cases, should be 0
            default:
                break;
        }
    }

    /**
     * The "callback" for when the app ask for a file selection
     */
    ActivityResultLauncher<Intent> openFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Check the request response
        if(result.getResultCode() == Activity.RESULT_OK) {
            // Get file and launch test case
        } else {
            // Display toast
            Toast.makeText(getApplicationContext(), "No test case selected", Toast.LENGTH_SHORT).show();
        }
    });


    /**
     * The "callback" for when the app ask for enabling Bluetooth.
     */
    ActivityResultLauncher<Intent> startBluetoothServiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Check the request response
        if(result.getResultCode() == Activity.RESULT_OK) {
            // Launch the device selector if Bluetooth got enabled.
            launchDeviceSelector();
        } else {
            // Display toast to why the app needs Bluetooth.
            Toast.makeText(getApplicationContext(), "Bluetooth is needed to connect to the LogicTester", Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Launches the device selector activity.
     */
    public void launchDeviceSelector()
    {
        Intent intent = new Intent(this, DeviceSelectorActivity.class);
        deviceSelectorLauncher.launch(intent);
    }

    /**
     * The "callback" for when the user has selected a Bluetooth device to be connected to.
     */
    ActivityResultLauncher<Intent> deviceSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Check the request response
        if(result.getResultCode() == Activity.RESULT_OK) {
            // Print the selected device
            Toast.makeText(getApplicationContext(), result.getData().getStringExtra("MAC_ADDRESS"), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), result.getData().getStringExtra("DEVICE_NAME"), Toast.LENGTH_SHORT).show();
            ltHandler.connect(btAdapter.getRemoteDevice(result.getData().getStringExtra("MAC_ADDRESS")));
        } else {
            // Display toast to why the app needs Bluetooth.
            Toast.makeText(getApplicationContext(), "No device selected", Toast.LENGTH_SHORT).show();
        }
    });

}