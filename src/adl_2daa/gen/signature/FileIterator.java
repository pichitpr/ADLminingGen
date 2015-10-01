package adl_2daa.gen.signature;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FileIterator implements Iterator<File>{

	private int pointer = -1;
	private File[] filelist;
	
	public void trackFiles(File dir){
		List<File> buf = new LinkedList<File>();
		trackFilesRecursive(dir, buf);
		filelist = buf.<File>toArray(new File[buf.size()]);
		pointer = -1;
	}
	
	//Traverse the directory and append all files found using DFS
	private void trackFilesRecursive(File file, List<File> buf){
		if(!file.isDirectory()){
			buf.add(file);
			return;
		}
		for(File f : file.listFiles()){
			trackFilesRecursive(f, buf);
		}
	}
	
	public void reset(){
		pointer = -1;
	}
	
	@Override
	public boolean hasNext() {
		return pointer < filelist.length-1;
	}

	@Override
	public File next() {
		return filelist[++pointer];
	}
}
