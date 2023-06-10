package com.example.keydisplayunit;

public class Constants {

    // KDU Button Byte Arrays
    static final byte[] MessageSWITCHMEMORYSLOT = { (byte)0xAA, (byte)0x00, (byte)0x21, (byte)0x00 };
    static final byte[] MessagePTT              = { (byte)0xAA, (byte)0x01, (byte)0x00, (byte)0x00 };
    static final byte[] MessageVOLUP            = { (byte)0xAA, (byte)0x00, (byte)0x22, (byte)0x00 };
    static final byte[] MessageVOLDOWN          = { (byte)0xAA, (byte)0x00, (byte)0x23, (byte)0x00 };
    static final byte[] MessageZERO             = { (byte)0xAA, (byte)0x00, (byte)0x0A, (byte)0x00 };
    static final byte[] MessageONE              = { (byte)0xAA, (byte)0x00, (byte)0x01, (byte)0x00 };
    static final byte[] MessageTWO              = { (byte)0xAA, (byte)0x00, (byte)0x02, (byte)0x00 };
    static final byte[] MessageTHREE            = { (byte)0xAA, (byte)0x00, (byte)0x03, (byte)0x00 };
    static final byte[] MessageFOUR             = { (byte)0xAA, (byte)0x00, (byte)0x04, (byte)0x00 };
    static final byte[] MessageFIVE             = { (byte)0xAA, (byte)0x00, (byte)0x05, (byte)0x00 };
    static final byte[] MessageSIX              = { (byte)0xAA, (byte)0x00, (byte)0x06, (byte)0x00 };
    static final byte[] MessageSEVEN            = { (byte)0xAA, (byte)0x00, (byte)0x07, (byte)0x00 };
    static final byte[] MessageEIGHT            = { (byte)0xAA, (byte)0x00, (byte)0x08, (byte)0x00 };
    static final byte[] MessageNINE             = { (byte)0xAA, (byte)0x00, (byte)0x09, (byte)0x00 };
    static final byte[] MessageCLR              = { (byte)0xAA, (byte)0x00, (byte)0x10, (byte)0x00 };
    static final byte[] MessageENT              = { (byte)0xAA, (byte)0x00, (byte)0x0F, (byte)0x00 };
    static final byte[] MessagePREUP            = { (byte)0xAA, (byte)0x00, (byte)0x31, (byte)0x00 };
    static final byte[] MessagePREDOWN          = { (byte)0xAA, (byte)0x00, (byte)0x32, (byte)0x00 };
    static final byte[] MessageARROWRIGHT       = { (byte)0xAA, (byte)0x00, (byte)0x0e, (byte)0x00 };
    static final byte[] MessageARROWLEFT        = { (byte)0xAA, (byte)0x00, (byte)0x0d, (byte)0x00 };
    static final byte[] MessageNULL             = { (byte)0xAA, (byte)0x00, (byte)0x00, (byte)0x00 };

    // KDU Connection constants
    static final int BAUDRATE = 19200;

    private Constants() {}
}
