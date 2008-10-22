package com.meidusa.amoeba.aladdin.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.aladdin.io.MysqlResultSetPacket;
import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;
import com.meidusa.amoeba.mysql.net.packet.FieldPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.ResultSetHeaderPacket;
import com.meidusa.amoeba.mysql.net.packet.RowDataPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.jdbc.ResultSetHandler;

/**
 * 
 * @author struct
 *
 */
public class ResultSetUtil {
	private static Logger logger = Logger.getLogger(ResultSetUtil.class);
	
	public static int toFlag(ResultSetMetaData metaData,int column) throws SQLException{
		
		int flags = 0;
		if(metaData.isNullable(column) == 1){
			flags |= 0001;
		}
		
		if(metaData.isSigned(column)){
			flags |= 0020;
		}
		
		if(metaData.isAutoIncrement(column)){
			flags |= 0200;
		}
		
		return flags;
	}
	
	public static void resultSetToPacket(Connection source, MysqlResultSetPacket packet,ResultSet rs, ResultSetHandler handler) throws SQLException{
		ResultSetMetaData metaData = rs.getMetaData();
		int colunmCount = metaData.getColumnCount();
		synchronized (packet) {
			if(packet.resulthead == null){
				packet.resulthead = new ResultSetHeaderPacket();
				packet.resulthead.columns = colunmCount;
			}
			
			if(colunmCount>0){
				if(packet.fieldPackets == null){
					packet.fieldPackets = new FieldPacket[colunmCount];
					for(int i=0;i<colunmCount;i++){
						int j=i+1;
						packet.fieldPackets[i] = new FieldPacket();
						packet.fieldPackets[i].catalog = "def".intern();
						packet.fieldPackets[i].orgName = metaData.getColumnLabel(j);
						packet.fieldPackets[i].name = metaData.getColumnName(j);
						packet.fieldPackets[i].orgTable = metaData.getTableName(j);
						packet.fieldPackets[i].table = metaData.getTableName(j);
						packet.fieldPackets[i].db = metaData.getSchemaName(j);
						packet.fieldPackets[i].length = metaData.getColumnDisplaySize(j);
						packet.fieldPackets[i].flags = toFlag(metaData,j);
						packet.fieldPackets[i].decimals = (byte)metaData.getScale(j);
						packet.fieldPackets[i].character = 45;
						packet.fieldPackets[i].javaType = metaData.getColumnType(j);
						packet.fieldPackets[i].type = (byte)(MysqlDefs.javaTypeMysql(metaData.getColumnType(j)) & 0xff);
					}
				}
			}
		}
		
		while(rs.next()){
			RowDataPacket row = new RowDataPacket();
			row.columns = new ArrayList<String>(colunmCount);
			for(int i=0;i<colunmCount;i++){
				int j=i+1;
				row.columns.add(rs.getString(j));
			}
			if(logger.isDebugEnabled()){
				logger.debug("fetch result row:"+row);
			}
			packet.addRowDataPacket(row);
		}
	}
	
	
	public static void metaDataToPacket(MysqlResultSetPacket packet,ResultSetMetaData metaData) throws SQLException{
			
		int colunmCount = metaData.getColumnCount();
		synchronized (packet) {
			if(packet.resulthead == null){
				packet.resulthead = new ResultSetHeaderPacket();
				packet.resulthead.columns = colunmCount;
			}
			
			if(colunmCount>0){
				if(packet.fieldPackets == null){
					packet.fieldPackets = new FieldPacket[colunmCount];
					for(int i=0;i<colunmCount;i++){
						int j=i+1;
						packet.fieldPackets[i] = new FieldPacket();
						packet.fieldPackets[i].orgName = metaData.getColumnName(j);
						packet.fieldPackets[i].name = metaData.getColumnLabel(j);
						packet.fieldPackets[i].catalog = "def".intern();
						packet.fieldPackets[i].type = (byte)MysqlDefs.javaTypeMysql(metaData.getColumnType(j));
					}
				}
			}
		}
	}
	
	 public static byte[] fromHex(String hexString) {
	            String[] hex = hexString.split(" ");
	            byte[] b = new byte[hex.length];
	            for (int i = 0; i < hex.length; i++) {
	                b[i] = (byte) (Integer.parseInt(hex[i], 16) & 0xff);
	            }

	            return b;
	    }
	
	public static void main(String[] args) throws Exception{
		byte[] byt = fromHex("20 00 00 02 03 64 65 66 00 00 00 0A 40 40 73 71 6C 5F 6D 6F 64 65 00 0C 21 00 BA 00 00 00 FD 01 00 1F 00 00");
		MysqlPacketBuffer buffer = new MysqlPacketBuffer(byt);
		/*ResultSetHeaderPacket packet = new ResultSetHeaderPacket();
		packet.init(buffer);*/
		FieldPacket[] fields = new FieldPacket[(int)1];
		for(int i=0;i<1;i++){
			fields[i] = new FieldPacket();
			fields[i].init(buffer);
		}
		System.out.println(1 | 0200);
		
	}
}
