package es.deusto.ingenieria.ssdd.bitTorrent.globalInformation;

import java.util.ArrayList;

/**
 * This class contains the list of peers returned by the tracker.
 *
 */
public class PeerStateList {
	private ArrayList<PeerState> peerStateList;
	private PeerState myself;
	private PeerState myselfLocal;
	
	public PeerStateList(PeerState myself){
		this.myself=myself;
		this.myselfLocal=new PeerState("127.0.0.1", myself.getPort(), myself.getBitfield().length);
		this.peerStateList=new ArrayList<PeerState>();
	}
	
	/**
	 * This method add a new peer to the peerList. If the peer is myself (my ip or local) 
	 * it is not added to the list
	 * @param peerState a new peer
	 * @return true if the peer is added or false otherwise
	 */
	public boolean add(PeerState peerState){
		if(!alreadyExists(peerState)){
			if(!isMyself(peerState)){
				return peerStateList.add(peerState);
			}else{
				return false;
			}
			
		}else{
			return false;
		}
	}
	
	/**
	 * THis method checks if the incoming peer is myself
	 * @param peerState the peer
	 * @return true if it is myself, false otherwise.
	 */
	private boolean isMyself(PeerState peerState){
		if(peerState.equals(this.myself)||peerState.equals(this.myselfLocal)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * THis method checks if the new peer already exists in the list.
	 * @param peerState the peer
	 * @return true if it exists and false otherwise.
	 */
	private boolean alreadyExists(PeerState peerState){
		boolean exists=false;
		for(int i=0, ii=this.peerStateList.size();i<ii&&!exists;i++)
		{
			if(peerStateList.get(i).equals(peerState)){
				exists=true;
			}
		}
		return exists;
	}
	
	public PeerState get(int index){
		return peerStateList.get(index);
	}
	public int size(){
		return this.peerStateList.size();
	}
	
	/**
	 * THis method get a peer from the list with an specific ip and port
	 * @param ip
	 * @param port
	 * @return the peer from the list and null if it does not exist
	 */
	public PeerState getByAddress(String ip, int port){
		PeerState peerState=null;
		for(int i=0, ii=this.peerStateList.size();i<ii&&peerState==null;i++)
		{
			if(peerStateList.get(i).getIp().equals(ip)&&peerStateList.get(i).getPort()==port){
				peerState=peerStateList.get(i);
			}
		}
		return peerState;
	}

}
