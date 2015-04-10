/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package misc;

import org.micromanager.MMStudio;

/**
 * Means for plugin classes to access the core, to support tricky things
 */
public class CoreCommunicator {

   public static int getImageWidth() {
      return (int) MMStudio.getInstance().getCore().getImageWidth();
   }

   public static int getImageHeight() {
      return (int) MMStudio.getInstance().getCore().getImageHeight();
   }

   public static void main(String[] args) {
      int[] lut = getCosineWarpLUT();
      System.out.println();
   }

   //TODO:
   //0) change camera files
   //1) Do deinterlacing in Java
   //2) add rank filtering
   //3) try summing edge pixels to alleviate flat fielding (compare # pixels tosses to linescan from flat field slide)
   //   -but you would hve to subtract the offset?
   //4) java layer image accumulater
   //5) put images into circular buffer when in a certain mode
   //6) why 1400 pix per line?
   
   
   
   /**
    * This function operates on an interlaced, but still warped image its
    * purpose is to generate a pixel LUT with unwarped pixel indices and 
    * warped pixel values
    * @return
    */
   public static int[] getCosineWarpLUT() {
      //on gen3, 1520 pixels per line, throw away 40 at beginning and 44 at the end giving a double wide image width of 1440
      int pixPerLine = 1400;
      int doubleWideWidth = 1400;
      int lineStartThrowawayPixels = 0;
      int interlacedWidth = doubleWideWidth / 2;
      //each pixel corresponds to the same amount of time, but a different amount of space
      //at the very center of the image, there will be a 1-to-1 correpondance of warped
      //pixels and unwarped pixels, at the edges, there will be several unwarped pixels
      //for each warped one
      double radiansPerPix = 2 * Math.PI / (double) pixPerLine;

      int[] lut = new int[doubleWideWidth / 2];

      //find center pixel, i.e. the pixel where distortion is minimized, and 1 pixel
      //in warped image corresponds to one pixel in unwarped image
      //this LUT generated by this function operates on images that have already been deinterlaced.
      //Assuming the offset is correct, the pixelPerLine will start at phase 0 and end at 2pi
      //so the center pixel should be at pi/2
      int centerPixel = pixPerLine / 4 - lineStartThrowawayPixels;
      int lutOffset = -1; //used to 0 base indices since theyre calculated form image center
      double[] warpedPixPerPix = new double[interlacedWidth];
      for (int warpedPixIndex = 0; warpedPixIndex < interlacedWidth; warpedPixIndex++) {
         double angle = ((warpedPixIndex+0.5) + lineStartThrowawayPixels) * radiansPerPix; //add 0.5 to calculate from center of image
         //represents the relative speed of the mirror at this pixel position
         double relativeSpeedOfMirror = Math.cos(angle - Math.PI / 2);
         //so the inverse of speed is the number of warped pixels per a single pixel in the unwarped image
         warpedPixPerPix[warpedPixIndex] = 1 / relativeSpeedOfMirror;
//         System.out.println(warpedPixPerPix[warpedPixIndex]);
         //correction factor = angular displacemnt / cos(angular displacement - pi/2)
         double displacement = angle - Math.PI/2;
         double correctionFactor = displacement / Math.cos(displacement - Math.PI / 2);
         
//         System.out.println((warpedPixIndex - centerPixel) + "\t" + ((warpedPixIndex - centerPixel) /correctionFactor));
         //make sure lutValues start at 0
         int lutValue = centerPixel + (int)((warpedPixIndex - centerPixel) /correctionFactor); 
         if (lutOffset == -1) {
            lutOffset = lutValue;
         }
         lutValue -= lutOffset;
         System.out.println( lutValue );
         lut[warpedPixIndex] = lutValue;
      }
      return lut;
   }
}
