package com.meidusa.amoeba.oracle.net.packet;

import java.sql.SQLException;

import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.oracle.net.OracleConnection;
import com.meidusa.amoeba.oracle.util.DBConversion;

/**
 * 协议数据包
 * 
 * @author hexianmao
 * @version 2008-8-14 下午07:29:53
 */
public class T4C8TTIproResponseDataPacket extends T4CTTIMsgPacket {

    public byte    proSvrVer        = 6;
    public short   oVersion         = 8100;
    public String  proSvrStr        = "Linuxi386/Linux-2.0.34-8.1.0";
    public short   svrCharSet       = 1;
    public byte    svrFlags         = 1;
    public short   svrCharSetElem   = 0;
    public boolean svrInfoAvailable = false;
    public short   NCHAR_CHARSET    = 2000;

    private byte[] abyte0           = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, (byte) 0xd0 };
    private byte[] as0              = null;
    private byte[] as1              = null;

    public T4C8TTIproResponseDataPacket(){
        super(TTIPRO);
    }

    @Override
    protected void marshal(AbstractPacketBuffer buffer) {
        super.marshal(buffer);
        T4CPacketBuffer meg = (T4CPacketBuffer) buffer;
        meg.writeByte(proSvrVer);
        meg.marshalNULLPTR();
        meg.writeBytes(proSvrStr.getBytes());
        meg.marshalNULLPTR();
        meg.marshalUB2(svrCharSet);
        meg.marshalUB1(svrFlags);
        meg.marshalUB2(svrCharSetElem);
        if (svrCharSetElem > 0) {
            byte[] ab = new byte[svrCharSetElem * 5];
            meg.marshalB1Array(ab);
        }

        if (proSvrVer < 5) {
            return;
        }
        byte byte0 = meg.getTypeRep().getRep((byte) 1);
        meg.getTypeRep().setRep((byte) 1, (byte) 0);

        meg.marshalUB2(abyte0.length);

        meg.getTypeRep().setRep((byte) 1, byte0);
        meg.marshalB1Array(abyte0);

        if (proSvrVer < 6) {
            return;
        }

        if (as0 != null) {
            meg.marshalUB1((short) as0.length);
            meg.marshalB1Array(as0);
        } else {
            meg.marshalNULLPTR();
        }

        if (as1 != null) {
            meg.marshalUB1((short) as1.length);
            meg.marshalB1Array(as1);
        } else {
            meg.marshalNULLPTR();
        }
    }

    @Override
    protected void unmarshal(AbstractPacketBuffer buffer) {
        super.unmarshal(buffer);
        T4CPacketBuffer meg = (T4CPacketBuffer) buffer;
        proSvrVer = meg.unmarshalSB1();
        switch (proSvrVer) {
            case 4:
                oVersion = MIN_OVERSION_SUPPORTED;
                break;
            case 5:
                oVersion = ORACLE8_PROD_VERSION;
                break;
            case 6:
                oVersion = ORACLE81_PROD_VERSION;
                break;
            default:
                throw new RuntimeException("不支持从服务器接收到的 TTC 协议版本");
        }
        meg.unmarshalSB1();
        proSvrStr = new String(meg.unmarshalTEXT(50));
        svrCharSet = (short) meg.unmarshalUB2();
        svrFlags = (byte) meg.unmarshalUB1();
        svrCharSetElem = (short) meg.unmarshalUB2();
        if (svrCharSetElem > 0) {
            meg.unmarshalNBytes(svrCharSetElem * 5);
        }
        svrInfoAvailable = true;

        if (proSvrVer < 5) {
            return;
        }
        byte byte0 = meg.getTypeRep().getRep((byte) 1);
        meg.getTypeRep().setRep((byte) 1, (byte) 0);
        int i = meg.unmarshalUB2();
        meg.getTypeRep().setRep((byte) 1, byte0);
        abyte0 = meg.unmarshalNBytes(i);
        int j = 6 + (abyte0[5] & 0xff) + (abyte0[6] & 0xff);
        NCHAR_CHARSET = (short) ((abyte0[j + 3] & 0xff) << 8);
        NCHAR_CHARSET |= (short) (abyte0[j + 4] & 0xff);

        if (proSvrVer < 6) {
            return;
        }
        short word0 = meg.unmarshalUB1();
        as0 = new byte[word0];
        for (int k = 0; k < word0; k++) {
            as0[k] = (byte) meg.unmarshalUB1();
        }
        short word1 = meg.unmarshalUB1();
        as1 = new byte[word1];
        for (int l = 0; l < word1; l++) {
            as1[l] = (byte) meg.unmarshalUB1();
        }
    }

}
