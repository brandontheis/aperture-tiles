/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering.impl;

        import com.oculusinfo.binning.TileData;
        import com.oculusinfo.binning.TileIndex;
        import com.oculusinfo.binning.io.PyramidIO;
        import com.oculusinfo.binning.io.serialization.TileSerializer;
        import com.oculusinfo.binning.io.transformation.FilterVarsDoubleArrayTileTransformer;
        import com.oculusinfo.binning.io.transformation.TileTransformer;
        import com.oculusinfo.binning.metadata.PyramidMetaData;
        import com.oculusinfo.binning.util.Pair;
        import com.oculusinfo.binning.util.TypeDescriptor;
        import com.oculusinfo.factory.ConfigurationException;
        import com.oculusinfo.factory.properties.StringProperty;
        import com.oculusinfo.tile.rendering.LayerConfiguration;
        import com.oculusinfo.tile.rendering.TileDataImageRenderer;
        import com.oculusinfo.tile.rendering.color.ColorRamp;
        import com.oculusinfo.tile.rendering.transformations.IValueTransformer;
        import org.codehaus.jettison.json.JSONObject;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.awt.*;
        import java.awt.image.BufferedImage;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.List;

        import org.apache.commons.lang.ArrayUtils;

/**
 * A server side to render List<Pair<String, Int>> tiles.
 *
 * This renderer by default renders the sum of all key values.
 *
 * To render select keys override
 * {@link \#getTextsToDraw(List)}.
 *
 *
 * @author mkielo
 */

