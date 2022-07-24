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
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;

    private TextView txtFreqA;
    private TextView txtFreqB;

    private TextView txtChannelA;
    private TextView txtChannelB;

    private TextView txtSignalStrengthA;
    private TextView txtSignalStrengthB;
    private ProgressBar barSignalStrengthA;
    private ProgressBar barSignalStrengthB;

    private TextView txtRxTx;
    private TextView txtBatVol;
    private ProgressBar barVolumeBattery;
    private TextView txtVolumeCtrl;
    private TextView txtMicType;
    private TextView txtCTActive;
    private TextView txtRCTC;
    private TextView txtESActive;
    private TextView txtAMActive;
    private TextView txtPTActive;

    private TextView txtTypeFreqShift;
    private TextView txtTRFRPT;
    private TextView txtPower;
    private TextView txtSQLLevel;
    private TextView txtLock;

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

        txtFreqA = view.findViewById(R.id.txtFreqA);
        txtFreqB = view.findViewById(R.id.txtFreqB);

        txtChannelA = view.findViewById(R.id.txtChannelA);
        txtChannelB = view.findViewById(R.id.txtChannelB);

        txtSignalStrengthA =  view.findViewById(R.id.txtSignalStrengthA);
        txtSignalStrengthB = view.findViewById(R.id.txtSignalStrengthB);
        barSignalStrengthA = view.findViewById(R.id.barSignalStrengthA);
        barSignalStrengthB = view.findViewById(R.id.barSignalStrengthB);

        txtRxTx         = view.findViewById(R.id.txtRxTx);
        txtVolumeCtrl   = view.findViewById(R.id.txtVolumeCtrl);
        txtBatVol       = view.findViewById(R.id.txtBatVol);
        barVolumeBattery = view.findViewById(R.id.barVolumeBattery);
        txtMicType      = view.findViewById(R.id.txtMicType);
        txtCTActive     = view.findViewById(R.id.txtCTActive);
        txtRCTC         = view.findViewById(R.id.txtRCTC);
        txtESActive     = view.findViewById(R.id.txtESActive);
        txtAMActive     = view.findViewById(R.id.txtAMActive);
        txtPTActive     = view.findViewById(R.id.txtPTActive);

        txtTypeFreqShift    = view.findViewById(R.id.txtTypeFreqShift);
        txtTRFRPT           = view.findViewById(R.id.txtTRFRPT);
        txtPower            = view.findViewById(R.id.txtPower);
        txtSQLLevel         = view.findViewById(R.id.txtSQLLevel);
        txtLock             = view.findViewById(R.id.txtLock);

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

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId(); //TODO add shortcut buttons
        if (id == R.id.change_freq_mode){
            sendKeyPress(Constants.MessageENT);
            SystemClock.sleep(300);
            sendKeyPress(Constants.MessageONE);
            return true;
        } else if (id == R.id.screen_light){
            sendKeyPress(Constants.MessageENT);
            SystemClock.sleep(300);
            sendKeyPress(Constants.MessageTWO);
            return true;
        } else if (id == R.id.lock){
            long start  = System.currentTimeMillis();
            long end    = start + 1500;
            while (System.currentTimeMillis() < end) {
                sendKeyPress(Constants.MessageCLR);
            }
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
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
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
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    private int bitSet(int n, int k) {
        // if it results to '1' then bit is set,
        // else it results to '0' bit is unset
        return ((n >> k) & 1);
    }

    private void setTopRxTx(int option){
        String RxTx = "";
        switch (option){
            case 64: //0100 0000
                RxTx = " R ";
                break;
            case 96: //0110 0000
                RxTx = " T ";
                break;
            default:
                RxTx = " - ";
        }
        txtRxTx.setText(RxTx);
    }

    private void setTopBatBar(int option){
        txtBatVol.setText("BAT");
        int batteryLevel = 0;
        switch (option){
            case 1: // 0000 0001
                batteryLevel = 0;
                break;
            case 2: // 0000 0010
                batteryLevel = 20;
                break;
            case 3: // 0000 0011
                batteryLevel = 40;
                break;
            case 4: // 0000 0100
                batteryLevel = 60;
                break;
            case 5: // 0000 0101
                batteryLevel = 80;
                break;
            case 6: // 0000 0110
                batteryLevel = 100;
                break;
        }
        barVolumeBattery.setProgress(batteryLevel);
    }

    private void setTopVolBar(int option){
        txtBatVol.setText("VOL");
        int volume = 0;
        switch (option) {
            case 4:         // Lowest 0: 0000 0100
                volume = 0;
                break;
            case 8:         // 0000 1000
                volume = 5;
                break;
            case 12:        // 0000 1100
                volume = 10;
                break;
            case 16:        // 0001 0000
                volume = 15;
                break;
            case 20:        // 0001 0100
                volume = 20;
                break;
            case 24:        // 0001 1000
                volume = 25;
                break;
            case 28:        // 0001 1100
                volume = 30;
                break;
            case 32:        // 0010 0000
                volume = 35;
                break;
            case 36:        // 0010 0100
                volume = 40;
                break;
            case 40:        // 0010 1000
                volume = 45;
                break;
            case 44:        // 0010 1100
                volume = 50;
                break;
            case 48:        // 0011 0000
                volume = 55;
                break;
            case 52:        // 0011 0100
                volume = 60;
                break;
            case 56:        // 0011 1000
                volume = 65;
                break;
            case 60:        // 0011 1100
                volume = 70;
                break;
            case 64:        // 0100 0000
                volume = 75;
                break;
            case 68:        // 0100 0100
                volume = 80;
                break;
            case 72:        // 0100 1000
                volume = 85;
                break;
            case 76:        // 0100 1100
                volume = 90;
                break;
            case 80:        // 0101 0000
                volume = 95;
                break;
            case 84:        // Higest: 0101 0100
                volume = 100;
                break;
            }
        barVolumeBattery.setProgress(volume);
    }

    private void setTopVolumeControl(int option){
        String control = "ELECT";
        if (option == 1){
            control = "VULOS";
        }
        txtVolumeCtrl.setText(control);
    }

    private void setTopMicType(int option){
        String mic = "MOI";
        if (option == 1){
            mic = "DYN";
        }
        txtMicType.setText(mic);
    }

    private void setTopCT(int option){
        String ct = "- -";
        if (option == 1){
            ct = "CT";
        }
        txtCTActive.setText(ct);
    }

    private void setTopRCTC(int option){
        String rctc = "-";
        if (option == 1)
            rctc = "T";
        txtRCTC.setText(rctc);
    }

    private void setTopES(int option){
        String es = "- -";
        if (option == 1) {
            es = "ES";
        }
        txtESActive.setText(es);
    }

    private void setTopAM(int option){
        if(option == 1)
            txtAMActive.setVisibility(View.VISIBLE);
        else
            txtAMActive.setVisibility(View.INVISIBLE);
    }

    private void setTopPT(int options){
        String pt = "- -";
        if (options == 1)
            pt = "PT";
        txtPTActive.setText(pt);
    }

    private void setRowTop(byte[] data){
        //Rx Tx Symbol
        setTopRxTx((data[25] & 96)); // AND operation against 0110 0000

        if (bitSet(data[25],4) == 0) // if display batterybar bit is 0, show volume
            setTopVolBar((data[26] & 124)); // AND againt 0111 1100
        else
            setTopBatBar((data[25] & 15)); // AND against 0000 1111

        setTopVolumeControl(bitSet(data[26], 1)); // Check if bit is set
        setTopMicType(bitSet(data[27], 5)); //Check if 5th Bit is set
        setTopCT(bitSet(data[27], 3));
        setTopRCTC(bitSet(data[27], 1));
        setTopES(bitSet(data[28], 5));
        setTopAM(bitSet(data[28], 3));
        setTopPT(bitSet(data[28], 1));
    }

    private void setBottomFreqShift(int option){
        String type = "TYPE";
        switch (option){
            case 32:    //0010 0000
                type = "TYPE+";
                break;
            case 48:    //0011 0000
                type = "TYPE-";
                break;
        }
        txtTypeFreqShift.setText(type);
    }

    private void setBottomTRFRPT(int option){
        String trf = "TRF";
        if (option == 1)
            trf = "RPT";
        txtTRFRPT.setText(trf);
    }

    private void setBottomPower(int option){
        String power = "LOW";
        switch (option){
            case 2:
                power = "MOD"; // 0000 0010
                break;
            case 3:
                power = "HIGH"; // 0000 0011
                break;
        }
        txtPower.setText(power);
    }

    private void setBottomSQLLevel(int option) {
        String sql = "";
        switch (option){
            case 8: // 0000 1000
                sql = "SQL1";
                break;
            case 12: // 0000 1100
                sql = "SQL2";
                break;
            case 16: // 0001 0000
                sql = "SQL3";
                break;
            case 20: // 0001 0100
                sql = "SQL4";
                break;
            case 24: // 0001 1000
                sql = "SQL5";
                break;
            case 28: // 0001 1100
                sql = "SQL6";
                break;
            case 32: // 0010 0000
                sql = "SQL7";
                break;
            case 36: // 0010 0100
                sql = "SQL8";
                break;
            case 40: // 0010 1000
                sql = "SQL9";
                break;
            default:    // 0000 0100
                sql = "SQL0";
        }
        txtSQLLevel.setText(sql);
    }

    private void setBottomLock(int option){
        String lock = "KEY";
        if (option == 1)
            lock = "LOCK";
        txtLock.setText(lock);
    }

    private void setRowBottom(byte[] data){
        setBottomFreqShift((data[29] & 48));
        setBottomTRFRPT(bitSet(data[29], 3));
        setBottomPower((data[29] & 3));
        setBottomSQLLevel((data[30] & 60)); // 0011 1100
        setBottomLock(bitSet(data[30], 1));
    }

    private void setRowFreqA(byte[] data){
        String freqA = "   ";

        if (bitSet(data[23], 0) == 0) {
            freqA = "> ";
            if (bitSet(data[24], 1) == 1)
                freqA = "\u00BB ";
        }

        freqA += HexDump.fromHexToString(data, 1, 9);
        txtFreqA.setText(freqA);

        int intMemoryA = Math.abs(data[21]);
        if (intMemoryA > 0){
            txtChannelA.setVisibility(View.VISIBLE);
            txtChannelA.setText(String.format("%03d", intMemoryA));
        } else {
            txtChannelA.setVisibility(View.INVISIBLE);
        }

        // only dashes data[23]
        if ((data[23] & 60) == 60) {
            barSignalStrengthA.setVisibility(View.GONE);
            txtSignalStrengthA.setVisibility(View.VISIBLE);
        } else {
            txtSignalStrengthA.setVisibility(View.GONE);
            barSignalStrengthA.setVisibility(View.VISIBLE);
            int signalStrength = 0;
            switch ((data[23] & 60)){
                case 4:		// 00000100
                    signalStrength = 0;
                    break;
                case 8:		// 00001000
                    signalStrength = 10;
                    break;
                case 12: 	// 00001100
                    signalStrength = 20;
                    break;
                case 16:	// 00010000
                    signalStrength = 30;
                    break;
                case 20:	// 00010100
                    signalStrength = 40;
                    break;
                case 24:	// 00011000
                    signalStrength = 50;
                    break;
                case 28:	// 00011100
                    signalStrength = 60;
                    break;
                case 32:	// 00100000
                    signalStrength = 70;
                    break;
                case 36:	// 00100100
                    signalStrength = 80;
                    break;
                case 40:	// 00101000
                    signalStrength = 90;
                    break;
                default:
                    signalStrength = 0;

            }
            barSignalStrengthA.setProgress(signalStrength);
        }
    }

    private void setRowFreqB(byte[] data){
        String freqB = "   ";

        if (bitSet(data[23], 0) == 1) {
            freqB = "> ";
            if (bitSet(data[24], 1) == 1)
                freqB = "\u00BB ";
        }

        freqB += HexDump.fromHexToString(data, 11, 9);
        txtFreqB.setText(freqB);

        int intMemoryB = Math.abs(data[22]);
        if (intMemoryB > 0){
            txtChannelB.setVisibility(View.VISIBLE);
            txtChannelB.setText(String.format("%03d", intMemoryB));
        } else {
            txtChannelB.setVisibility(View.INVISIBLE);
        }

        // only dashes data[23]
        if ((data[24] & 60) == 60) {
            barSignalStrengthB.setVisibility(View.GONE);
            txtSignalStrengthB.setVisibility(View.VISIBLE);
        } else {
            txtSignalStrengthB.setVisibility(View.GONE);
            barSignalStrengthB.setVisibility(View.VISIBLE);
            int signalStrength = 0;
            switch ((data[24] & 60)){
                case 4:		// 00000100
                    signalStrength = 0;
                    break;
                case 8:		// 00001000
                    signalStrength = 10;
                    break;
                case 12: 	// 00001100
                    signalStrength = 20;
                    break;
                case 16:	// 00010000
                    signalStrength = 30;
                    break;
                case 20:	// 00010100
                    signalStrength = 40;
                    break;
                case 24:	// 00011000
                    signalStrength = 50;
                    break;
                case 28:	// 00011100
                    signalStrength = 60;
                    break;
                case 32:	// 00100000
                    signalStrength = 70;
                    break;
                case 36:	// 00100100
                    signalStrength = 80;
                    break;
                case 40:	// 00101000
                    signalStrength = 90;
                    break;
                default:
                    signalStrength = 0;
            }
            barSignalStrengthB.setProgress(signalStrength);
        }
    }

    private void continuousMessage(byte[] data) throws IOException {

        if(data.length > 0) {

            outputStream = new ByteArrayOutputStream();
            if(readBuffer != null && readBuffer.length > 0) {
                outputStream.write(readBuffer);
            }
            outputStream.write(data);

            readBuffer = outputStream.toByteArray();
            if (readBuffer.length > 96){ // Got enough data to stitch a config up

                int startPos = 0;
                for(byte b : readBuffer)
                {
                    if (b == -69 && (startPos+32) < readBuffer.length){ // -69 Is the config start Control Character
                        byte[] message = new byte[32];
                        System.arraycopy(readBuffer, startPos, message, 0, 32);

                        setRowFreqA(message);
                        setRowFreqB(message);
                        setRowTop(message);
                        setRowBottom(message);

                        startPos = 0;
                        break;
                    }
                    startPos += 1;
                }
                readBuffer = new byte[0]; //Empty the readBuffer
            }
        }
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        Toast.makeText(getActivity(), spn, Toast.LENGTH_SHORT).show();
    }
}
