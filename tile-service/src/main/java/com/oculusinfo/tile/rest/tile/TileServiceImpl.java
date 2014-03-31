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
package com.oculusinfo.tile.rest.tile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.util.AvroJSONConverter;

/**
 * @author dgray
 *
 */
@Singleton
public class TileServiceImpl implements TileService {
	private static final Logger _logger = LoggerFactory.getLogger(TileServiceImpl.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);

	
	private Map<String, JSONObject> _metadataCache;
	private Map<UUID, JSONObject>   _uuidToOptionsMap;
	private Map<String, UUID>       _latestIDMap;

	@Inject
	private FactoryProvider<PyramidIO> _pyramidIOFactoryProvider;
	@Inject
	private FactoryProvider<TileSerializer<?>> _serializationFactoryProvider;
	@Inject
	private FactoryProvider<TileDataImageRenderer> _rendererFactoryProvider;

	public TileServiceImpl () {
		_metadataCache = Collections.synchronizedMap(new HashMap<String, JSONObject>());
		_uuidToOptionsMap = Collections.synchronizedMap(new HashMap<UUID, JSONObject>());
		_latestIDMap = Collections.synchronizedMap(new HashMap<String, UUID>());
	}

	/*
	 * Returns an uninitialized render parameter factory
	 */
	private LayerConfiguration getLayerConfiguration () throws ConfigurationException {
		return new LayerConfiguration(_pyramidIOFactoryProvider,
		                              _serializationFactoryProvider,
		                              _rendererFactoryProvider, null,
		                              new ArrayList<String>());
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getLayer(String)
	 */
	public JSONObject getLayer(String hostUrl, JSONObject options) {
		try {
			
			UUID id = UUID.randomUUID();
			String layer = options.getString("layer");
			_uuidToOptionsMap.put(id, options);
			_latestIDMap.put(layer, id);

			// Determine the pyramidIO, so we can get the metaData
			LayerConfiguration config = getLayerConfiguration();
			config.readConfiguration(options);
			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			PyramidMetaData metadata = getMetadata(layer, pyramidIO);

			// Construct our return object
			String[] names = JSONObject.getNames(metadata.getRawData());
			JSONObject result = new JSONObject(metadata.getRawData(), names);

			result.put("layer", layer);
			result.put("id", id);
			result.put("tms", hostUrl + "tile/" + id.toString() + "/");
			result.put("apertureservice", "/tile/" + id.toString() + "/");

			TileDataImageRenderer renderer = config.produce(TileDataImageRenderer.class);
			result.put("imagesPerTile", renderer.getNumberOfImagesPerTile(metadata));

			System.out.println("UUID Count after "+layer+": " + _uuidToOptionsMap.size());
			return result;
		} catch (ConfigurationException e) {
			_logger.warn("Configuration exception trying to apply layer parameters to json object.", e);
			return new JSONObject();
		} catch (JSONException e) {
			_logger.warn("Failed to apply layer parameters to json object.", e);
			return new JSONObject();
		} 

	}

	@Override
	public LayerConfiguration getLevelSpecificConfiguration (UUID id, String layer, TileIndex tile) throws ConfigurationException {
		LayerConfiguration config = getLayerConfiguration();

		if (null == id) {
			id = _latestIDMap.get(layer);
		}

		if (id != null){
			// Get rendering options
			JSONObject options = _uuidToOptionsMap.get(id);
			config.readConfiguration(options);
		} else {
			config.readConfiguration(new JSONObject());
		}

		PyramidIO pyramidIO = config.produce(PyramidIO.class);

		PyramidMetaData metadata = getMetadata(config.getPropertyValue(LayerConfiguration.LAYER_NAME), pyramidIO);
		config.setLevelProperties(tile,
		                          metadata.getLevelMinimum(tile.getLevel()),
		                          metadata.getLevelMaximum(tile.getLevel()));
		return config;
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getTile(int, double, double)
	 */
	public BufferedImage getTileImage (UUID id, String layer, int zoomLevel, double x, double y) {
		int width = 256;
		int height = 256;
		BufferedImage bi = null;

		try {
			LayerConfiguration config = getLevelSpecificConfiguration(id, layer, new TileIndex(zoomLevel, (int) x, (int) y));
    
			// Record image dimensions in case of error. 
			width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);

			TileDataImageRenderer tileRenderer = config.produce(TileDataImageRenderer.class);

			bi = tileRenderer.render(config);
		} catch (ConfigurationException e) {
			_logger.info("No renderer specified for tile request.");
		}

		if (bi == null){
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bi.createGraphics();
			g.setColor(COLOR_BLANK);
			g.fillRect(0, 0, 256, 256);
			//g.setColor(Color.red);
			//g.drawLine(1, 1, 254, 254);
			g.dispose();
		}

		return bi;
	}

	@Override
	public JSONObject getTileObject(UUID id, String layer, int zoomLevel, double x, double y) {
		TileIndex tileCoordinate = new TileIndex(zoomLevel, (int)x, (int)y);
		try {
			LayerConfiguration config = getLayerConfiguration();
			if (id != null){
				// Get rendering options
				config.readConfiguration(_uuidToOptionsMap.get(id));
			} else {
				config.readConfiguration(new JSONObject());
			}
			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			InputStream tile = pyramidIO.getTileStream(layer, tileCoordinate);
			if (null == tile) return null;
			return AvroJSONConverter.convert(tile);
		} catch (IOException e) {
			_logger.warn("Exception getting tile for {}", tileCoordinate, e);
		} catch (JSONException e) {
			_logger.warn("Exception getting tile for {}", tileCoordinate, e);
		} catch (ConfigurationException e) {
			_logger.warn("Exception getting tile for {}", tileCoordinate, e);
		}
		return null;
	}

	/**
	 * @param layer
	 * @param pyramidIo 
	 * @return
	 */
	private PyramidMetaData getMetadata (String layer, PyramidIO pyramidIO) {
		try {
			JSONObject metadata = _metadataCache.get(layer);
			if (metadata == null){
				String s = pyramidIO.readMetaData(layer);
				metadata = new JSONObject(s);
				_metadataCache.put(layer, metadata);
			}
			return new PyramidMetaData(metadata);
		} catch (JSONException e) {
			_logger.error("Metadata file for layer is missing or corrupt: "+layer, e);
		} catch (IOException e) {
			_logger.error("Couldn't read metadata: "+layer, e);
		}
		return new PyramidMetaData(new JSONObject());
	}
}