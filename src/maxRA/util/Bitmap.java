package maxRA.util;


/**
 * @author Dongwan Choi
 * @date 2011. 12. 2.
 */
public class Bitmap {
	private byte[] bits;
	private int len;
	
	public Bitmap(int len) {
		bits = new byte[(len/Byte.SIZE)+(len%Byte.SIZE)];
		this.len = len;
	}
	
	public Bitmap(byte[] bits) {
		this.bits = bits;
		this.len = bits.length * Byte.SIZE;
	}
	
	public void set(int pos) {
		if (pos < len) {
			int index = pos/Byte.SIZE;
			int offset = pos%Byte.SIZE;
			
			byte tmp = bits[index];
			bits[index] = (byte) (tmp | ((byte)0x01 << offset));
		}
		else {
			Debug._Error(this, "invalid bit position");
		}
	}
	
	public void unset(int pos) {
		if (pos < len) {
			int index = pos/Byte.SIZE;
			int offset = pos%Byte.SIZE;
			
			byte tmp = bits[index];
			bits[index] = (byte) (tmp & ~((byte)0x01 << offset));
		}
		else {
			Debug._Error(this, "invalid bit position");
		}
	}
	
	public void setAll() {
		for ( int i = 0; i < len/Byte.SIZE; i++ )
			bits[i] = (byte)0xff;
		if ( len%Byte.SIZE > 0 ) {
			for (int pos = (len/Byte.SIZE)*Byte.SIZE; pos < len; pos++)
				set(pos);
		}
	}
	
	public byte[] toByteArray() {
		return bits;
	}

	public boolean get(int pos) {
		if (pos < len) {
			int index = pos/Byte.SIZE;
			int offset = pos%Byte.SIZE;
			
			byte tmp = bits[index];
			return ( (tmp & ((byte)0x01 << offset)) != 0 );			
		}
		else {
			Debug._Error(this, "invalid bit position");
			return false;
		}
	}
	
	public int size() {
		return len;
	}
}