public class DoubleListHeatMapImageRenderer implements TileDataImageRenderer {
	private final Logger _logger = LoggerFactory.getLogger(getClass());

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublesImageRenderer.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);

    //public <T> TileData<T> Transform (TileData<T> inputData, Class<? extends T> type)
    public static Class<List<Double>> getRuntimeBinClass () {
        return (Class<List<Double>>) new ArrayList<Double>().getClass();
    }

    public static TypeDescriptor getRuntimeTypeDescriptor () {
        return new TypeDescriptor(List.class, new TypeDescriptor(Double.class));
    }

	/**
	 * This function returns the sum of values from keys which are not being excluded
	 */

	protected long getValueSumToDraw (int[] cellData, int[] excludedIndices) {
		long sum = 0;

		for(int i = 0; i < cellData.length; i++){
			if(!ArrayUtils.contains(excludedIndices, i)){
				sum = sum + cellData[i];
			}
		}
		return sum;
	}

	protected long getValueSumToDraw (int[] cellData) {
		return getValueSumToDraw(cellData, new int[0]);
	}

    private double parseExtremum (LayerConfiguration parameter, StringProperty property, String propName, String layer, double def) {
        String rawValue = parameter.getPropertyValue(property);
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException|NullPointerException e) {
            LOGGER.warn("Bad "+propName+" value "+rawValue+" for "+layer+", defaulting to "+def);
            return def;
        }
    }

	@Override
	public Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException {
		String layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
		double minimumValue = parseExtremum(config, LayerConfiguration.LEVEL_MINIMUMS, "minimum", layer, 0.0);
		double maximumValue = parseExtremum(config, LayerConfiguration.LEVEL_MAXIMUMS, "maximum", layer, 1000.0);
		return new Pair<Double, Double>(minimumValue,  maximumValue);
	}



	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
		/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
    public BufferedImage render (LayerConfiguration config) {
        BufferedImage bi;
        String layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
        TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
        try {
            int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
            int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
            int rangeMax = config.getPropertyValue(LayerConfiguration.RANGE_MAX);
            int rangeMin = config.getPropertyValue(LayerConfiguration.RANGE_MIN);
            int coarseness = config.getPropertyValue(LayerConfiguration.COARSENESS);
            double maximumValue = getLevelExtrema(config).getSecond();

            bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

            IValueTransformer t = config.produce(IValueTransformer.class);
            int[] rgbArray = new int[outputWidth*outputHeight];

            double scaledLevelMaxFreq = t.transform(maximumValue)*rangeMax/100;
            double scaledLevelMinFreq = t.transform(maximumValue)*rangeMin/100;

            int coarsenessFactor = (int)Math.pow(2, coarseness - 1);

            PyramidIO pyramidIO = config.produce(PyramidIO.class);
            TileSerializer<List<Double>> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
                    getRuntimeBinClass(),
                    getRuntimeTypeDescriptor());

            List<TileData<List<Double>>> tileDatas = null;

            // Get the coarseness-scaled true tile index
            TileIndex scaleLevelIndex = null;
            // need to get the tile data for the level of the base level minus the courseness
            for (int coursenessLevel = coarseness - 1; coursenessLevel >= 0; --coursenessLevel) {
                scaleLevelIndex = new TileIndex(index.getLevel() - coursenessLevel,
                        (int)Math.floor(index.getX() / coarsenessFactor),
                        (int)Math.floor(index.getY() / coarsenessFactor));

                tileDatas = pyramidIO.readTiles(layer, serializer, Collections.singleton(scaleLevelIndex));
                if (tileDatas.size() >= 1) {
                    //we got data for this level so use it
                    break;
                }
            }

            // Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
            if (tileDatas.size() < 1) {
                LOGGER.info("Missing tile " + index + " for layer " + layer);
                return null;
            }

            TileData<List<Double>> data = tileDatas.get(0);
            int xBins = data.getDefinition().getXBins();
            int yBins = data.getDefinition().getYBins();

            //calculate the tile tree multiplier to go between tiles at each level.
            //this is also the number of x/y tiles in the base level for every tile in the scaled level
            int tileTreeMultiplier = (int)Math.pow(2, index.getLevel() - scaleLevelIndex.getLevel());

            int baseLevelFirstTileY = scaleLevelIndex.getY() * tileTreeMultiplier;

            //the y tiles are backwards, so we need to shift the order around by reversing the counting direction
            int yTileIndex = ((tileTreeMultiplier - 1) - (index.getY() - baseLevelFirstTileY)) + baseLevelFirstTileY;

            //figure out which bins to use for this tile based on the proportion of the base level tile within the scale level tile
            int xBinStart = (int)Math.floor(xBins * (((double)(index.getX()) / tileTreeMultiplier) - scaleLevelIndex.getX()));
            int xBinEnd = (int)Math.floor(xBins * (((double)(index.getX() + 1) / tileTreeMultiplier) - scaleLevelIndex.getX()));
            int yBinStart = ((int)Math.floor(yBins * (((double)(yTileIndex) / tileTreeMultiplier) - scaleLevelIndex.getY())) ) ;
            int yBinEnd = ((int)Math.floor(yBins * (((double)(yTileIndex + 1) / tileTreeMultiplier) - scaleLevelIndex.getY())) ) ;

            int numBinsWide = xBinEnd - xBinStart;
            int numBinsHigh = yBinEnd - yBinStart;
            double xScale = ((double) bi.getWidth())/numBinsWide;
            double yScale = ((double) bi.getHeight())/numBinsHigh;
            ColorRamp colorRamp = config.produce(ColorRamp.class);

            TileTransformer tileTransformer = config.produce(TileTransformer.class);
            TileData<List<Double>> transformedContents= tileTransformer.Transform(data, List.class);

            for(int ty = 0; ty < numBinsHigh; ty++){
                for(int tx = 0; tx < numBinsWide; tx++){
                    //calculate the scaled dimensions of this 'pixel' within the image
                    int minX = (int) Math.round(tx*xScale);
                    int maxX = (int) Math.round((tx+1)*xScale);
                    int minY = (int) Math.round(ty*yScale);
                    int maxY = (int) Math.round((ty+1)*yScale);

                    List<Double> binContents = transformedContents.getBin(tx, ty);
                    double binCount = 0;
                    for(int i = 0; i < binContents.size(); i++){
                        binCount = binCount + binContents.get(i);
                    }

                    //log/linear
                    double transformedValue = t.transform(binCount);
                    int rgb;
                    if (binCount > 0

                            && transformedValue >= scaledLevelMinFreq
                            && transformedValue <= scaledLevelMaxFreq) {
                        rgb = colorRamp.getRGB(transformedValue);
                    } else {
                        rgb = COLOR_BLANK.getRGB();
                    }

                    //'draw' out the scaled 'pixel'
                    for (int ix = minX; ix < maxX; ++ix) {
                        for (int iy = minY; iy < maxY; ++iy) {
                            int i = iy*bi.getWidth() + ix;
                            rgbArray[i] = rgb;
                        }
                    }
                }
            }

            bi.setRGB(0, 0, outputWidth, outputHeight, rgbArray, 0, outputWidth);
        } catch (Exception e) {
            LOGGER.debug("Tile is corrupt: " + layer + ":" + index);
            LOGGER.debug("Tile error: ", e);
            bi = null;
        }
        return bi;
    }


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Text score rendering always produces a single image.
		return 1;
	}
}
