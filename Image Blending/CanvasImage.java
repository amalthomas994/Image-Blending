import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

// Canvas for image display
class CanvasImage extends Canvas {
	BufferedImage image;
	int rad = 2;
	boolean isHorizontal = true;
	int pyramid_levels = 5;
	float div = 255;
	BufferedImage jacobi_image;
	float smallest_r = 0;
	float smallest_g = 0;
	float smallest_b = 0;
	// BufferedImage blend_pyramids[] = new BufferedImage[pyramid_levels];


	// initialize the image and mouse control
	public CanvasImage(BufferedImage input) {
		image = input;
		addMouseListener(new ClickListener());
	}
	public CanvasImage(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		addMouseListener(new ClickListener());
	}

	// redraw the canvas
	public void paint(Graphics g) {
		// draw boundary
		g.setColor(Color.gray);
		g.drawRect(1, 1, getWidth()-2, getHeight()-2);
		// compute the offset of the image.
		int xoffset = (getWidth() - image.getWidth()) / 2;
		int yoffset = (getHeight() - image.getHeight()) / 2;
		
		g.drawImage(image, xoffset, yoffset, this);
	}

	// change link to image
	public void resetImage(BufferedImage input) {
		image = input;
		repaint();
	}
	// reset an empty image
	public void resetBuffer(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		repaint();
	}
	// reset image based on the input
	public void copyImage(BufferedImage input) {
		System.out.println("ASD");
		Graphics2D g2D = image.createGraphics();
		g2D.drawImage(input, 0, 0, null);
		repaint();
	}

	/*Feathering Function: Called when "Feathering" button clicked.
		Function takes foreground and background RGB pixel values and pre-multiplies the foreground/background mask grayscale values.
		New RGB value is then fed into blending algorithm.
		RGB values divided by total alpha to normalize.
	*/

