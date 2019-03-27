package android_serialport_api;

public class Crc {
	public static int xCal_crc(int ptr[], long len) {
		int crc;
		int i;
		crc = 0;
		int count = 0;
		while (len-- != 0) {
			 crc = (int)(crc^(ptr[count]));
			 if(crc>255) {
				 crc%=256;
			 }
			count++;
			for (i = 0; i < 8; i++) {
				if ((crc & 0b00000001) != 0) {
					crc = (int)((crc >> 1) ^ 0b10001100);
					if(crc>255) {
						 crc%=256;
					 }
				}else
					crc >>= 1;
			}
		}
		if(crc>127)
			crc= crc-256;
		return crc;
	}

}