package com.meidusa.amoeba.oracle.net.packet;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.oracle.accessor.Accessor;
import com.meidusa.amoeba.oracle.accessor.T4CCharAccessor;
import com.meidusa.amoeba.oracle.accessor.T4CDateAccessor;
import com.meidusa.amoeba.oracle.accessor.T4CNumberAccessor;
import com.meidusa.amoeba.oracle.accessor.T4CVarcharAccessor;
import com.meidusa.amoeba.oracle.accessor.T4CVarnumAccessor;
import com.meidusa.amoeba.oracle.net.packet.assist.T4C8TTILob;
import com.meidusa.amoeba.oracle.net.packet.assist.T4CTTIoac;

/**
 * @author hexianmao
 * @version 2008-8-20 ����03:02:04
 */
public class T4C8OallDataPacket extends T4CTTIfunPacket {

    private static Logger logger      = Logger.getLogger(T4C8OallDataPacket.class);

    long                  options;
    int                   cursor;
    int                   sqlStmtLength;
    int                   numberOfParams;
    int                   al8i4Length;
    int                   defCols;

    public String         sqlStmt;                                                 // sql
    long[]                al8i4;
    T4CTTIoac[]           oacBind;
    public Accessor[]     accessors;                                               // parameter Accessors
    T4CTTIoac[]           oacdefDefines;
    public Accessor[]     definesAccessors;
    public byte[][]       paramBytes;                                              // parameter bytes

    private boolean       isSqlPacket = false;
    private boolean       isOlobops   = false;
    private T4C8TTILob    lob;

    private boolean       isOlogoff   = false;

    public T4C8OallDataPacket(){
        super(OALL8);
        this.defCols = 0;
        this.al8i4 = new long[13];
    }

    public T4C8TTILob getLob() {
        return lob;
    }

    public boolean isOlogoff() {
        return isOlogoff;
    }

    public boolean isSqlPacket() {
        return isSqlPacket;
    }

    public boolean isOlobops() {
        return isOlobops;
    }

    public String getSqlStmt() {
        return sqlStmt;
    }

    public Accessor[] getAccessors() {
        return accessors;
    }

    public byte[][] getParamBytes() {
        return paramBytes;
    }