	public void feather(BufferedImage foreground, BufferedImage background, BufferedImage foreground_mask, BufferedImage background_mask) {
		// float div = 255;
		int imageHeight = background.getHeight();
		int imageWidth = background.getWidth();
		image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		//For all x and y pixels
		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				/* Getting foreground/background RGB values.
					The getRGB function returns an integer pixel value in the default RGB color model.
					This value is passed into the Color function to decompose it into readable R, G, B values.
				*/
				Color foreground_color = new Color(foreground.getRGB(x, y));
				Color background_color = new Color(background.getRGB(x, y));
				
				//Getting foreground/background grayscale values between 0-255. Value is divided by 255 to set in range between 0-1.
				float foreground_alpha = foreground_mask.getRaster().getSample(x, y, 0)/div; 
				float background_alpha = background_mask.getRaster().getSample(x, y, 0)/div;
				
				//Computing total alpha.
				float alpha = background_alpha*(1-foreground_alpha) + foreground_alpha;
				
				//Getting individual foreground R, G, B values from Color object. Each value is divided by 255 to set in range between 0-1.
				float foreground_red = foreground_color.getRed() / div;
				float foreground_green = foreground_color.getGreen() / div;
				float foreground_blue = foreground_color.getBlue() / div;
				
				//Getting individual background R, G, B values from Color object. Each value is divided by 255 to set in range between 0-1.
				float b_r = background_color.getRed() / div;
				float b_g = background_color.getGreen() / div;
				float b_b = background_color.getBlue() / div;

				//Premultiplied Pixel - Background
				float r_b_ = b_r * background_alpha;
				float g_b_ = b_g * background_alpha;
				float b_b_ = b_b * background_alpha;

				//Premultiplied Pixel - Foreground
				float r_f_ = foreground_red * foreground_alpha;
				float g_f_ = foreground_green * foreground_alpha;
				float b_f_ = foreground_blue * foreground_alpha;
				
				//Blending Algorithm
				float rr = r_b_ * (1 - foreground_alpha) + r_f_;
				float gg = g_b_ * (1 - foreground_alpha) + g_f_;
				float bb = b_b_ * (1 - foreground_alpha) + b_f_;

				//Dividing by total alpha to normalize.
				float r = rr / alpha;
				float g = gg / alpha;
				float b = bb / alpha;

				//If R, G, B values add to a value above 1, set to 1.
				if (r > 1){
					r = 1.0f;
				}
				if (g > 1){
					g = 1.0f;
				}
				if (b > 1){
					b = 1.0f;
				}

				// Convert individual R, G, B values into integer pixel value in the default RGB color model.
				int featheredColor = new Color(r, g, b).getRGB();
				// Paint Image.
				image.setRGB(x, y, featheredColor);
			}
		}
		//Refresh window. Paint image onto app.
		repaint();
	}
	
	/*Resize Function:
		Function takes the image to resize, new width, and new height to resize to.
	*/
	public static BufferedImage resize(BufferedImage img, int newW, int newH){
		// Getting a scaled instance of the original image with width "newW" and height "newH".
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_FAST);

		BufferedImage resized_img = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = resized_img.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return resized_img;
	}

	/*Function to blur/smooth the image. Convolution with a Kernel
		Function takes the image to blur. Convolves it with the Average Kernel (Average Filter)
	*/
	public static BufferedImage gaussianBlur(BufferedImage image){
		
		//Average Kernel
		Kernel kernel = new Kernel(3, 3, new float[] {
			1f/10f, 1f/10f, 1f/10f, 
			1f/10f, 1f/10f, 1f/10f, 
			1f/10f, 1f/10f, 1f/10f
		});
		
		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		//Run convolution filter with given image.
		BufferedImage blurredImage = op.filter(image, null);

		return blurredImage;
	}

	public static ConvolveOp getGaussianBlurFilter(int radius,
            boolean horizontal) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }
        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;
        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare)
                    / sigmaRoot;
            total += data[index];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }
        Kernel kernel;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

	/*Function to calculate Prediction Residue for Image/Laplacian Pyramids
		Function takes in the input image at level k image and k-1 approximation.
		The approximation is resized to the same size as the input image at level k.
		An approximation filter is then applied to the resized image.
		The difference between the approximated image and input image is calculated, giving the residue.
	*/
	public BufferedImage calculateResidue(BufferedImage input_image, BufferedImage approximation){
		int imageWidth = input_image.getWidth();
		int imageHeight = input_image.getHeight();
		
		//Resize image
		BufferedImage upsampled_approximation = resize(approximation, imageWidth, imageHeight);
		//Apply blur
		BufferedImage approximated_img = approximationFilter(upsampled_approximation); //Blur
		//Buffered Image to store residue
		BufferedImage residue = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		//For all x and y pixels
		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				
				Color input_image_color = new Color(input_image.getRGB(x, y));
				Color approximation_color = new Color(approximated_img.getRGB(x, y));
				float image_red = input_image_color.getRed() / div;
				float image_green = input_image_color.getGreen() / div;
				float image_blue = input_image_color.getBlue() / div;

				float approximation_red = approximation_color.getRed() / div;
				float approximation_green = approximation_color.getGreen() / div;
				float approximation_blue = approximation_color.getBlue() / div;

				//Subtract input image with approximated image to get R, G, B values for residue image.
				float rr = image_red - approximation_red;
				float gg= image_green - approximation_green;
				float bb = image_blue - approximation_blue;
				
				//Get absolute value of residue pixels since some pixel values become negative during subtraction.
				float r = Math.abs(rr);
				float g = Math.abs(gg);
				float b = Math.abs(bb);
				int residueColor = new Color(r, g, b).getRGB();
				residue.setRGB(x, y, residueColor);
			}
		}
		return residue;
	}
	
	/*Function to add prediction residue with approximated image at level k-1
	*/
	public BufferedImage addResidue(BufferedImage residual, BufferedImage approximation){
		// float div = 255;
		int imageWidth = residual.getWidth();
		int imageHeight = residual.getHeight();
		BufferedImage resized_approximation = resize(approximation, residual.getWidth(), residual.getHeight());
		BufferedImage upsampled_approximation = approximationFilter(resized_approximation); //Blur

		BufferedImage residue = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		
		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				Color residual_color = new Color(residual.getRGB(x, y));
				Color approximation_color = new Color(upsampled_approximation.getRGB(x, y));
				float image_red = residual_color.getRed() / div;
				float image_green = residual_color.getGreen() / div;
				float image_blue = residual_color.getBlue() / div;

				float approximation_red = approximation_color.getRed() / div;
				float approximation_green = approximation_color.getGreen() / div;
				float approximation_blue = approximation_color.getBlue() / div;

				//Add prediction residue R, G, B values with approximation R, G, B values.
				float rr = image_red + approximation_red;
				float gg= image_green + approximation_green;
				float bb = image_blue + approximation_blue;
				
				float r = Math.abs(rr);
				float g = Math.abs(gg);
				float b = Math.abs(bb);
				if (r > 1){
					r = 1.0f;
				}
				if (g > 1){
					g = 1.0f;
				}
				if (b > 1){
					b = 1.0f;
				}
				int residueColor = new Color(r, g, b).getRGB();
				residue.setRGB(x, y, residueColor);
			}
		}
		return residue;
	}
	
	//Approximation Filter function calls the blur function and returns the blurred image.
	public BufferedImage approximationFilter(BufferedImage img){
		BufferedImage approximated_img = gaussianBlur(img);
		return approximated_img;
	}

	/*Function to calculate gaussian pyramid.
		Function takes in the original image and blurs and resizes original input image k and up to total pyramid size.
		Function stores all images into an array so that it can be accessed later.
	*/
	public BufferedImage[] gaussian(BufferedImage img){

		BufferedImage gaussians[] = new BufferedImage[pyramid_levels];
		gaussians[0] = img;
		
		//Approximation: Apply approximation filter (Blur) -> Downsample (Downsize)
		for (int k = 1; k < gaussians.length; k++){
			BufferedImage approximated_img = approximationFilter(gaussians[k-1]); //Blur
			BufferedImage foreground_k1 = resize(approximated_img, gaussians[k-1].getWidth()/2, gaussians[k-1].getHeight()/2); //Downsample
			gaussians[k] = foreground_k1;
		}
		return gaussians;
	}

	/*Function to calculate laplacian pyramid.
		Function takes in the original image and blurs and resizes original input image k and up to total pyramid size.
		Function stores all images into an array so that it can be accessed later.
	*/
	public BufferedImage[] laplacian(BufferedImage[] gaussians){

		BufferedImage laplacians[] = new BufferedImage[pyramid_levels];
		laplacians[gaussians.length-1] = gaussians[gaussians.length-1];
		
		//Prediction Residual: Upsample (Upsize) approximation at k-1 -> Apply approximation filter (Blur) -> Subtract with Gaussian at level k
		for (int k = 0; k < laplacians.length-1; k++){
			BufferedImage prediction_residual = calculateResidue(gaussians[k], gaussians[k+1]);
			laplacians[k] = prediction_residual;
		}
		return laplacians;
	}

	/*Function to draw laplacian/gaussian pyramid*/
	public void laplacian_pyramid(BufferedImage foreground, BufferedImage background, BufferedImage foreground_alpha, BufferedImage background_alpha){
		int imageHeight = background.getHeight();
		int imageWidth = background.getWidth();
		//Calculating total canvas width and height to create required canvas dimensions to fit all pyramid levels
		int totalCanvasHeight = imageHeight + (imageHeight/2) + (imageHeight/(2*2)) + (imageHeight/(2*2*2)) + (imageHeight/(2*2*2*2));
		int totalCanvasWidth = imageWidth + (imageWidth/2) + (imageWidth/(2*2)) + (imageWidth/(2*2*2)) + (imageWidth/(2*2*2*2));
		totalCanvasWidth = totalCanvasWidth + 10*pyramid_levels;

		image = new BufferedImage(totalCanvasWidth, totalCanvasHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2D = image.createGraphics();
		g2D.setPaint(Color.gray);
		g2D.fillRect(0,0, totalCanvasWidth, totalCanvasHeight);

		BufferedImage gaussians[] = gaussian(foreground);
		BufferedImage laplacians[] = laplacian(gaussians);
		int totalWidth = 0;
		int totalWidth_residual = totalCanvasWidth;


		for (int i = 0; i < gaussians.length; i++){
			g2D.drawImage(gaussians[i], totalWidth, totalCanvasHeight-gaussians[i].getHeight(), null); //Draw Approximation at level k-1
			totalWidth = totalWidth + gaussians[i].getWidth()+10;
		}
		for (int i = 0; i < laplacians.length; i++){
			g2D.drawImage(laplacians[i], totalWidth_residual - laplacians[i].getWidth(), 0, null); //Draw Laplacian at level k-1
			totalWidth_residual = totalWidth_residual - laplacians[i].getWidth()-10;
		}

		// //Uncomment the below block to get sum of all laplacians to produce original image.
		// BufferedImage sum = laplacians[laplacians.length - 1];
		// for (int i = laplacians.length - 1; i > 0 ; i--){
		// 	BufferedImage reconstruction = addResidue(laplacians[i-1], sum);
		// 	sum = reconstruction;
		// }
		// g2D.drawImage(sum, totalCanvasWidth/2 - sum.getWidth()/2, totalCanvasHeight/2 - sum.getHeight()/2, null);

		repaint();
	}

	/*Function to perform Laplacian Pyramid Blending:
		Function takes laplacian pyramids, L-A and L-B from foreground and background images.
		Function takes gaussian pyramids, G-A and G-B from foreground and background masks.
		Function calculates L-C at level k using the Pyramid Blending algorithm
	*/
	public BufferedImage blend_pyramid(BufferedImage L_A, BufferedImage L_B, BufferedImage G_A, BufferedImage G_B){
		int imageWidth = L_A.getWidth();
		int imageHeight = L_A.getHeight();
		BufferedImage L_C = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				//Getting color values for forground/background laplacian and forground/background gaussian masks.
				Color L_A_color = new Color(L_A.getRGB(x, y));
				Color L_B_color = new Color(L_B.getRGB(x, y));
				Color G_A_color = new Color(G_A.getRGB(x, y));
				Color G_B_color = new Color(G_B.getRGB(x, y));

				//Dividing by 255 to get values in range of 0-1.
				float L_A_red = L_A_color.getRed() / div;
				float L_A_green = L_A_color.getGreen() / div;
				float L_A_blue = L_A_color.getBlue() / div;

				float L_B_red = L_B_color.getRed() / div;
				float L_B_green = L_B_color.getGreen() / div;
				float L_B_blue = L_B_color.getBlue() / div;

				float G_A_red = G_A_color.getRed() / div;
				float G_A_green = G_A_color.getGreen() / div;
				float G_A_blue = G_A_color.getBlue() / div;

				float G_B_red = G_B_color.getRed() / div;
				float G_B_green = G_B_color.getGreen() / div;
				float G_B_blue = G_B_color.getBlue() / div;

				//Pyramid Blending algorithm
				float L_C_red = ((G_A_red * L_A_red) + (G_B_red * L_B_red))/(G_A_red + G_B_red);
				float L_C_green = ((G_A_green * L_A_green) + (G_B_green * L_B_green))/(G_A_green + G_B_green);
				float L_C_blue = ((G_A_blue * L_A_blue) + (G_B_blue * L_B_blue))/(G_A_blue + G_B_blue);
				
				float r = L_C_red;
				float g = L_C_green;
				float b = L_C_blue;

				int newColor = new Color(r, g, b).getRGB();
				L_C.setRGB(x, y, newColor);

			}
		}

		return L_C;
	}

	/*Function to get all L-C blended pyramids at all levels*/
	public void pyramid_blending_levels(BufferedImage foreground, BufferedImage background, BufferedImage foreground_alpha, BufferedImage background_alpha) {
		int imageHeight = background.getHeight();
		int imageWidth = background.getWidth();
		int totalCanvasHeight = imageHeight + (imageHeight/2) + (imageHeight/(2*2)) + (imageHeight/(2*2*2)) + (imageHeight/(2*2*2*2));
		int totalCanvasWidth = imageWidth + (imageWidth/2) + (imageWidth/(2*2)) + (imageWidth/(2*2*2)) + (imageWidth/(2*2*2*2));
		totalCanvasWidth = totalCanvasWidth + 10*pyramid_levels;

		image = new BufferedImage(totalCanvasWidth, totalCanvasHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2D = image.createGraphics();
		g2D.setPaint(Color.gray);
		g2D.fillRect(0,0, totalCanvasWidth, totalCanvasHeight);

		//Array to store all blended pyramid levels.
		BufferedImage blend_pyramids[] = new BufferedImage[pyramid_levels];

		//Arrays which store the gaussian pyramids for the foreground and background masks.
		BufferedImage G_A[] = gaussian(foreground_alpha);
		BufferedImage G_B[] = gaussian(background_alpha);

		//Arrays which store the gaussian pyramids for the foreground and background images.
		BufferedImage foreground_gaussian[] = gaussian(foreground);
		BufferedImage background_gaussian[] = gaussian(background);

		//Arrays which store the laplacian pyramids for the foreground and background images.
		BufferedImage L_A[] = laplacian(foreground_gaussian);
		BufferedImage L_B[] = laplacian(background_gaussian);

		int totalWidth = 0;

		//Iteratively calculate all blended pyramid levels
		for (int i = blend_pyramids.length - 1; i >=0; i--){
			BufferedImage l_c = blend_pyramid(L_A[i], L_B[i], G_A[i], G_B[i]);
			blend_pyramids[i] = l_c;
		}

		//Draw all images on canvas.
		for (int i = 0; i < blend_pyramids.length; i++){
			g2D.drawImage(blend_pyramids[i], totalWidth, totalCanvasHeight-blend_pyramids[i].getHeight(), null); //Draw Approximation at level k-1
			totalWidth = totalWidth + blend_pyramids[i].getWidth()+10;
		}
		repaint();

	}

	//Function to blend all blended pyramid images to generate final blended output image.
	public void pyramid_blending_levels_out(BufferedImage foreground, BufferedImage background, BufferedImage foreground_alpha, BufferedImage background_alpha) {
		int imageHeight = background.getHeight();
		int imageWidth = background.getWidth();
		int totalCanvasHeight = imageHeight + (imageHeight/2) + (imageHeight/(2*2)) + (imageHeight/(2*2*2)) + (imageHeight/(2*2*2*2));
		int totalCanvasWidth = imageWidth + (imageWidth/2) + (imageWidth/(2*2)) + (imageWidth/(2*2*2)) + (imageWidth/(2*2*2*2));
		totalCanvasWidth = totalCanvasWidth + 10*pyramid_levels;

		image = new BufferedImage(totalCanvasWidth, totalCanvasHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2D = image.createGraphics();
		g2D.setPaint(Color.gray);
		g2D.fillRect(0,0, totalCanvasWidth, totalCanvasHeight);

		BufferedImage blend_pyramids[] = new BufferedImage[pyramid_levels];

		BufferedImage G_A[] = gaussian(foreground_alpha);
		BufferedImage G_B[] = gaussian(background_alpha);

		BufferedImage foreground_gaussian[] = gaussian(foreground);
		BufferedImage background_gaussian[] = gaussian(background);

		BufferedImage L_A[] = laplacian(foreground_gaussian);
		BufferedImage L_B[] = laplacian(background_gaussian);

		int totalWidth = 0;

		for (int i = blend_pyramids.length - 1; i >=0; i--){
			BufferedImage l_c = blend_pyramid(L_A[i], L_B[i], G_A[i], G_B[i]);
			blend_pyramids[i] = l_c;
		}

		for (int i = 0; i < blend_pyramids.length; i++){
			g2D.drawImage(blend_pyramids[i], totalWidth, totalCanvasHeight-blend_pyramids[i].getHeight(), null); //Draw Approximation at level k-1
			totalWidth = totalWidth + blend_pyramids[i].getWidth()+10;
		}
		pyramid_blending(blend_pyramids);

	}

	/*Function to blend all blended pyramid images to generate final blended output image.
		Function will blend add the blended pyramid residues to the lowest level laplacian image in the blended pyramid.
	*/
	public void pyramid_blending(BufferedImage[] b_p) {
		int imageHeight = b_p[0].getHeight();
		int imageWidth = b_p[0].getWidth();
		int totalCanvasHeight = imageHeight + (imageHeight/2) + (imageHeight/(2*2)) + (imageHeight/(2*2*2)) + (imageHeight/(2*2*2*2));
		int totalCanvasWidth = imageWidth + (imageWidth/2) + (imageWidth/(2*2)) + (imageWidth/(2*2*2)) + (imageWidth/(2*2*2*2));
		totalCanvasWidth = totalCanvasWidth + 10*pyramid_levels;

		image = new BufferedImage(totalCanvasWidth, totalCanvasHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2D = image.createGraphics();
		g2D.setPaint(Color.gray);
		g2D.fillRect(0,0, totalCanvasWidth, totalCanvasHeight);

		BufferedImage sum = b_p[b_p.length - 1];
		for (int i = b_p.length - 1; i > 0 ; i--){
			BufferedImage reconstruction = addResidue(b_p[i-1], sum);
			sum = reconstruction;
		}
		g2D.drawImage(sum, totalCanvasWidth/2 - sum.getWidth()/2, totalCanvasHeight/2 - sum.getHeight()/2, null);

		repaint();
	}

	/*Function to generate the laplacian map of a given image*/
	public BufferedImage laplacian_map(BufferedImage img){
		int imageWidth = img.getWidth();
		int imageHeight = img.getHeight();
		
		BufferedImage map = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				Color f_i_j_color = new Color(img.getRGB(x, y));
				float f_i_j_red = f_i_j_color.getRed() / div;
				float f_i_j_green = f_i_j_color.getGreen() / div;
				float f_i_j_blue = f_i_j_color.getBlue() / div;


				float f_i_plus_1_j_red = 0;
				float f_i_plus_1_j_green = 0;
				float f_i_plus_1_j_blue = 0;

				float f_i_minus_1_j_red = 0;
				float f_i_minus_1_j_green = 0;
				float f_i_minus_1_j_blue = 0;

				float f_i_j_plus_1_red = 0;
				float f_i_j_plus_1_green = 0;
				float f_i_j_plus_1_blue = 0;

				float f_i_j_minus_1_red = 0;
				float f_i_j_minus_1_green = 0;
				float f_i_j_minus_1_blue = 0;
				
				//Try Catch used to skip pixels which are non existent, such as pixels (x, y) = (-1, 0)
				try{ 
					Color f_i_plus_1_j_color = new Color(img.getRGB(x+1, y));
					f_i_plus_1_j_red = f_i_plus_1_j_color.getRed() / div;
					f_i_plus_1_j_green = f_i_plus_1_j_color.getGreen() / div;
					f_i_plus_1_j_blue = f_i_plus_1_j_color.getBlue() / div;
				}catch(Exception e){
					// System.out.println("f_i_plus_1_j: x: " + x + " y: " + y);
				}
				
				try{
					Color f_i_minus_1_j_color = new Color(img.getRGB(x-1, y));
					f_i_minus_1_j_red = f_i_minus_1_j_color.getRed() / div;
					f_i_minus_1_j_green = f_i_minus_1_j_color.getGreen() / div;
					f_i_minus_1_j_blue = f_i_minus_1_j_color.getBlue() / div;
				}catch(Exception e){
					// System.out.println("f_i_minus_1_j: x: " + x + " y: " + y);
					
				}

				try{
					Color f_i_j_plus_1_color = new Color(img.getRGB(x, y+1));
					f_i_j_plus_1_red = f_i_j_plus_1_color.getRed() / div;
					f_i_j_plus_1_green = f_i_j_plus_1_color.getGreen() / div;
					f_i_j_plus_1_blue = f_i_j_plus_1_color.getBlue() / div;
				}catch(Exception e){
					// System.out.println("f_i_j_plus_1: x: " + x + " y: " + y);
				}

				try{
					Color f_i_j_minus_1_color = new Color(img.getRGB(x, y-1));
					f_i_j_minus_1_red = f_i_j_minus_1_color.getRed() / div;
					f_i_j_minus_1_green = f_i_j_minus_1_color.getGreen() / div;
					f_i_j_minus_1_blue = f_i_j_minus_1_color.getBlue() / div;
				}catch(Exception e){
					// System.out.println("f_i_j_minus_1: x: " + x + " y: " + y);
				}

				//Laplacian map equation
				float map_red = f_i_plus_1_j_red + f_i_minus_1_j_red + f_i_j_plus_1_red + f_i_j_minus_1_red - (f_i_j_red * 4);
				float map_green = f_i_plus_1_j_green + f_i_minus_1_j_green + f_i_j_plus_1_green + f_i_j_minus_1_green - (f_i_j_green * 4);
				float map_blue = f_i_plus_1_j_blue + f_i_minus_1_j_blue + f_i_j_plus_1_blue + f_i_j_minus_1_blue - (f_i_j_blue * 4);
				
				float r = Math.abs(map_red);
				float g = Math.abs(map_green);
				float b = Math.abs(map_blue);
				
				if (r > 1){
					r = 1f;
				}
				if (g > 1){
					g = 1f;
				}
				if (b > 1){
					b = 1f;
				}
				if (r < 0){
					r = 0f;
				}
				if (g < 0){
					g = 0f;
				}
				if (b < 0){
					b = 0f;
				}
				int newColor = new Color(r, g, b).getRGB();
				map.setRGB(x, y, newColor);
			}
		}

		return map;
	}

	//Function to draw laplacian maps on canvas.
	public void image_laplacian(BufferedImage img) {
		int imageHeight = img.getHeight();
		int imageWidth = img.getWidth();

		image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		image = laplacian_map(img);
		repaint();

	}

	//Function which returns the weighted average of set of images.
	public BufferedImage weighted_average(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask){
		int imageHeight = fg.getHeight();
		int imageWidth = fg.getWidth();
		BufferedImage poisson = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage foreground = fg;
		BufferedImage background = bg;
		

		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				Color foreground_color = new Color(foreground.getRGB(x, y));
				Color background_color = new Color(background.getRGB(x, y));
				
				float foreground_alpha = foreground_mask.getRaster().getSample(x, y, 0)/div;
				float background_alpha = background_mask.getRaster().getSample(x, y, 0)/div;

				float foreground_red = foreground_color.getRed() / div;
				float foreground_green = foreground_color.getGreen() / div;
				float foreground_blue = foreground_color.getBlue() / div;
				
				float b_r = background_color.getRed() / div;
				float b_g = background_color.getGreen() / div;
				float b_b = background_color.getBlue() / div;

				float r = ((foreground_red * foreground_alpha) + (b_r * background_alpha))/(foreground_alpha + background_alpha);
				float g = ((foreground_green * foreground_alpha) + (b_g * background_alpha))/(foreground_alpha + background_alpha);
				float b = ((foreground_blue * foreground_alpha) + (b_b * background_alpha))/(foreground_alpha + background_alpha);

				if (r > 1){
					r = 1.0f;
				}
				if (g > 1){
					g = 1.0f;
				}
				if (b > 1){
					b = 1.0f;
				}

				int featheredColor = new Color(r, g, b).getRGB();

				poisson.setRGB(x, y, featheredColor);

			}
		}
		return poisson;
	}

	//Function to merge foreground and background laplacain images using weighted average.
	public BufferedImage poisson_blend_weighted_average(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask){
		int imageHeight = fg.getHeight();
		int imageWidth = fg.getWidth();
		BufferedImage poisson = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage foreground = laplacian_map(fg);
		BufferedImage background = laplacian_map(bg);
		

		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				Color foreground_color = new Color(foreground.getRGB(x, y));
				Color background_color = new Color(background.getRGB(x, y));
				
				float foreground_alpha = foreground_mask.getRaster().getSample(x, y, 0)/div;
				float background_alpha = background_mask.getRaster().getSample(x, y, 0)/div;
				
				float foreground_red = foreground_color.getRed() / div;
				float foreground_green = foreground_color.getGreen() / div;
				float foreground_blue = foreground_color.getBlue() / div;
				
				float b_r = background_color.getRed() / div;
				float b_g = background_color.getGreen() / div;
				float b_b = background_color.getBlue() / div;

				float r = ((foreground_red * foreground_alpha) + (b_r * background_alpha))/(foreground_alpha + background_alpha);
				float g = ((foreground_green * foreground_alpha) + (b_g * background_alpha))/(foreground_alpha + background_alpha);
				float b = ((foreground_blue * foreground_alpha) + (b_b * background_alpha))/(foreground_alpha + background_alpha);

				if (r > 1){
					r = 1.0f;
				}
				if (g > 1){
					g = 1.0f;
				}
				if (b > 1){
					b = 1.0f;
				}

				int featheredColor = new Color(r, g, b).getRGB();

				poisson.setRGB(x, y, featheredColor);

			}
		}
		return poisson;
	}

	//Function to merge foreground and background laplacain images using feathering algorithm.
	public BufferedImage poisson_blend_feather(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask){
		int imageHeight = fg.getHeight();
		int imageWidth = fg.getWidth();
		BufferedImage poisson = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage foreground = laplacian_map(fg);
		BufferedImage background = laplacian_map(bg);
		

		for (int x = 0; x < imageWidth; x++){
			for (int y = 0; y < imageHeight; y++){
				Color foreground_color = new Color(foreground.getRGB(x, y));
				Color background_color = new Color(background.getRGB(x, y));
				
				float foreground_alpha = foreground_mask.getRaster().getSample(x, y, 0)/div;
				float background_alpha = background_mask.getRaster().getSample(x, y, 0)/div;
				
				float alpha = background_alpha*(1-foreground_alpha) + foreground_alpha;
				
				float foreground_red = foreground_color.getRed() / div;
				float foreground_green = foreground_color.getGreen() / div;
				float foreground_blue = foreground_color.getBlue() / div;
				
				float b_r = background_color.getRed() / div;
				float b_g = background_color.getGreen() / div;
				float b_b = background_color.getBlue() / div;

				float r_b_ = b_r * background_alpha;
				float g_b_ = b_g * background_alpha;
				float b_b_ = b_b * background_alpha;

				float r_f_ = foreground_red * foreground_alpha;
				float g_f_ = foreground_green * foreground_alpha;
				float b_f_ = foreground_blue * foreground_alpha;
				
				float rr = r_b_ * (1 - foreground_alpha) + r_f_;
				float gg = g_b_ * (1 - foreground_alpha) + g_f_;
				float bb = b_b_ * (1 - foreground_alpha) + b_f_;

				float r = rr / alpha;
				float g = gg / alpha;
				float b = bb / alpha;
				if (r > 1){
					r = 1.0f;
				}
				if (g > 1){
					g = 1.0f;
				}
				if (b > 1){
					b = 1.0f;
				}

				int featheredColor = new Color(r, g, b).getRGB();

				poisson.setRGB(x, y, featheredColor);

			}
		}
		return poisson;
	}

	//Function to draw blended laplacians using weighted average method.
	public void poisson_editing_weighted_average(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask){
		image = new BufferedImage(fg.getWidth(), fg.getHeight(), BufferedImage.TYPE_INT_RGB);
		image = poisson_blend_weighted_average(fg, bg, foreground_mask, background_mask);
		repaint();
	}

	//Function to draw blended laplacians using feathering method.
	public void poisson_editing_feather(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask){
		image = new BufferedImage(fg.getWidth(), fg.getHeight(), BufferedImage.TYPE_INT_RGB);
		image = poisson_blend_feather(fg, bg, foreground_mask, background_mask);
		repaint();
	}

	/*Function to generate the jacobi conversion from laplacian map to final image.
		Function takes the blended laplacian (using weighted average) and first iteration input image (background).
		Function calculated RGB values using jacobi method equations.
	*/
	public BufferedImage jacobi(BufferedImage laplacian, BufferedImage ads){
		int imageWidth = laplacian.getWidth();
		int imageHeight = laplacian.getHeight();
		int i = 0;
		jacobi_image = ads;
		BufferedImage finalOutput = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = image.createGraphics();

		//K number of iterations
		for (int k = 0; k < 1; k++){
			for (int x = 0; x < imageWidth; x++){
				for (int y = 0; y < imageHeight; y++){
					Color b_i_j_color = new Color(laplacian.getRGB(x, y));
					float b_i_j_red = b_i_j_color.getRed() / div;
					float b_i_j_green = b_i_j_color.getGreen() / div;
					float b_i_j_blue = b_i_j_color.getBlue() / div;


					float f_i_plus_1_j_red = b_i_j_red;
					float f_i_plus_1_j_green = b_i_j_green;
					float f_i_plus_1_j_blue = b_i_j_blue;

					float f_i_minus_1_j_red = b_i_j_red;
					float f_i_minus_1_j_green = b_i_j_green;
					float f_i_minus_1_j_blue = b_i_j_blue;

					float f_i_j_plus_1_red = b_i_j_red;
					float f_i_j_plus_1_green = b_i_j_green;
					float f_i_j_plus_1_blue = b_i_j_blue;

					float f_i_j_minus_1_red = b_i_j_red;
					float f_i_j_minus_1_green = b_i_j_green;
					float f_i_j_minus_1_blue = b_i_j_blue;
					
					try{
						Color f_i_plus_1_j_color = new Color(jacobi_image.getRGB(x+1, y));
						f_i_plus_1_j_red = f_i_plus_1_j_color.getRed() / div;
						f_i_plus_1_j_green = f_i_plus_1_j_color.getGreen() / div;
						f_i_plus_1_j_blue = f_i_plus_1_j_color.getBlue() / div;
					}catch(Exception e){
						// System.out.println("f_i_plus_1_j: x: " + x + " y: " + y);
					}
					
					try{
						Color f_i_minus_1_j_color = new Color(jacobi_image.getRGB(x-1, y));
						f_i_minus_1_j_red = f_i_minus_1_j_color.getRed() / div;
						f_i_minus_1_j_green = f_i_minus_1_j_color.getGreen() / div;
						f_i_minus_1_j_blue = f_i_minus_1_j_color.getBlue() / div;
					}catch(Exception e){
						// System.out.println("f_i_minus_1_j: x: " + x + " y: " + y);
						
					}

					try{
						Color f_i_j_plus_1_color = new Color(jacobi_image.getRGB(x, y+1));
						f_i_j_plus_1_red = f_i_j_plus_1_color.getRed() / div;
						f_i_j_plus_1_green = f_i_j_plus_1_color.getGreen() / div;
						f_i_j_plus_1_blue = f_i_j_plus_1_color.getBlue() / div;
					}catch(Exception e){
						// System.out.println("f_i_j_plus_1: x: " + x + " y: " + y);
					}

					try{
						Color f_i_j_minus_1_color = new Color(jacobi_image.getRGB(x, y-1));
						f_i_j_minus_1_red = f_i_j_minus_1_color.getRed() / div;
						f_i_j_minus_1_green = f_i_j_minus_1_color.getGreen() / div;
						f_i_j_minus_1_blue = f_i_j_minus_1_color.getBlue() / div;
					}catch(Exception e){
						// System.out.println("f_i_j_minus_1: x: " + x + " y: " + y);
					}

					float map_red = (f_i_plus_1_j_red + f_i_minus_1_j_red + f_i_j_plus_1_red + f_i_j_minus_1_red - b_i_j_red) / 4f;
					float map_green = (f_i_plus_1_j_green + f_i_minus_1_j_green + f_i_j_plus_1_green + f_i_j_minus_1_green - b_i_j_green) / 4f;
					float map_blue = (f_i_plus_1_j_blue + f_i_minus_1_j_blue + f_i_j_plus_1_blue + f_i_j_minus_1_blue - b_i_j_blue) / 4f;
					
					float r = Math.abs(map_red);
					float g = Math.abs(map_green);
					float b = Math.abs(map_blue);
				
					if (r > 1){
						r = 1f;
					}
					if (g > 1){
						g = 1f;
					}
					if (b > 1){
						b = 1f;
					}
					if (r < 0){
						r = 0f;
					}
					if (g < 0){
						g = 0f;
					}
					if (b < 0){
						b = 0f;
					}
					int newColor = new Color(r, g, b).getRGB();
					jacobi_image.setRGB(x, y, newColor);
				}
			}
		}
		return jacobi_image;
	}

	//Function to draw jacobi result on canvas.
	public void get_jacobi(BufferedImage fg, BufferedImage bg, BufferedImage foreground_mask, BufferedImage background_mask, BufferedImage map){
		//Get weighted average of foreground and background laplacian maps.
		BufferedImage poisson_blend = poisson_blend_weighted_average(fg, bg, foreground_mask, background_mask);
		//Get weighted average of foreground and background images.
		BufferedImage image_weighted_average = weighted_average(fg, bg, foreground_mask, background_mask);
		//Calculate final image using jacobi method.
		BufferedImage jacobi_blend = jacobi(poisson_blend, image_weighted_average);
		image = jacobi_blend;
		repaint();
	}
	
	// listen to mouse click
	class ClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			if ( e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON3 )
				try {
					ImageIO.write(image, "png", new File("saved.png"));
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
		}
	}
}
