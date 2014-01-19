package es.deusto.ingenieria.ssdd.bitTorrent.metainfo;

import java.util.ArrayList;
import java.util.HashMap;

import es.deusto.ingenieria.ssdd.bitTorrent.bencoding.Bencoder;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.PeerState;
import es.deusto.ingenieria.ssdd.bitTorrent.globalInformation.PeerStateList;
/**
 * Class that parses the tracker response
 * 
 *
 */
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
	/**
	 * Generates, from the peer list into the tracker response,
	 * a PeerStateList to be used into the application
	 * @param numberOfPieces NUmber of pieces of the fragment
	 * @param myself Peer with the ip and port of the own application in order to not introduce the peers with the same ip an port into the list.
	 * @return The PeerStateList with all the PeerStates containing the peer's information.
	 */
	@SuppressWarnings("rawtypes")
	public PeerStateList getPeerStateArray(int numberOfPieces, PeerState myself){
		PeerStateList peerStateArray= new PeerStateList(myself);
		for(HashMap peer:peers){
			PeerState peerState= new PeerState(((String)peer.get("ip")), (int) (peer.get("port")), numberOfPieces);
			peerStateArray.add(peerState);
		}
		return peerStateArray;
	}

}
