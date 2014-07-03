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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tilegen.datasets



import java.lang.{Double => JavaDouble}

import scala.reflect.ClassTag

import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex

import com.oculusinfo.tilegen.tiling.AnalysisDescription
import com.oculusinfo.tilegen.tiling.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.CompositeAnalysisDescription
import com.oculusinfo.tilegen.tiling.MinimumDoubleTileAnalytic
import com.oculusinfo.tilegen.tiling.MaximumDoubleTileAnalytic
import com.oculusinfo.tilegen.util.PropertiesWrapper



object CSVDataAnalyticExtractor {
	def fromProperties[IT, PT] (properties: PropertiesWrapper,
	                                indexType: ClassTag[IT],
	                                processingType: ClassTag[PT]):
			AnalysisWithTag[(IT, PT), _] = {
		val analysis: Option[AnalysisDescription[(IT, PT), Int]] = None
		new AnalysisWithTag[(IT, PT), Int](analysis)
	}
}

object CSVTileAnalyticExtractor {
	def fromProperties[BT] (sc: SparkContext,
	                        properties: PropertiesWrapper,
	                        valuer: CSVValueExtractor[_, BT],
	                        levels: Seq[Seq[Int]]):
			AnalysisWithTag[TileData[BT], _] =
	{
		val metaDataKeys = (levels.flatMap(lvls => lvls).toSet.map((level: Int) =>
			                    (""+level -> ((index: TileIndex) => (index.getLevel == level)))
		                    ) + ("global" -> ((index: TileIndex) => true))
		).toMap

		val binType = ClassTag.unapply(valuer.valueTypeTag).get

		if (binType == classOf[Double]) {
			val convertFcn: BT => Double = bt => bt.asInstanceOf[Double]
			val minAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Double](sc,
				                                               convertFcn,
				                                               new MinimumDoubleTileAnalytic,
				                                               metaDataKeys)
			val maxAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Double](sc,
				                                               convertFcn,
				                                               new MaximumDoubleTileAnalytic,
				                                               metaDataKeys)
			new AnalysisWithTag(Some(new CompositeAnalysisDescription(minAnalytic, maxAnalytic)))
		} else {
			new AnalysisWithTag[TileData[BT], Int](None)
		}
	}
}

class AnalysisWithTag[BT, AT: ClassTag] (val analysis: Option[AnalysisDescription[BT, AT]]) {
	val analysisTypeTag: ClassTag[AT] = implicitly[ClassTag[AT]]
}
