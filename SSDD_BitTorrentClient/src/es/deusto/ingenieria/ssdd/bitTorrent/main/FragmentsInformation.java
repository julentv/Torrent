package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.util.List;


public class FragmentsInformation {
	private int currentFragment;
	private byte[][]downloadingFragments;
	private boolean[]isDownloaded;
	//estas posiciones se ponen a true cuando alguien EMPIEZA a descargar un fragmento
	private boolean[]canBeDownloaded;
	private int fragmentLength;
	private int fileLength;
	private int subfragmentLength;
	private int numberOfFragments;
	private List<String>pieceHashes;
	public FragmentsInformation(int fileLength, int fragmentLength, int subfragmentLength, int currentFragment, List<String>pieceHashes){
			this.fileLength=fileLength;
			this.fragmentLength=fragmentLength;
			this.subfragmentLength=subfragmentLength;
			this.currentFragment=currentFragment;
			this.numberOfFragments=numberOfPieces(fileLength, fragmentLength);
			this.pieceHashes=pieceHashes;
			initializeSubFragments();
			
			System.out.println("asdasd");
	}
	private synchronized void initializeSubFragments(){
		int numberOfSubFragments=0;
		if(this.isLastPiece()){
			numberOfSubFragments=numberOfPieces(fragmentLength,subfragmentLength);
			downloadingFragments= new byte[numberOfSubFragments][];
			for(int i=0,ii=downloadingFragments.length;i<ii;i++){
				downloadingFragments[i]=new byte[subfragmentLength];
			}
		}else{
			int resto=fileLength%fragmentLength;
			numberOfSubFragments=numberOfPieces(resto,subfragmentLength);
			int lastPieceSize=resto%subfragmentLength;
			downloadingFragments= new byte[numberOfSubFragments][];
			for(int i=0,ii=downloadingFragments.length;i<ii;i++){
				downloadingFragments[i]=new byte[subfragmentLength];
			}
			if(lastPieceSize!=0){
				downloadingFragments[numberOfSubFragments-1]=new byte[lastPieceSize];
			}
			
		}
		this.isDownloaded= new boolean[numberOfSubFragments];
		this.canBeDownloaded= new boolean[numberOfSubFragments];
		
	}
	private int numberOfPieces(int big, int small){
		int result=big/small;
		if(big%small!=0){
			result++;
		}
		return result;
	}
	/**
	 * returns:
	 * first position of the array=piece index
	 * second position: block offset (the beginning of the subfragment)
	 * third position: block length
	 * @return null if there is not piece to download
	 */
	public synchronized int[] pieceToAsk(){
		int blockToDownload=-1;
		for(int i=0,ii=isDownloaded.length;i<ii&&blockToDownload==-1;i++){
			if(isDownloaded[i]){
				blockToDownload=i;
			}
		}
		int[]pieceInformation=null;
		if(blockToDownload!=-1){
			pieceInformation=new int[3];
			pieceInformation[0]=this.currentFragment;
			pieceInformation[1]=blockToDownload*this.subfragmentLength;
			pieceInformation[2]=this.subfragmentLength;
		}		
		return pieceInformation;
	}
	
	/**
	 * 
	 * @param piece
	 * @param piecePosition
	 * @return true if the thread must notify that it has the piece
	 */
	public synchronized boolean addPieceToArray(byte[]piece, int piecePosition){
		this.downloadingFragments[piecePosition]=piece;
		this.isDownloaded[piecePosition]=true;
		boolean notify=false;
		// comprobar si se completa el fragmento
		if(this.isCompleted()){
			//validar hash 
			if(this.validateHash()){
				//guardar en fichero
				this.saveToFile();
				this.currentFragment++;
				initializeSubFragments();
				notify=true;
			}else{
				initializeSubFragments();
			}
		}
		return notify;
		
	}
	public synchronized boolean isCompleted(){
		boolean isCompleted=true;
		for(int i=0,ii=isDownloaded.length;i<ii&&isCompleted;i++){
			if(!isDownloaded[i]){
				isCompleted=false;
			}
		}
		return isCompleted;
	}
	public boolean isLastPiece(){
		return currentFragment+1!=numberOfFragments;
	}
	public synchronized boolean beginFragmentDownload(int fragmentPos){
		if(this.canBeDownloaded[fragmentPos]){
			this.canBeDownloaded[fragmentPos]=false;
			return true;
		}else{
			return false;
		}
	}
	
	
	/*METODOS SIN IMPLEMENTAR*/
	private boolean validateHash(){
		return false;
	}
	private void saveToFile(){
		
	}
	
}
