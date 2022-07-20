package com.example.keydisplayunit;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView receiveText;
    private TextView txtFreqA;
    private TextView txtFreqB;
    private ControlLines controlLines;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    byte[] readBuffer;
    private ByteArrayOutputStream outputStream;

    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = Constants.BAUDRATE;
        withIoManager = getArguments().getBoolean("withIoManager");
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));

        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
    }

    @Override
    public void onPause() {
        if(connected) {
            status("disconnected");
            disconnect();
        }
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        txtFreqA = view.findViewById(R.id.txtFreqA);
        txtFreqB = view.findViewById(R.id.txtFreqB);

        // Single press of the buttons
        View changeRadioBtn = view.findViewById(R.id.btnChangeRadioChan);
        View volIncreaseBtn = view.findViewById(R.id.btnVolumeIncrease);
        View volDecreaseBtn = view.findViewById(R.id.btnVolumeDecrease);
        View pttBtn = view.findViewById(R.id.btnPTT);

        changeRadioBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageSWITCHMEMORYSLOT));
        volIncreaseBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageVOLUP));
        volDecreaseBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageVOLDOWN));
        pttBtn.setOnClickListener(v -> sendKeyPress(Constants.MessagePTT));

        View kduZeroBtn     = view.findViewById(R.id.btnKDUZero);
        View kduOneBtn      = view.findViewById(R.id.btnKDUOne);
        View kduTwoBtn      = view.findViewById(R.id.btnKDUTwo);
        View kduThreeBtn    = view.findViewById(R.id.btnKDUThree);
        View kduFourBtn     = view.findViewById(R.id.btnKDUFour);
        View kduFiveBtn     = view.findViewById(R.id.btnKDUFive);
        View kduSixBtn      = view.findViewById(R.id.btnKDUSix);
        View kduSevenBtn    = view.findViewById(R.id.btnKDUSeven);
        View kduEightBtn    = view.findViewById(R.id.btnKDUEight);
        View kduNineBtn     = view.findViewById(R.id.btnKDUNine);

        kduZeroBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageZERO));
        kduOneBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageONE));
        kduTwoBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageTWO));
        kduThreeBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageTHREE));
        kduFourBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageFOUR));
        kduFiveBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageFIVE));
        kduSixBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageSIX));
        kduSevenBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageSEVEN));
        kduEightBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageEIGHT));
        kduNineBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageNINE));

        View kduArrowLeft   = view.findViewById(R.id.btnKDULeftArrow);
        View kduArrowRight  = view.findViewById(R.id.btnKDURightArrow);

        kduArrowLeft.setOnClickListener(v -> sendKeyPress(Constants.MessageARROWLEFT));
        kduArrowRight.setOnClickListener(v -> sendKeyPress(Constants.MessageARROWRIGHT));

        View kduClrBtn      = view.findViewById(R.id.btnKDUCLR);
        View kduEntBtn      = view.findViewById(R.id.btnKDUENT);
        View kduPrePos      = view.findViewById(R.id.btnKDUPRePositive);
        View kduPreNeg      = view.findViewById(R.id.btnKDUPReNegative);

        kduClrBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageCLR));
        kduEntBtn.setOnClickListener(v -> sendKeyPress(Constants.MessageENT));
        kduPrePos.setOnClickListener(v -> sendKeyPress(Constants.MessagePREUP));
        kduPreNeg.setOnClickListener(v -> sendKeyPress(Constants.MessagePREDOWN));

        // Press and hold

        kduClrBtn.setOnLongClickListener(v -> {
            long start  = System.currentTimeMillis();
            long end    = start + 1500;
            while (System.currentTimeMillis() < end) {
                sendKeyPress(Constants.MessageCLR);
            }
            return true;
        });

        controlLines = new ControlLines(view);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> {
            try {
                continuousMessage(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            status("connection lost: " + e.getMessage());
            disconnect();
        });
    }

    /*
     * Serial + UI
     */
    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
            controlLines.start(); //TODO: Are controllines necessary?
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        controlLines.stop();
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void sendKeyPress(byte[] message){
        if(!connected) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = message;
            SpannableStringBuilder spn = new SpannableStringBuilder();
            spn.append("send " + data.length + " bytes\n");
            spn.append(HexDump.dumpHexString(data)).append("\n");
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    private void continuousMessage(byte[] data) throws IOException {
        //SpannableStringBuilder spn = new SpannableStringBuilder();
        //spn.append("receive " + data.length + " bytes\n");

        if(data.length > 0) {

            outputStream = new ByteArrayOutputStream();
            if(readBuffer != null && readBuffer.length > 0) {
                outputStream.write(readBuffer);
            }
            outputStream.write(data);

            readBuffer = outputStream.toByteArray();
            //spn.append("receive " + readBuffer.length + " bytes\n");
            if (readBuffer.length > 96){ // Got enough data to stitch a config up
                int startPos = 0;
                for(byte b : readBuffer)
                {
                    if (b == -69 && (startPos+32) < readBuffer.length){ // -69 Is the config start Control Character
                        txtFreqA.setText("Channel A: " + HexDump.fromHexToString(readBuffer, startPos+1, 9));
                        txtFreqB.setText("Channel B: " + HexDump.fromHexToString(readBuffer, startPos+11, 9));
                        break;
                    }
                    startPos += 1;
                }


                readBuffer = new byte[0];//Empty the readBuffer
            }
            //spn.append(HexDump.dumpHexString(data)).append("\n");
        }
        //receiveText.append(spn); // TODO HERE
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    class ControlLines {
        private static final int refreshInterval = 200; // msec

        private final Runnable runnable;
        private final ToggleButton rtsBtn, ctsBtn, dtrBtn, dsrBtn, cdBtn, riBtn;

        ControlLines(View view) {
            runnable = this::run; // w/o explicit Runnable, a new lambda would be created on each postDelayed, which would not be found again by removeCallbacks

            rtsBtn = view.findViewById(R.id.controlLineRts);
            ctsBtn = view.findViewById(R.id.controlLineCts);
            dtrBtn = view.findViewById(R.id.controlLineDtr);
            dsrBtn = view.findViewById(R.id.controlLineDsr);
            cdBtn = view.findViewById(R.id.controlLineCd);
            riBtn = view.findViewById(R.id.controlLineRi);
            rtsBtn.setOnClickListener(this::toggle);
            dtrBtn.setOnClickListener(this::toggle);
        }

        private void toggle(View v) {
            ToggleButton btn = (ToggleButton) v;
            if (!connected) {
                btn.setChecked(!btn.isChecked());
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            String ctrl = "";
            try {
                if (btn.equals(rtsBtn)) { ctrl = "RTS"; usbSerialPort.setRTS(btn.isChecked()); }
                if (btn.equals(dtrBtn)) { ctrl = "DTR"; usbSerialPort.setDTR(btn.isChecked()); }
            } catch (IOException e) {
                status("set" + ctrl + "() failed: " + e.getMessage());
            }
        }

        private void run() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getControlLines();
                rtsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RTS));
                ctsBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CTS));
                dtrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DTR));
                dsrBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.DSR));
                cdBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.CD));
                riBtn.setChecked(controlLines.contains(UsbSerialPort.ControlLine.RI));
                mainLooper.postDelayed(runnable, refreshInterval);
            } catch (IOException e) {
                status("getControlLines() failed: " + e.getMessage() + " -> stopped control line refresh");
            }
        }

        void start() {
            if (!connected)
                return;
            try {
                EnumSet<UsbSerialPort.ControlLine> controlLines = usbSerialPort.getSupportedControlLines();
                if (!controlLines.contains(UsbSerialPort.ControlLine.RTS)) rtsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CTS)) ctsBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DTR)) dtrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.DSR)) dsrBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.CD))   cdBtn.setVisibility(View.INVISIBLE);
                if (!controlLines.contains(UsbSerialPort.ControlLine.RI))   riBtn.setVisibility(View.INVISIBLE);
                run();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "getSupportedControlLines() failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stop() {
            mainLooper.removeCallbacks(runnable);
            rtsBtn.setChecked(false);
            ctsBtn.setChecked(false);
            dtrBtn.setChecked(false);
            dsrBtn.setChecked(false);
            cdBtn.setChecked(false);
            riBtn.setChecked(false);
        }
    }
}