    @Override
    protected void unmarshal(AbstractPacketBuffer buffer) {
        super.unmarshal(buffer);

        T4CPacketBuffer meg = (T4CPacketBuffer) buffer;
        if (msgCode == TTIFUN) {
            parseFunPacket(meg);
        } else if (msgCode == TTIPFN && funCode == OCCA) {
            T4C8OcloseDataPacket closePacket = new T4C8OcloseDataPacket();
            closePacket.initCloseStatement();
            closePacket.parsePacket(meg);
            msgCode = closePacket.msgCode;
            funCode = closePacket.funCode;
            seqNumber = closePacket.seqNumber;
            parseFunPacket(meg);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("type:OtherPacket msgCode:" + msgCode + " funCode:" + funCode);
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    private void parseFunPacket(T4CPacketBuffer meg) {
        switch (funCode) {
            case OALL8:
                parseOALL8(meg);
                break;
            case OFETCH:
                parseOFETCH(meg);
                break;
            case OEXEC:
                parseOEXEC(meg);
                break;
            case OLOBOPS:
                parseOLOBOPS(meg);
                break;
            case OLOGOFF:
                parseOLOGOFF(meg);
                break;
            default:
                if (logger.isDebugEnabled()) {
                    logger.debug("type:OtherFunPacket funCode:" + funCode);
                }
        }
    }

    private void parseOALL8(T4CPacketBuffer meg) {
        unmarshalPisdef(meg);

        byte[] sqlStmtBytes = meg.unmarshalCHR(sqlStmtLength);

        sqlStmt = meg.getConversion().CharBytesToString(sqlStmtBytes, sqlStmtLength);
        isSqlPacket = true;

        meg.unmarshalUB4Array(al8i4);

        oacBind = new T4CTTIoac[numberOfParams];
        accessors = new Accessor[numberOfParams];
        unmarshalBindsTypes(meg);// ������������������ʼ����Ӧ��accessor��

        if (meg.versionNumber >= 9000 && defCols > 0) {
            oacdefDefines = new T4CTTIoac[defCols];
            for (int i = 0; i < defCols; i++) {
                oacdefDefines[i] = new T4CTTIoac();
                oacdefDefines[i].unmarshal(meg);
            }
        }

        paramBytes = new byte[numberOfParams][];
        if (numberOfParams > 0) {
            unmarshalBinds(meg);// ��������������ȡ��Ӧ�Ĳ���ֵ��
        }
    }

    private void unmarshalPisdef(T4CPacketBuffer meg) {
        options = meg.unmarshalUB4();
        cursor = meg.unmarshalSWORD();
        meg.unmarshalPTR();
        sqlStmtLength = meg.unmarshalSWORD();
        meg.unmarshalPTR();
        al8i4Length = meg.unmarshalSWORD();
        meg.unmarshalPTR();
        meg.unmarshalPTR();
        meg.unmarshalUB4();
        meg.unmarshalUB4();
        meg.unmarshalUB4();

        meg.unmarshalPTR();
        numberOfParams = meg.unmarshalSWORD();

        meg.unmarshalPTR();
        meg.unmarshalPTR();
        meg.unmarshalPTR();
        meg.unmarshalPTR();
        meg.unmarshalPTR();

        if (meg.versionNumber >= 9000) {
            meg.unmarshalPTR();
            defCols = meg.unmarshalSWORD();
        }
    }

    private void unmarshalBindsTypes(T4CPacketBuffer meg) {
        for (int i = 0; i < numberOfParams; i++) {
            oacBind[i] = new T4CTTIoac();
            oacBind[i].unmarshal(meg);
            fillAccessor(i, oacBind[i], meg);
        }
    }

    private void fillAccessor(int i, T4CTTIoac oac, T4CPacketBuffer meg) {
        switch (oac.oacdty) {
            case Accessor.CHAR:
                accessors[i] = new T4CCharAccessor();
                break;
            case Accessor.NUMBER:
                accessors[i] = new T4CNumberAccessor();
                break;
            case Accessor.VARCHAR:
                accessors[i] = new T4CVarcharAccessor();
                break;
            case Accessor.LONG:
                // accessors[idx] = new T4CLongAccessor();
                break;
            case Accessor.VARNUM:
                accessors[i] = new T4CVarnumAccessor();
                break;
            case Accessor.BINARY_FLOAT:
                // accessors[idx] = new T4CBinaryFloatAccessor();
                break;
            case Accessor.BINARY_DOUBLE:
                // accessors[idx] = new T4CBinaryDoubleAccessor();
                break;
            case Accessor.RAW:
                // accessors[idx] = new T4CRawAccessor();
                break;
            case Accessor.LONG_RAW:
                // if (meg.versionNumber >= 9000) {
                // accessors[idx] = new T4CRawAccessor();
                // } else {
                // accessors[idx] = new T4CLongRawAccessor();
                // }
                break;
            case Accessor.ROWID:
            case Accessor.UROWID:
                // accessors[idx] = new T4CRowidAccessor();
                break;
            case Accessor.RESULT_SET:
                // accessors[idx] = new T4CResultSetAccessor();
                break;
            case Accessor.DATE:
                accessors[i] = new T4CDateAccessor();
                break;
            case Accessor.BLOB:
                // if (meg.versionNumber >= 9000 && l1 == -4) {
                // accessors[idx] = new T4CLongRawAccessor();
                // } else if (meg.versionNumber >= 9000 && l1 == -3) {
                // accessors[idx] = new T4CRawAccessor();
                // } else {
                // accessors[idx] = new T4CBlobAccessor();
                // }
                break;
            case Accessor.CLOB:
                // if (meg.versionNumber >= 9000 && l1 == -1) {
                // accessors[idx] = new T4CLongAccessor();
                // } else if (meg.versionNumber >= 9000 && (l1 == 12 || l1 == 1)) {
                // accessors[idx] = new T4CVarcharAccessor();
                // } else {
                // accessors[idx] = new T4CClobAccessor();
                // }
                break;
            case Accessor.BFILE:
                // accessors[idx] = new T4CBfileAccessor();
                break;
            case Accessor.NAMED_TYPE:
                // accessors[idx] = new T4CNamedTypeAccessor();
                break;
            case Accessor.REF_TYPE:
                // accessors[idx] = new T4CRefTypeAccessor();
                break;
            case Accessor.TIMESTAMP:
                // accessors[idx] = new T4CTimestampAccessor();
                break;
            case Accessor.TIMESTAMPTZ:
                // accessors[idx] = new T4CTimestamptzAccessor();
                break;
            case Accessor.TIMESTAMPLTZ:
                // accessors[idx] = new T4CTimestampltzAccessor();
                break;
            case Accessor.INTERVALYM:
                // accessors[idx] = new T4CIntervalymAccessor();
                break;
            case Accessor.INTERVALDS:
                // accessors[idx] = new T4CIntervaldsAccessor();
                break;
            default:
                throw new RuntimeException("unknown data type!");
        }

        if (accessors[i] != null) {
            accessors[i].setOac(oac);
            accessors[i].setConv(meg.getConversion());
        }
    }

    private void unmarshalBinds(T4CPacketBuffer meg) {
        short msgCode = meg.unmarshalUB1();
        if (msgCode == TTIRXD) {
            byte[][] tmp = new byte[numberOfParams][];
            byte[][] bigTmp = new byte[numberOfParams][];

            int m = 0, l = 0;
            for (int k = 0; k < numberOfParams; k++) {
                byte[] tmpBytes = meg.unmarshalCLRforREFS();
                if (tmpBytes != null && tmpBytes.length > 4000) {
                    bigTmp[m++] = tmpBytes;
                } else {
                    tmp[l++] = tmpBytes;
                }
            }

            int x = 0, y = 0;
            for (int i = 0; i < numberOfParams; i++) {
                if (oacBind[i].oacmxl > 4000) {
                    paramBytes[i] = bigTmp[x++];
                } else {
                    paramBytes[i] = tmp[y++];
                }
            }
            tmp = null;
            bigTmp = null;
        } else {
            throw new RuntimeException();
        }
    }

    private void parseOFETCH(T4CPacketBuffer meg) {
    }

    private void parseOEXEC(T4CPacketBuffer meg) {
        if (logger.isDebugEnabled()) {
            logger.debug("type:T4CTTIfunPacket.OEXEC");
        }
    }

    private void parseOLOBOPS(T4CPacketBuffer meg) {
        isOlobops = true;
        lob = new T4C8TTILob();
        lob.unmarshal(meg);
    }

    private void parseOLOGOFF(T4CPacketBuffer meg) {
        isOlogoff = true;
    }

    // ///////////////////////////////////////////////////////////////////////////////////

    public static boolean isParseable(byte[] message) {
        if (T4CTTIfunPacket.isFunType(message, T4CTTIfunPacket.OALL8)) {
            return true;
        }
        if (T4CTTIfunPacket.isFunType(message, T4CTTIfunPacket.OFETCH)) {
            return true;
        }
        if (T4CTTIfunPacket.isFunType(message, T4CTTIfunPacket.OEXEC)) {
            return true;
        }
        if (T4CTTIfunPacket.isFunType(message, T4CTTIfunPacket.OLOBOPS)) {
            return true;
        }
        if (T4CTTIfunPacket.isFunType(message, T4CTTIMsgPacket.TTIPFN, T4CTTIfunPacket.OCCA)) {
            return true;
        }
        return false;
    }

}