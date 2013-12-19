package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.util.ArrayList;
import java.util.HashMap;

import es.deusto.ingenieria.ssdd.bitTorrent.bencoding.Bencoder;

public class MetainfoStringHandler {
	private int interval;
	private ArrayList<HashMap<String,String>> peers;
	
	@SuppressWarnings("unchecked")
	public MetainfoStringHandler(String textoToHandle){
		byte[] bytes=textoToHandle.getBytes();
		Bencoder bencoder= new Bencoder();
		HashMap<String, Object> dictionary = bencoder.unbencodeDictionary(bytes);
		this.peers=(ArrayList<HashMap<String, String>>) dictionary.get("peers");
		this.interval =(Integer) dictionary.get("interval");
	}

	public int getInterval() {
		return interval;
	}

	public ArrayList<HashMap<String, String>> getPeers() {
		return peers;
	}
	public ArrayList<PeerState> getPeerStateArray(int numberOfPieces){
		ArrayList<PeerState> peerStateArray= new ArrayList<PeerState>();
		for(HashMap<String, String> peer:peers){
			PeerState peerState= new PeerState(peer.get("ip"), Integer.parseInt(peer.get("port")), numberOfPieces);
			peerStateArray.add(peerState);
		}
		return peerStateArray;
	}

}
