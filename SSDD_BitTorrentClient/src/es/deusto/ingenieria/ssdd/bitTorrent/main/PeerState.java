package es.deusto.ingenieria.ssdd.bitTorrent.main;

/**
 * This class has all information relative to a peer.
 *
 */
public class PeerState {
	private String ip;
	private int port;
	private boolean am_choking;
	private boolean am_interested;
	private boolean peer_choking;
	private boolean peer_interested;
	//fragments that the peer has
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
	 * THis method set the biftield of the peer
	 * The lenght of the new bitfield array must be equals to the existent one.
	 * @param bitfield
	 */
	public void setBitfield(byte[] bitfield) {
		
		if(bitfield.length==this.bitfield.length){
			this.bitfield = bitfield;
		}
		
	}
	
	/**
	 * This method update one position in the bitfield of the peer. 
	 * This is done when a peer informs that it downloads another fragment.
	 * @param position index of the download fragment
	 * @param value 0 or 1 depending if the peer has download or not the fragment.
	 */
	public void updateBitfieldPosition(int position, byte value){
		this.bitfield[position]=value;		
	}
	public byte getBitfieldPosition(int position){
		return this.bitfield[position];
	}
	
	/**
	 * This method checks if the peer has one specific fragment or not.
	 * @param position index of the fragment
	 * @return true if the peer has the fragment and false otherwise.
	 */
	public boolean hasFragment(int position){
		if(this.bitfield[position]==0){
			return false;
		}else{
			return true;
		}
	
	}
	
	/**
	 * This method compare two peers checking the ip and port.
	 * @param peerState one peer
	 * @return true if they are equals or false otherwise
	 */
	public boolean equals(PeerState peerState){
		if(this.ip.equals(peerState.getIp())&&this.port==peerState.getPort()){
			return true;
		}else{
			return false;
		}
	}

	
}
