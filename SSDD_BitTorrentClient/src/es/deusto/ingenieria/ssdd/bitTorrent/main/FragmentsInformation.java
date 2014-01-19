package es.deusto.ingenieria.ssdd.bitTorrent.main;

import java.io.UnsupportedEncodingException;
import java.util.List;
import es.deusto.ingenieria.ssdd.bitTorrent.file.FileManagement;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

/**
 * Class with all the important information of the downloading file
 * @author JulenTV
 *
 */
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
	private String fileName;
	public FragmentsInformation(int fileLength, int fragmentLength, int subfragmentLength, int currentFragment, List<byte[]>pieceHashes, String fileName){
			this.fileLength=fileLength;
			this.fragmentLength=fragmentLength;
			this.subfragmentLength=subfragmentLength;
			this.currentFragment=currentFragment;
			this.numberOfFragments=numberOfPieces(fileLength, fragmentLength);
			this.pieceHashes=pieceHashes;
			this.fileName=fileName;
			initializeSubFragments();
	}
	private synchronized void initializeSubFragments(){
		int numberOfSubFragments=0;
		if(currentFragment+1>numberOfFragments||currentFragment<0){
			//the file is completed
			this.downloadingFragments=null;
			this.isDownloaded=null;
			this.canBeDownloaded=null;
			currentFragment=-1;
			
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
	public int getCurrentFragment() {
		return currentFragment;
	}
	
	public int getNumberOfFragments() {
		return numberOfFragments;
	}
	
	public int getFragmentLength() {
		return fragmentLength;
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
		System.out.println("Añadiendo al fragmento: ["+this.currentFragment+"/"+(this.numberOfFragments-1)+"] el subfragmento: ["+piecePosition+"/"+(downloadingFragments.length-1)+"]");
		// comprobar si se completa el fragmento
		if(this.isCompleted()){
			System.out.println("Fragmento completado");
			//validar hash
			byte[]fullArray=concatSubFragments();
			if(this.validateHash(fullArray)){
				//guardar en fichero
				this.saveToFile(fullArray);
				System.out.println("Guardado en fichero el fragmento: "+this.currentFragment);
				this.currentFragment=this.currentFragment+1;
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
		int ii=isDownloaded==null?-1:isDownloaded.length;
		for(int i=0;i<ii&&isCompleted;i++){
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
	
	/**
	 * Indicates if the download is finished
	 * @return
	 */
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
	
	/**
	 * Generates a hash from the byte array obtain as a parameter. Then validates it with the one 
	 * obtain from the torrent file
	 * @param fullArray
	 * @return true if the hashes are equals.
	 */
	private boolean validateHash(byte[]fullArray){
		byte[]generatedHash=ToolKit.generateSHA1Hash(fullArray);
		String generatedHashString=new String(generatedHash);
		try {
			generatedHash=generatedHashString.getBytes("ASCII");
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[]goodHash=this.pieceHashes.get(currentFragment);
		if(new String(generatedHash).equals(new String(goodHash))){
			return true;
		}
		return false;
	}
	
	/**
	 * Concatenates all the subfragments of one fragment in order to generate the fragment.
	 * @return the fragment obtained from all the subfragments
	 */
	private byte[] concatSubFragments(){
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
		
		return fullArray;
	}
	/**
	 * Saves the fragment into the file.
	 * @param bytes corresponding to the fragment to be saved
	 */
	private void saveToFile(byte[]bytes){
		if(!this.isLastPiece()){
			FileManagement fileManagement= new FileManagement(this.fileName, this.fileLength+4);
			fileManagement.storeInFileWithLast(this.currentFragment*this.fragmentLength, bytes,this.currentFragment+1);
		}else{
			FileManagement fileManagement= new FileManagement(this.fileName, this.fileLength);
			fileManagement.storeInFile(this.currentFragment*this.fragmentLength, bytes);
		}
		
	}
	
}
