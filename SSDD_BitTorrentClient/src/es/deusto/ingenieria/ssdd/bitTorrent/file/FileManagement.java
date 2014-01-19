package es.deusto.ingenieria.ssdd.bitTorrent.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import es.deusto.ingenieria.ssdd.bitTorrent.util.ToolKit;

/**
 * This class enables the reading and writing of the file. a
  */
public class FileManagement {

	private String fileName;
	private RandomAccessFile randomAccessFile;
	private int fileLength;
	public FileManagement(String fileName, int fileLength){
		this.fileLength= fileLength;
		this.fileName= "data/"+fileName;
	}
	
	/**
	 * This method store bytes in the file.
	 * @param position corresponds to the start point to write bytes.
	 * @param bytesToStore array of bytes to store in the file
	 */
	public void storeInFile(long position, byte[] bytesToStore){
		
		try {
			// Open the file with read and write permissions.
			randomAccessFile = new RandomAccessFile (this.fileName, "rw");
			//Set the length of the file
			randomAccessFile.setLength(fileLength);
			//Go to the position to start writing
			randomAccessFile.seek(position);
			// Write the bytes
			randomAccessFile.write(bytesToStore);
			//close the file
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			
		} 
	}
	
	/**
	 * This method store the bytes in the file and at the end one integer that indicates the 
	 * last fragment downloaded
	 * @param position corresponds to the start point to write bytes.
	 * @param bytesToStore array of bytes to store in the file
	 * @param last integer that indicates the last download fragment
	 */
	public void storeInFileWithLast(int position, byte[] bytesToStore,int last){
		storeInFile(position,bytesToStore);
		storeInFile(this.fileLength-4,ToolKit.intToBigEndianBytes(last, new byte[4], 0));
	}
	
	/**
	 * This method read bytes from the file. 
	 * @param position the start position to start reading
	 * @param lenght the number of bytes to read
	 * @return array of bytes read
	 */
	public byte[] readFromFile(int position, int lenght){
		byte[] read= new byte[lenght];
		try {
			// open the file with read permission
			randomAccessFile = new RandomAccessFile (this.fileName, "r");
			//Go to the position to start reading
			randomAccessFile.seek(position);
			//read the bytes
			randomAccessFile.read(read, 0, lenght);
			//close the file
			randomAccessFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return read;
	}
	
	/**
	 * This method say if the file is completed or not.
	 * @return true if it is completed and false otherwise
	 */
	public boolean isCompleted(){
		File file = new File(this.fileName);
		if(file.length()==this.fileLength){
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * This method get the last integer from the file that corresponds to the
	 * last download fragment
	 * @return -1 if the file is completed and the last download fragment otherwise
	 */
	public int getCurrentFragment(){
		if(this.isCompleted()){
			return -1;
		}else{
			return ToolKit.bigEndianBytesToInt(readFromFile(this.fileLength-4,4), 0);
		}
	}
	
	/**
	 * This method say if the file exists or not.
	 * @return true if it exists and false otherwise.
	 */
	public boolean exists(){
		File file = new File(this.fileName);
		if(file.exists()){
			return true;
		}else{
			return false;
		}
	}

}
