package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.util.ArrayList;

public class PeerStateList {
	private ArrayList<PeerState> peerStateList;
	private PeerState myself;
	private PeerState myselfLocal;
	public PeerStateList(PeerState myself){
		this.myself=myself;
		this.myselfLocal=new PeerState("127.0.0.1", myself.getPort(), myself.getBitfield().length);
		this.peerStateList=new ArrayList<PeerState>();
	}
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
	private boolean isMyself(PeerState peerState){
		if(peerState.equals(this.myself)||peerState.equals(this.myselfLocal)){
			return true;
		}else{
			return false;
		}
	}
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
