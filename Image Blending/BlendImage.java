import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
// Main class
public class BlendImage extends Frame implements ActionListener{
	CanvasImage input, output;
	BufferedImage photo1, photo2, mask1, mask2;
	int rows = 1, columns = 2, horGap = 8, verGap = 8; //Declaring Grid Layout Parameters for easier access
	// Constructor
	public BlendImage(String p1, String p2, String m1, String m2) {
		super("Assignment 1 - Image Blending - Amal Thomas");
		photo1 = loadImageFile(p1);
		photo2 = loadImageFile(p2);
		mask1 = loadImageFile(m1);
		mask2 = loadImageFile(m2);
		// creating the canvas
		Panel main = new Panel();
		input = new CanvasImage(photo1);
		output = new CanvasImage(photo2);
		main.setLayout(new GridLayout(rows, columns, horGap, verGap));
		main.add(input);
		main.add(output);
		// add canvas and control panel
		Panel controls = new Panel();
		Button button = new Button("Photos");
		
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Weight Maps");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Feathering");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Laplacian Pyramids");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Pyramid Blending");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Image Laplacians");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Poisson Editing");
		button.addActionListener(this);
		controls.add(button);
		// add two panels
		add("Center", main);
		add("South", controls);
		addWindowListener(new ExitListener());
		// int width = photo1.getWidth();
		// int height = photo1.getHeight();
		int imageHeight = photo1.getHeight();
		int imageWidth = photo1.getWidth();
		int totalCanvasHeight = imageHeight + (imageHeight/2) + (imageHeight/(2*2)) + (imageHeight/(2*2*2)) + (imageHeight/(2*2*2*2));
		int totalCanvasWidth = imageWidth + (imageWidth/2) + (imageWidth/(2*2)) + (imageWidth/(2*2*2)) + (imageWidth/(2*2*2*2));
		totalCanvasWidth = totalCanvasWidth + (10*5);
		setSize(totalCanvasWidth*2+50, totalCanvasHeight+80);
		setVisible(true);
	}
	class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}

	// load image
	BufferedImage loadImageFile(String file) {
		Image image = Toolkit.getDefaultToolkit().getImage(file);
		MediaTracker mt = new MediaTracker(this);
		try {
			mt.addImage(image, 0);
			mt.waitForID(0);
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		// convert to buffered image
		BufferedImage data = new BufferedImage
			(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = data.createGraphics();
		g2D.drawImage(image, 0, 0, null);
		return data;
	}

	// Action listener for buttons
	public void actionPerformed(ActionEvent e) {
		if ( ((Button)e.getSource()).getLabel().equals("Photos") ) {
			input.resetImage(photo1);
			output.resetImage(photo2);
		} else if ( ((Button)e.getSource()).getLabel().equals("Weight Maps") ) {
			input.resetImage(mask1);
			output.resetImage(mask2);
		} else if ( ((Button)e.getSource()).getLabel().equals("Feathering") ) {
			input.resetImage(photo1);
			output.feather(photo1, photo2, mask1, mask2);
		} else if ( ((Button)e.getSource()).getLabel().equals("Laplacian Pyramids") ){
			output.laplacian_pyramid(photo1, photo2, mask1, mask2);
		} else if ( ((Button)e.getSource()).getLabel().equals("Pyramid Blending") ){
			input.pyramid_blending_levels(photo1, photo2, mask1, mask2);
			output.pyramid_blending_levels_out(photo1, photo2, mask1, mask2);
		} else if ( ((Button)e.getSource()).getLabel().equals("Image Laplacians") ){
			input.image_laplacian(photo1);
			output.image_laplacian(photo2);
		}else if ( ((Button)e.getSource()).getLabel().equals("Poisson Editing") ){
			input.poisson_editing_weighted_average(photo1, photo2, mask1, mask2);
			// BufferedImage map = new BufferedImage(photo1.getWidth(), photo1.getHeight(), BufferedImage.TYPE_INT_RGB);
			output.get_jacobi(photo1, photo2, mask1, mask2, photo2);
		}
	}
	//Main Method: Entry point for the java program. Parses command line arguments and stores into a String array
	public static void main(String[] args) {
		BlendImage window;
		if ( args.length == 4 )
			window = new BlendImage(args[0], args[1], args[2], args[3]);
		else
			window = new BlendImage("boat.png", "lake.png", "boat_weight.png", "lake_weight.png");
	}
}
