package se.assystems.LogicTester;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;

public class DeviceSelectorActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> btDiscoveredList;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selector);

        //Instantiate the discovered list.
        btDiscoveredList = new ArrayList<BluetoothDevice>();

        // Get the android devices BluetoothAdapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Retrieve LocationManager.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Cancel current discovery if it is running
        if(btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        // Register filter for all major bluetooth discovery/scan actions.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISCOVERY_STARTED);
        filter.addAction(ACTION_DISCOVERY_FINISHED);
        filter.addAction(ACTION_FOUND);

        // Register the receiver that will handle all of the actions above.
        registerReceiver(btBroadcastReceiver, filter);

        // Set the RecyclerView list to be a linear list.
        RecyclerView recyclerView = findViewById(R.id.deviceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        updateDeviceRecycleListView();
    }

    /**
     * Gets run when the user clicks the Scan button.
     * @param view
     */
    public void onScanClick(View view){
        // Disable scan if it's already running, like a toggle.
        if(btAdapter.isDiscovering())
        {
            stopScan();
        }
        else
        {
            // Start scan, but make sure that location services are enabled first.
            if (!isLocationServicesEnabled()) {
                // Create an AlertDialog to inform user that location services are needed inorder to scan for bluetooth devices.
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setMessage("Location services are needed to scan for Bluetooth devices, want to enable them?")
                        .setCancelable(false)
                        // Create a positive button which takes the user to the location services settings page in the OS.
                        .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startLocationServiceLauncher.launch(intent);
                                dialog.cancel();
                            }
                        })
                        // Do nothing if the user dosen't want to enable location services
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing...
                                dialog.cancel();
                            }
                        });

                // Show AlertDialog.
                AlertDialog dialog = dialogBuilder.create();
                dialog.setTitle("Enable location services");
                dialog.show();

            } else {
                // Start a scan if location services are enabled.
                startScan();
            }
        }
    }

    public void onCancelClick(View view)
    {
        // Set result to CANCELED since the user didn't select a device.
        setResult(RESULT_CANCELED);
        // Kill activity.
        finish();
    }

    /**
     * Gets run when the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        // Unregister from the broadcast.
        unregisterReceiver(btBroadcastReceiver);

        // Make sure the app has stopped scanning for bluetooth devices since it can inpact bluetooth performance.
        stopScan();

        super.onDestroy();
    }

    /**
     * Stops a scan for Bluetooth devices.
     */
    public void stopScan() {
        btAdapter.cancelDiscovery();
    }

    /**
     * Starts a scan for Bluetooth devices.
     */
    public void startScan() {
        // Clear the list before searching so previously connectable devices aren't included.
        btDiscoveredList.clear();
        // Start discovery/scan.
        btAdapter.startDiscovery();
    }

    /**
     * The "callback" for when the app ask for enabling location services.
     */
    ActivityResultLauncher<Intent> startLocationServiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Check the request response
        if(isLocationServicesEnabled()) {
            // Launch the scan if location services got enabled.
            startScan();
        } else {
            // Display toast to why the app needs location services.
            Toast.makeText(getApplicationContext(), "Location services are needed to discover Bluetooth devices", Toast.LENGTH_SHORT).show();
        }
    });

    /**
     * Check if location services are enabled.
     * @return Status of location services.
     */
    private boolean isLocationServicesEnabled()
    {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Updates the RecycleView to display new information
     */
    private void updateDeviceRecycleListView(){
        // Find the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.deviceRecyclerView);
        // Create a new adapter inorder to refresh the information in the RecyclerView
        DeviceRecycleListAdapter adapter = new DeviceRecycleListAdapter(this, btDiscoveredList, itemClickEventHandler);
        // Set the adapter to be the RecyclerViews adapter effectivly updating it.
        recyclerView.setAdapter(adapter);
    }

    /**
     * The event handler for when the user clicks on one of the list items
     */
    private DeviceRecycleListAdapter.OnItemClickListener itemClickEventHandler = new DeviceRecycleListAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(BluetoothDevice device) {
            // Store the BluetoothDevice data in the data packet
            Intent data = new Intent();
            data.putExtra("MAC_ADDRESS", device.getAddress());
            data.putExtra("DEVICE_NAME", device.getName());

            // Set result to ok since the user selected a device.
            setResult(RESULT_OK,data);

            // Kill this activity since it isn't needed anymore.
            finish();
        }
    };

    /**
     * The broadcast recevier ("Event handler") for bluetooth discovery actions. Does the heavy lifting of actually getting BluetoothDevices.
     */
    private BroadcastReceiver btBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_DISCOVERY_STARTED.equals(action)) {
                //Discovery started, change scan button text.
                Button scanButton = findViewById(R.id.scanButton);
                scanButton.setText("Stop scanning...");

            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Discovery finished, change back scan button text.
                Button scanButton = findViewById(R.id.scanButton);
                scanButton.setText("Scan");

            } else if (ACTION_FOUND.equals(action)) {
                //Bluetooth device found, add it to the list if it's unique and update RecyclerView list.
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!btDiscoveredList.contains(device))
                {
                    // Add device to discovered list
                    btDiscoveredList.add(device);

                    // Update RecylcerView list.
                    updateDeviceRecycleListView();
                }
            }
        }
    };
}
