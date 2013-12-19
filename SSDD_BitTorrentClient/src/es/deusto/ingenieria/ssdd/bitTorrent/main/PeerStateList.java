package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.util.ArrayList;

public class PeerStateList {
	private ArrayList<PeerState> peerStateList;
	public PeerStateList(){
		this.peerStateList=new ArrayList<PeerState>();
	}
	public boolean add(PeerState peerState){
		if(!alreadyExists(peerState)){
			return peerStateList.add(peerState);
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

}
