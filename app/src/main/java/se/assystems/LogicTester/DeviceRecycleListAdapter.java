package se.assystems.LogicTester;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * The list adapter for showing Bluetooth Devices in an RecyclerView list.
 */
public class DeviceRecycleListAdapter extends RecyclerView.Adapter<DeviceRecycleListAdapter.DeviceViewHolder> {

    // The list of devices in the list.
    ArrayList<BluetoothDevice> devices;
    // The parents context
    Context context;
    // Event listener for when a user click on one of the devices
    OnItemClickListener listener;

    /**
     * Creates an instance of DeviceRecycleListAdapter.
     * @param context Parents context.
     * @param devices Devices to be shown.
     * @param listener Event listener for on item clicks.
     */
    public DeviceRecycleListAdapter(Context context, ArrayList<BluetoothDevice> devices, OnItemClickListener listener)
    {
        this.context = context;
        this.devices = devices;
        this.listener = listener;
    }

    /**
     * Gets run when the RecycleView creates the individual items.
     * @param parent The parent ViewGroup.
     * @param viewType The viewType.
     * @return An inflated DeviceViewHolder ready to be shown to the UI.
     */
    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.device_list_card, parent, false);
        return new DeviceViewHolder(view);
    }

    /**
     * Binds the data in the adapter to the items ViewHolder.
     * @param holder The list items ViewHolder.
     * @param position The index of the item
     */
    @Override
    public void onBindViewHolder(DeviceRecycleListAdapter.DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position), this.listener);
    }

    /**
     * The number of items in this adapter
     * @return number of items.
     */
    @Override
    public int getItemCount() {
        return devices.size();
    }

    /**
     * The event listener interface for item clicks.
     */
    public interface OnItemClickListener {
        /**
         * Event that gets triggered when the user clicks on an item.
         * @param device The BluetoothDevice of the clicked item.
         */
        void onItemClick(BluetoothDevice device);
    }

    /**
     * ViewHolder for viewing Bluetooth Devices as an item in a list.
     */
    public class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView macAddressTextView, deviceNameTextView;

        BluetoothDevice device;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            macAddressTextView = itemView.findViewById(R.id.deviceMacTextView);
            deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
        }

        public void bind(BluetoothDevice device, OnItemClickListener listener)
        {
            this.device = device;
            this.macAddressTextView.setText(this.device.getAddress());
            this.deviceNameTextView.setText(this.device.getName());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(device);
                }
            });
        }
    }
}
