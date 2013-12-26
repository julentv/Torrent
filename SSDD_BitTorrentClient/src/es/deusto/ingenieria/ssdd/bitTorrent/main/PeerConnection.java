package es.deusto.ingenieria.ssdd.bitTorrent.main;

import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;

public class PeerConnection implements Runnable{
	private PeerState peer;
	private TorrentClient torrent;
	
	private void connectToPeer(){
		/**
		 * ifsi devuelve handshake !=null--parsear
		 * procesar
		 * requesst
		 */
		
		if (Handshake()!=null){
			
		}
	}
	
	public byte[] Handshake (){
		return null;
		
	}
	
	private PeerProtocolMessage[] parseHandshakeResponse(byte[] response) throws IllegalArgumentException{
		return null;
		
	}
	
	private void ProcessMessage(PeerProtocolMessage message){
		
	}
	
	private void Request(){
		//comprobar pperstate contains currentfragment y si el current fragment es -1 (ha acabao ya)
		//comprobar siguiente subfragmento a descargar
		//descargar subfragmento--mandar request
		//volver a paso 1
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		connectToPeer();
	}
	
	
}
