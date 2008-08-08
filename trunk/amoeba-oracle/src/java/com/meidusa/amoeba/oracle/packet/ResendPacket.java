package com.meidusa.amoeba.oracle.packet;

import org.apache.log4j.Logger;

/**
 * �������˷��ص�Ҫ��ͻ����ط������ݰ�
 * 
 * @author hexianmao
 * @version 2008-8-6 ����05:32:52
 */
public class ResendPacket extends AbstractPacket {

    private static Logger logger = Logger.getLogger(ResendPacket.class);

    public void init(byte[] buffer) {
        super.init(buffer);

        if (logger.isDebugEnabled()) {
            logger.debug(this.toString());
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ResendPacket info ==============================\n");
        return sb.toString();
    }
}