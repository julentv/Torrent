package es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages;

import java.io.ByteArrayOutputStream;

public class Handsake {	
	private static final String DEFAULT_PROTOCOL = "BitTorrent protocol";
	private static final String RESERVED = "00000000";
	
	private int nameLength;
	private String protocolName;
	private String reserved;
	private String infoHash;
	private String peerId;
	
	public Handsake() {		
		this.protocolName = Handsake.DEFAULT_PROTOCOL;
		this.nameLength = this.protocolName.length();
		this.reserved = Handsake.RESERVED;
	}
	
	//Constructor con todos los parámetros
	public Handsake(String infoHash, String peerId){
		this();
		this.setInfoHash(infoHash);
		this.setPeerId(peerId);
		
		
	}
	
	public int getNameLength() {
		return nameLength;
	}

	public String getProtocolName() {
		return protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
		this.nameLength = protocolName.length();
	}

	public String getReserved() {
		return reserved;
	}

	public String getInfoHash() {
		return infoHash;
	}

	public void setInfoHash(String infoHash) {
		this.infoHash = infoHash;
	}

	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}
	
	public byte[] getBytes() {
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();

			result.write(this.nameLength);
			result.write(this.protocolName.getBytes());
			result.write(this.reserved.getBytes());
			result.write(this.infoHash.getBytes());
			result.write(this.peerId.getBytes());
			
			return result.toByteArray();
		} catch (Exception ex) {
			return null;
		}
	}		
		
		
	public String toString() {
		return new String(this.getBytes());
	}
	
	/**
	 * Método que parsea el string recibido del handsake a un objeto de tipo Handsake
	 * @param stringHandsake string recibido como hansake del peer
	 * @return handsake un objeto de tipo Handsake con los correspondientes atributos
	 */
	public static Handsake parseStringToHandsake(String stringHandsake){
		String protocolName= stringHandsake.substring(1,20);
		String infoHash= stringHandsake.substring(28,48);
		String peerId= stringHandsake.substring(48);
		Handsake handsake = new Handsake(infoHash, peerId);
		handsake.setProtocolName(protocolName);
		
		return handsake;
	}
	/**
	 * Método que comprueba que el handsake es válido
	 * @param handsake recibido por el peer
	 * @param infoHash del torrent que tengo
	 * @return isValid: booleano que devuelve true si el handsake es correcto y false si no lo es
	 */
	public static boolean isValidHandsakeForBitTorrentProtocol(Handsake handsake,  String infoHash){
		boolean isValid=false;
		if(handsake.getProtocolName().equals(DEFAULT_PROTOCOL)&&handsake.getInfoHash().equals(infoHash)){
			isValid=true;
		}
		
		return isValid;
	}
}