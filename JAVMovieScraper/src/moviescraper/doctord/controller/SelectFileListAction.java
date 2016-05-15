package moviescraper.doctord.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.IOUtils;
import moviescraper.doctord.controller.xmlserialization.KodiXmlMovieBean;
import moviescraper.doctord.model.Movie;
import moviescraper.doctord.model.MovieFactory;
import moviescraper.doctord.model.dataitem.Thumb;
import moviescraper.doctord.view.GUIMain;

public class SelectFileListAction implements ListSelectionListener {
	/**
	 * 
	 */
	private final GUIMain guiMain;

	/**
	 * @param guiMain
	 */
	public SelectFileListAction(GUIMain guiMain) {
		this.guiMain = guiMain;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {

			if (this.guiMain.getFileList().getSelectedIndex() == -1) {
				// No selection
				// Clear out old selection references
				this.guiMain.removeOldSelectedFileReferences();

			} else {
				//totally new selection
				this.guiMain.removeOldSelectedFileReferences();

				
				for(File currentSelectedFile : this.guiMain.getFileList().getSelectedValuesList())
				{
					this.guiMain.getCurrentlySelectedNfoFileList().add(new File(Movie
							.getFileNameOfNfo(currentSelectedFile, this.guiMain.getPreferences().getNfoNamedMovieDotNfo())));
					this.guiMain.getCurrentlySelectedPosterFileList().add(new File(Movie
							.getFileNameOfPoster(currentSelectedFile, this.guiMain.getPreferences().getNoMovieNameInImageFiles())));
					this.guiMain.getCurrentlySelectedFolderJpgFileList().add(new File(Movie
							.getFileNameOfFolderJpg(currentSelectedFile)));
					this.guiMain.getCurrentlySelectedFanartFileList().add(new File(Movie
							.getFileNameOfFanart(currentSelectedFile, this.guiMain.getPreferences().getNoMovieNameInImageFiles())));
					this.guiMain.getCurrentlySelectedTrailerFileList().add(new File(Movie.getFileNameOfTrailer(currentSelectedFile)));
				}

				this.guiMain.debugWriter("nfos after selection: " + this.guiMain.getCurrentlySelectedNfoFileList());
				this.guiMain.debugWriter("posters after selection: " + this.guiMain.getCurrentlySelectedPosterFileList());
				this.guiMain.debugWriter("folderjpgs after selection: " + this.guiMain.getCurrentlySelectedFolderJpgFileList());
				this.guiMain.debugWriter("fanartfiles after selection: " + this.guiMain.getCurrentlySelectedFanartFileList());
				this.guiMain.debugWriter("trailer after selection: " + this.guiMain.getCurrentlySelectedTrailerFileList());


				this.guiMain.setCurrentlySelectedMovieFileList(this.guiMain.getFileList().getSelectedValuesList());



				this.guiMain.updateActorsFolder();

				// clean up old scraped movie results from previous selection
				//this.guiMain.removeOldScrapedMovieReferences();

				//this method is rather ineffecient - it has to reread the entire list's nfo every time
				//instead of just reading ones it doesn't already have!
				boolean readInAnInfo = false;
				for(File currentSelectedFile : this.guiMain.getFileList().getSelectedValuesList())
				{
					File potentialNfoFile = new File(Movie.getFileNameOfNfo(currentSelectedFile, this.guiMain.getPreferences().getNfoNamedMovieDotNfo()));
					List<File> nfoList = guiMain.getCurrentlySelectedNfoFileList();
					int potentialIndex = nfoList.indexOf(potentialNfoFile);
					if(potentialIndex != -1)
					{
						if (nfoList.get(potentialIndex).exists()) {
							readMovieFromNfoFile(guiMain.getCurrentlySelectedNfoFileList().get(potentialIndex));
							readInAnInfo = true;
						}
					}

				}
				if(!readInAnInfo)
				{
					guiMain.movieToWriteToDiskList.add(MovieFactory.createEmptyMovie());
					this.guiMain.updateAllFieldsOfFileDetailPanel(false, false);
				}
				this.guiMain.debugWriter("currentlySelectedMovieFileList: " + guiMain.getCurrentlySelectedMovieFileList());
				this.guiMain.debugWriter("movieToWriteToDiskList: " + guiMain.movieToWriteToDiskList.size());
				
				//Update gui with whether we have a valid movie to write to disk
				if(shouldFileWritingBeEnabled()) {
					guiMain.enableFileWrite();
				}
				else {
					guiMain.disableFileWrite();
				}
			}
		}
	}
	
	/**
	 * 
	 * @return true if any movie in the movie list has a title at least one letter long
	 */
	private boolean shouldFileWritingBeEnabled() {
		boolean fileWritingEnabled = false;
		for(Movie currentMovie : guiMain.movieToWriteToDiskList) {
			if(currentMovie != null && currentMovie.getTitle() != null && currentMovie.getTitle().getTitle() != null && currentMovie.getTitle().getTitle().length() > 0) {
				fileWritingEnabled = true;
				break;
			}
		}
		return fileWritingEnabled;
	}
	
	protected void readMovieFromNfoFile(File nfoFile) {
		FileInputStream fisTargetFile = null;
		try {
			fisTargetFile = new FileInputStream(nfoFile);
			String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
			//Sometimes there's some junk before the prolog tag. Do a workaround to remove that junk.
			//This really isn't the cleanest way to do this, but it'll work for now
			//check first to make sure the string even contains <?xml so we don't loop through an invalid file needlessly
			if(targetFileStr.contains("<?xml"))
			{
				while(targetFileStr.length() > 0 && !targetFileStr.startsWith("<?xml"))
				{
					if(targetFileStr.length() > 1)
					{
						targetFileStr = targetFileStr.substring(1,targetFileStr.length());
					}
					else break;
				}
			}
			KodiXmlMovieBean xmlMovieBean = KodiXmlMovieBean.makeFromXML(targetFileStr);
			if(xmlMovieBean != null)
			{
				Movie movieFromNfo = xmlMovieBean.toMovie();
				guiMain.movieToWriteToDiskList.add(movieFromNfo);
				if (guiMain.getCurrentlySelectedPosterFileList().get(0).exists()) {
					//we don't want to resize this poster later
					Thumb[] currentPosters = guiMain.movieToWriteToDiskList.get(0).getPosters();
					File currentPosterFile = guiMain.getCurrentlySelectedPosterFileList().get(0);
					Thumb thumbFromDisk;
					if(currentPosterFile != null)
						{
						if(currentPosters.length > 0 && guiMain.getCurrentlySelectedPosterFileList().size() > 0 && currentPosters[0] != null && currentPosters[0].getThumbURL() != null)
							thumbFromDisk = new Thumb(guiMain.getCurrentlySelectedPosterFileList().get(0), currentPosters[0].getThumbURL().toString());
						else
						{
							thumbFromDisk = new Thumb(guiMain.getCurrentlySelectedPosterFileList().get(0));
							currentPosters = new Thumb[1];
						}
						currentPosters[0] = thumbFromDisk;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				fisTargetFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally
			{
				guiMain.updateAllFieldsOfFileDetailPanel(false, false);
			}
		}
	}
}