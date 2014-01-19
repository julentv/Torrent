package es.deusto.ingenieria.ssdd.bitTorrent.main;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.tools.Tool;

import es.deusto.ingenieria.ssdd.bitTorrent.file.FileManagement;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.BitfieldMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.ChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.Handsake;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.HaveMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PeerProtocolMessage;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.PieceMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.RequestMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.peer.protocol.messages.UnChokeMsg;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

public class SendToPeer extends Thread {
	private TorrentClient torrentClient;
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private PeerState peerState;
	public SendToPeer(TorrentClient torrentClient, Socket socket){
		this.torrentClient=torrentClient;
		this.socket=socket;
	}
	
	@Override
	public void run() {
		try {
			this.inputStream=socket.getInputStream();
			this.outputStream=socket.getOutputStream();
		
		peerState=torrentClient.getPeerStateList().getByAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
		//read handhake
			byte []received=new byte[68];
			System.out.println("Waiting for handshake");
			inputStream.read(received);	
			System.out.println("handshake received");
			Handsake handshakeMessage=Handsake.parseStringToHandsake(new String(received));
			//validar hash
			String torrentHash=new String(torrentClient.getMetainf().getInfo().getInfoHash());
			if(Handsake.isValidHandsakeForBitTorrentProtocol(handshakeMessage, torrentHash)){
				//send handshake
				Handsake handShakeToSend= new Handsake(torrentHash, this.torrentClient.getPeerId());
				this.outputStream.write(handShakeToSend.getBytes());
				//enviar bitfield
				try{
					this.sendBitfield();
					//leer
					PeerProtocolMessage message=this.readNormalMessage();
					//comprobar que se ha recibido un interested
					if(message.getType().equals(PeerProtocolMessage.Type.INTERESTED)){
						if(this.peerState!=null){
							peerState=torrentClient.getPeerStateList().getByAddress(socket.getInetAddress().getHostAddress(), socket.getPort());						
						}
						if(this.peerState!=null){
							this.peerState.setPeer_interested(true);
						}
						
						//unchoke
						this.outputStream.write(new UnChokeMsg().getBytes());
						if(this.peerState!=null){
							this.peerState.setAm_choking(false);
						}
						
						boolean continueReading=true;
						while(continueReading){
							message=this.readNormalMessage();
							continueReading=processMessage(message);
						}
						
						
					}else{
						//choke
						this.outputStream.write(new ChokeMsg().getBytes());
					}
					
					
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
			}
			//close connection
			this.inputStream.close();
			this.outputStream.close();
			socket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Sending thread closed");
			e.printStackTrace();
			try {
				this.inputStream.close();
				this.outputStream.close();
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	private boolean processMessage(PeerProtocolMessage message) throws IOException{
		boolean accepted= true;
		if(message.getType().equals(PeerProtocolMessage.Type.KEEP_ALIVE)){
			
		}else if(message.getType().equals(PeerProtocolMessage.Type.REQUEST)){
			//read the index
			System.out.println("Request recibido");
			//leer pieza del fichero
			FileManagement fm= new FileManagement(this.torrentClient.getMetainf().getInfo().getName(), this.torrentClient.getMetainf().getInfo().getLength());
			int position=0;
			position=(((RequestMsg) message).getIndex()*this.torrentClient.getFragmentsInformation().getFragmentLength())+((RequestMsg) message).getOffset();
			if((position+((RequestMsg) message).getOffset())<this.torrentClient.getMetainf().getInfo().getLength()){
				
				byte[]bytes=fm.readFromFile(position, ((RequestMsg) message).getPieceLength());
				//send piece
				PieceMsg pieceMsg= new PieceMsg (((RequestMsg) message).getIndex(), ((RequestMsg) message).getOffset(), bytes);
				this.outputStream.write(pieceMsg.getBytes());
				this.torrentClient.setUploaded(this.torrentClient.getUploaded()+((RequestMsg) message).getPieceLength());
			}else{
				accepted=false;
			}
			
		}else if(message.getType().equals(PeerProtocolMessage.Type.HAVE)){
			if(this.peerState==null){
				peerState=torrentClient.getPeerStateList().getByAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
			}
			if(this.peerState!=null){
				//actualize the bitfield of the peerState when a have message arrives
				int pieceNumber=ToolKit.bigEndianBytesToInt(((HaveMsg)message).getPayload(), 0);
				peerState.getBitfield()[pieceNumber]=1;
			}
		}else{
			accepted=false;
		}
		return accepted;
	}
	private void sendBitfield() throws IOException{
		byte[]bitfield=new byte[this.torrentClient.getNumberOfPieces()];
		FragmentsInformation fi=this.torrentClient.getFragmentsInformation();
		if(fi==null||fi.getCurrentFragment()==-1){
			//tenemos todas las piezas
			for(int i=0,ii=bitfield.length;i<ii;i++){
				bitfield[i]=1;
			}
		}else{
			//tenemos algunas piezas
			int lastFragment=this.torrentClient.getFragmentsInformation().getCurrentFragment();
			int i=0;
			
			for(;i<lastFragment;i++){
				bitfield[i]=1;
			}
			for(int ii=bitfield.length;i<ii;i++){
				bitfield[i]=0;
			}
		}
		
		BitfieldMsg message= new BitfieldMsg(bitfield);
		this.outputStream.write(message.getBytes());
	}
	
	private PeerProtocolMessage readNormalMessage() throws IOException{
		byte[]length=new byte[4];
		this.inputStream.read(length);
		int lengthInt=ToolKit.bigEndianBytesToInt(length, 0);
		byte[]message= new byte[lengthInt];
		this.inputStream.read(message);
		byte[]result=new byte[length.length+message.length];
		System.arraycopy(length, 0, result, 0, length.length);
		System.arraycopy(message, 0, result, length.length, message.length);
		return PeerProtocolMessage.parseMessage(result);
	}

}
