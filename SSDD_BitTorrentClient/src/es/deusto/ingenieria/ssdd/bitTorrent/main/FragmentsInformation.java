package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.UnsupportedEncodingException;
import java.util.List;

import es.deusto.ingenieria.ssdd.bitTorrent.util.StringUtils;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;


public class FragmentsInformation {
	//-1 if is finished
	private int currentFragment;
	private byte[][]downloadingFragments;
	private boolean[]isDownloaded;
	//estas posiciones se ponen a true cuando alguien EMPIEZA a descargar un fragmento
	private boolean[]canBeDownloaded;
	private int fragmentLength;
	private int fileLength;
	private int subfragmentLength;
	private int numberOfFragments;
	private List<byte[]>pieceHashes;
	public FragmentsInformation(int fileLength, int fragmentLength, int subfragmentLength, int currentFragment, List<byte[]>pieceHashes){
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
		if(currentFragment+1>numberOfFragments||currentFragment<-1){
			//the file is completed
			this.downloadingFragments=null;
			this.isDownloaded=null;
			this.canBeDownloaded=null;
			
		}else{
			if(!this.isLastPiece()){
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
			for(int i=0,ii=canBeDownloaded.length;i<ii;i++){
				canBeDownloaded[i]=true;
			}
		}
		
		
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
			if(!isDownloaded[i]){
				blockToDownload=i;
			}
		}
		int[]pieceInformation=null;
		if(blockToDownload!=-1){
			pieceInformation=new int[3];
			pieceInformation[0]=this.currentFragment;
			pieceInformation[1]=blockToDownload*this.subfragmentLength;
			pieceInformation[2]=this.downloadingFragments[blockToDownload].length;
		}		
		return pieceInformation;
	}
	
	/**
	 * 
	 * @param piece
	 * @param piecePosition
	 * @return true if the thread must notify that it has the piece
	 */
	public synchronized boolean addPieceToArray(byte[]piece, int pieceBeginingPosition){
		int piecePosition=pieceBeginingPosition/this.subfragmentLength;
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
	/**
	 * Indicates if the current fragment is completed
	 * @return
	 */
	public synchronized boolean isCompleted(){
		boolean isCompleted=true;
		for(int i=0,ii=isDownloaded.length;i<ii&&isCompleted;i++){
			if(!isDownloaded[i]){
				isCompleted=false;
			}
		}
		return isCompleted;
	}
	/**
	 * Indicates if the current fragment is the last
	 * @return
	 */
	public synchronized boolean isLastPiece(){
		return currentFragment+1==numberOfFragments;
	}
	
	public boolean downloadFinished(){
		boolean finished=false;
		if(currentFragment<0||(this.isLastPiece()&&this.isCompleted())){
			finished=true;
			currentFragment=-1;
		}
		return finished;
	}
	
	/**
	 * 
	 * Blocks a fragment when is going to begin its download so another thread
	 * doesn't start the same fragments download
	 * @param fragmentPos
	 * @return
	 */
	public synchronized boolean blockFragment(int fragmentBeginningPos){
		int fragmentPos=fragmentBeginningPos/this.subfragmentLength;
		if(this.isDownloaded[fragmentPos]){
			this.canBeDownloaded[fragmentPos]=false;
			return false;
		}else{
			if(this.canBeDownloaded[fragmentPos]){
				this.canBeDownloaded[fragmentPos]=false;
				return true;
			}else{
				return false;
			}
		}
	}
	/**
	 * Unblocks a fragment if the thread could not download it
	 * @param fragmentPos
	 */
	public synchronized void unblockFragment(int fragmentBeginningPos){
		int fragmentPos=fragmentBeginningPos/this.subfragmentLength;
		if(!this.isDownloaded[fragmentPos]){
			this.canBeDownloaded[fragmentPos]=true;
		}
	}
	
	
	/*METODOS SIN IMPLEMENTAR*/
	private boolean validateHash(){
		byte[]fullArray;
		if(this.isLastPiece()){
			fullArray=new byte[this.fileLength%this.fragmentLength];
		}else{
			fullArray=new byte[this.fragmentLength];
		}
		//concatenate the subfragments
		for(int i=0,ii=this.downloadingFragments.length;i<ii;i++){
			for(int j=0,jj=this.downloadingFragments[i].length;j<jj;j++){
				fullArray[(i*this.subfragmentLength)+j]=downloadingFragments[i][j];
			}
		}
		byte[]generatedHash=ToolKit.generateSHA1Hash(fullArray);
		String generatedHashString=StringUtils.toHexString(generatedHash);
		byte[]goodHash=this.pieceHashes.get(currentFragment);
		String goodHashString=StringUtils.toHexString(goodHash);
		if(generatedHash.equals(goodHash)){
			return true;
		}
		return false;
	}
	private void saveToFile(){
		
	}
	
}