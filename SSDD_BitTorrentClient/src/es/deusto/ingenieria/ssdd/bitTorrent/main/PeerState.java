package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.util.ArrayList;

public class PeerState {
	private String ip;
	private int port;
	//yo a el
	private boolean am_choking;
	private boolean am_interested;
	//el a mi
	private boolean peer_choking;
	private boolean peer_interested;
	//los fragmentos que tiene o no
	private byte[] bitfield;
	
	public PeerState (String ip, int port, int numberOfPieces){
		this.ip=ip;
		this.port=port;
		this.am_choking=true;
		this.am_interested=false;
		this.peer_choking=true;
		this.peer_interested=false;
		this.bitfield=new byte[numberOfPieces];
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public boolean isAm_choking() {
		return am_choking;
	}

	public void setAm_choking(boolean am_choking) {
		this.am_choking = am_choking;
	}

	public boolean isAm_interested() {
		return am_interested;
	}

	public void setAm_interested(boolean am_interested) {
		this.am_interested = am_interested;
	}

	public boolean isPeer_choking() {
		return peer_choking;
	}

	public void setPeer_choking(boolean peer_choking) {
		this.peer_choking = peer_choking;
	}

	public boolean isPeer_interested() {
		return peer_interested;
	}

	public void setPeer_interested(boolean peer_interested) {
		this.peer_interested = peer_interested;
	}

	public byte[] getBitfield() {
		return bitfield;
	}

	/**
	 * El tamaño del array de bytes introducido debe ser del mismo tamaño que el ya existente.
	 * @param bitfield
	 */
	public void setBitfield(byte[] bitfield) {
		if(bitfield.length==this.bitfield.length){
			this.bitfield = bitfield;
		}else{
			throw new IllegalArgumentException("The length of the array is illegal.");
		}
		
	}
	public void updateBitfieldPosition(int position, byte value){
		this.bitfield[position]=value;		
	}
	public byte getBitfieldPosition(int position){
		return this.bitfield[position];
	}

}
