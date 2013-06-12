package mvbrowser;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class MovieBrowserFrame extends JFrame
{
	protected JProgressBar	progressBar;

	protected ImagePanel	coverImgPanel;
	protected JList<MovieData>	movieList;

	protected String		VLC_EXE			= "C:/Program Files (x86)/VideoLAN/VLC/vlc.exe";
	protected String[]		VID_EXTENSIONS	= new String[] { "avi", "m4v", "m2ts", "mp4" };

	protected int			COVER_W			= 460;
	protected int			COVER_H			= 680;

	protected int			movieCount		= 0;

	public MovieBrowserFrame(String folderStr)
	{
		super("Movie Browser");

		setUndecorated(true);
		setExtendedState(Frame.MAXIMIZED_BOTH);
		setLayout(new BorderLayout());

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				if(e.getID() == KeyEvent.KEY_PRESSED)
				{
					// pressed enter
					switch(e.getKeyCode())
					{
					case 10: // ENTER
						MovieData selectedMovie = (MovieData) movieList.getSelectedValue();
						if(selectedMovie != null)
							try
							{
								if(!selectedMovie.videoFile.isEmpty())
									Runtime.getRuntime().exec(VLC_EXE + " --fullscreen \"" + selectedMovie.videoFile + "\"");
								else
									JOptionPane.showMessageDialog(MovieBrowserFrame.this, "Couldn't locate video file for " + selectedMovie.title, "Whoops!", JOptionPane.WARNING_MESSAGE);
							}
							catch(IOException e1)
							{
								e1.printStackTrace();
							}
						break;
					case 27: // RIGHT
						System.exit(1);
						break;
					case 37: // LEFT
						movieList.setSelectedIndex((movieList.getSelectedIndex() + movieCount - 1) % movieCount);
						break;
					case 39: // RIGHT
						movieList.setSelectedIndex((movieList.getSelectedIndex() + 1) % movieCount);
						break;
					}
				}
				return false;
			}
		});

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		File movieFolder = null;
		if(!folderStr.isEmpty())
		{
			movieFolder = new File(folderStr);
			if(!movieFolder.exists())
			{
				movieFolder = null;
				JOptionPane.showMessageDialog(MovieBrowserFrame.this, "Couldn't locate movie folder: " + folderStr, "Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		if(movieFolder == null)
		{
			JFileChooser folderChooser = new JFileChooser();
			folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = folderChooser.showDialog(this, "Select");
			if(result == JFileChooser.APPROVE_OPTION)
				movieFolder = folderChooser.getSelectedFile();
			else
				System.exit(1);
		}

		// Cover Image Panel

		final JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());

		coverImgPanel = new ImagePanel();

		rightPanel.add(coverImgPanel, BorderLayout.CENTER);

		// Movie Title Panel

		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		final ArrayList<MovieData> movieFolders = new ArrayList<MovieData>();

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		final JFrame progressFrame = new JFrame();
		progressFrame.setUndecorated(true);
		progressFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		progressFrame.setSize(300, 30);
		progressFrame.setLocationRelativeTo(null);

		progressFrame.getContentPane().add(progressBar);

		final File movieFolderList[] = movieFolder.listFiles();

		SwingWorker<Void, Void> loadTask = new SwingWorker<Void, Void>()
		{
			@Override
			public Void doInBackground()
			{
				progressFrame.setVisible(true);

				float progress = 0;
				float increment = 100f / movieFolderList.length;

				setProgress(0);

				for(File f : movieFolderList)
					if(f.isDirectory())
					{
						movieFolders.add(new MovieData(f));
						movieCount++;
						progress += increment;
						progressBar.setString(f.getName());
						setProgress((int) Math.min(progress, 100));
					}

				return null;
			}

			@Override
			public void done()
			{
				progressFrame.dispose();

				MovieData[] movieDataArray = new MovieData[0];
				movieList = new JList<MovieData>(movieFolders.toArray(movieDataArray));
				movieList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				movieList.addListSelectionListener(new ListSelectionListener()
				{
					@Override
					public void valueChanged(ListSelectionEvent e)
					{
						if(!e.getValueIsAdjusting())
						{
							MovieData selectedMovie = (MovieData) movieList.getSelectedValue();
							coverImgPanel.setMovie(selectedMovie);
							movieList.ensureIndexIsVisible(movieList.getSelectedIndex());
						}
					}
				});
				movieList.setSelectedIndex(0);

				leftPanel.add(new JScrollPane(movieList), BorderLayout.CENTER);

				add(leftPanel, BorderLayout.WEST);
				add(rightPanel, BorderLayout.CENTER);

				setVisible(true);
			}
		};
		loadTask.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if("progress" == evt.getPropertyName())
				{
					int progress = (Integer) evt.getNewValue();
					progressBar.setValue(progress);
					progressBar.repaint();
				}
			}
		});
		loadTask.execute();
	}

	class MovieData
	{
		protected File		folder;
		protected String	title;
		protected String	year;
		protected Image		coverImage;
		protected Image		backdropImage;
		protected String	videoFile;

		private Pattern		p	= Pattern.compile("(.*)\\(([0-9]{4})\\).*");

		public MovieData(File folder)
		{
			this.folder = folder;

			String name = folder.getName();

			// get movie title and year
			Matcher m = p.matcher(name);

			if(m.matches())
			{
				title = m.group(1);
				year = m.group(2);
			}
			else
			{
				// set default movie title and year
				title = name;
				year = "";
			}

			// get cover image, backdrop image, and video file
			videoFile = "";
			for(File f : folder.listFiles())
			{
				try
				{
					if(f.getName().equalsIgnoreCase("folder.jpg"))
					{
						coverImage = new ImageIcon(f.toURI().toURL()).getImage();
					}
					else if(f.getName().equalsIgnoreCase("backdrop.jpg"))
					{
						backdropImage = new ImageIcon(f.toURI().toURL()).getImage();
					}
					else
					{
						for(String ext : VID_EXTENSIONS)
							if(f.getName().toLowerCase().endsWith("." + ext))
							{
								videoFile = f.getAbsolutePath();
							}
					}
				}
				catch(MalformedURLException e)
				{
				}
			}
		}

		public String toString()
		{
			return title;
		}
	}

	class ImagePanel extends JPanel
	{
		private Image		backgroundImage;
		private Image		videoPresentImage;
		private MovieData	movie;
		private float		reflection_transparency	= .25f;
		private float		backdrop_transparency	= .50f;
		private float		title_transparency		= .7f;

		public ImagePanel()
		{
			backgroundImage = new ImageIcon(ClassLoader.getSystemResource("background.jpg")).getImage();
			videoPresentImage = new ImageIcon(ClassLoader.getSystemResource("videoPresent.png")).getImage();
		}

		public void setMovie(MovieData movie)
		{
			this.movie = movie;
			setLayout(null);
			repaint();
		}

		public void paintComponent(Graphics g)
		{
			super.paintComponents(g);
			Graphics2D g2 = (Graphics2D) g;

			Composite comp = g2.getComposite();

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(Color.black);

			g2.fillRect(0, 0, getWidth(), getHeight());

			if(movie != null && movie.backdropImage != null)
			{
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, backdrop_transparency));
				g2.drawImage(movie.backdropImage, 0, 0, getWidth(), getHeight(), null);
				g2.setComposite(comp);
			}
			else if(backgroundImage != null)
			{
				g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
			}

			if(movie != null)
			{
				drawCover(g2, movie.coverImage, COVER_W, COVER_H, 0, 0, 0);

				if(!movie.videoFile.isEmpty())
				{
					g2.drawImage(videoPresentImage, getWidth() - 60, 12, 48, 48, null);
				}

				g2.setColor(Color.white);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, title_transparency));
				g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
				FontMetrics fm = g2.getFontMetrics();
				g2.drawString(movie.title, (getWidth() - fm.stringWidth(movie.title)) / 2, 100);
				g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
				g2.drawString(movie.year, (getWidth() - fm.stringWidth(movie.year)) / 2, 140);
			}
		}

		private void drawCover(Graphics2D g2, Image coverImage, int width, int height, int x_offset, int y_offset, float shear)
		{
			if(coverImage != null)
			{
				AffineTransform at = g2.getTransform();
				Paint paint = g2.getPaint();
				Composite comp = g2.getComposite();

				g2.drawImage(coverImage, (getWidth() / 2) - (width / 2) + x_offset, (getHeight() / 2) - (height / 2) + y_offset, width, height, null);

				g2.translate(0, (getHeight() / 2) + (height / 4));
				g2.scale(1, -1);
				g2.translate(0, -(getHeight() / 2) - (height / 4));
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, reflection_transparency));
				g2.drawImage(coverImage, (getWidth() / 2) - (width / 2) + x_offset, (getHeight() / 2) - height + y_offset, width, height, null);

				g2.setTransform(at);
				g2.setPaint(paint);
				g2.setComposite(comp);
			}
		}
		// public void paintComponent(Graphics g)
		// {
		// super.paintComponents(g);
		// Graphics2D g2 = (Graphics2D) g;
		//
		// AffineTransform at = g2.getTransform();
		// Paint paint = g2.getPaint();
		// Composite comp = g2.getComposite();
		//
		// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		//
		// g2.setColor(Color.black);
		//
		// g2.fillRect(0, 0, getWidth(), getHeight());
		//
		// if(movie != null && movie.backdropImage != null)
		// {
		// g2.setComposite(AlphaComposite.getInstance(
		// AlphaComposite.SRC_OVER, backdrop_transparency));
		// g2.drawImage(movie.backdropImage, 0, 0, getWidth(), getHeight(),
		// null);
		// g2.setComposite(comp);
		// }
		// else if (backgroundImage != null)
		// {
		// g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(),
		// null);
		// }
		//
		// if (movie != null)
		// {
		// if (movie.coverImage != null)
		// {
		// // g2.shear(0, -.25);
		//
		// g2.drawImage(movie.coverImage, (getWidth() / 2)
		// - (COVER_W / 2), (getHeight() / 2) - (COVER_H / 2),
		// COVER_W, COVER_H, null);
		//
		// g2.translate(0, (getHeight() / 2) + (COVER_H / 4));
		// g2.scale(1, -1);
		// g2.translate(0, -(getHeight() / 2) - (COVER_H / 4));
		// g2.setComposite(AlphaComposite.getInstance(
		// AlphaComposite.SRC_OVER, reflection_transparency));
		// g2.drawImage(movie.coverImage, (getWidth() / 2)
		// - (COVER_W / 2), (getHeight() / 2) - (COVER_H),
		// COVER_W, COVER_H, null);
		//
		// g2.setTransform(at);
		// g2.setPaint(paint);
		// g2.setComposite(comp);
		// }
		//
		// if (movie.next.coverImage != null)
		// {
		// g2.shear(0, -.25);
		//
		// g2.drawImage(movie.next.coverImage, (getWidth() / 2)
		// - (int)((COVER_W*next_cover_scale / 2)), (getHeight() / 2) -
		// (int)((COVER_H*next_cover_scale / 2)),
		// (int)(COVER_W*next_cover_scale), (int)(COVER_H*next_cover_scale),
		// null);
		//
		// g2.translate(0, (getHeight() / 2) + (COVER_H*next_cover_scale / 4));
		// g2.scale(1, -1);
		// g2.translate(0, -(getHeight() / 2) - (COVER_H*next_cover_scale / 4));
		// g2.setComposite(AlphaComposite.getInstance(
		// AlphaComposite.SRC_OVER, reflection_transparency));
		// g2.drawImage(movie.next.coverImage, (getWidth() / 2)
		// - (int)((COVER_W*next_cover_scale / 2)), (getHeight() / 2) -
		// (int)((COVER_H*next_cover_scale)),
		// (int)(COVER_W*next_cover_scale), (int)(COVER_H*next_cover_scale),
		// null);
		//
		// g2.setTransform(at);
		// g2.setPaint(paint);
		// g2.setComposite(comp);
		// }
		//
		//
		// if (!movie.videoFile.isEmpty())
		// {
		// g2.drawImage(videoPresentImage, getWidth() - 60, 12, 48,
		// 48, null);
		// }
		//
		// g2.setColor(Color.white);
		// g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
		// FontMetrics fm = g2.getFontMetrics();
		// g2.setComposite(AlphaComposite.getInstance(
		// AlphaComposite.SRC_OVER, title_transparency));
		// g2.drawString(movie.title,
		// (getWidth() - fm.stringWidth(movie.title)) / 2,
		// getHeight() - 30);
		// }
		// }
	}
}
