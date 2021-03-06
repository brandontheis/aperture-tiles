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

package com.oculusinfo.binning.impl;


import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SubTileDataView;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


public class TileDataViewTests {

	private static TileData<Integer> source16 = new DenseTileData<>(new TileIndex(0, 0, 0, 4, 4), Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15));

	@Test
	public void testSimple () {
		SubTileDataView<Integer> underTest = SubTileDataView.fromSourceAbsolute(source16, new TileIndex(1, 1, 1));

		Assert.assertEquals(1, underTest.getDefinition().getLevel());
		Assert.assertEquals(1, underTest.getDefinition().getX());
		Assert.assertEquals(1, underTest.getDefinition().getY());
		Assert.assertEquals(2, underTest.getDefinition().getXBins());
		Assert.assertEquals(2, underTest.getDefinition().getYBins());

		Assert.assertEquals(2, (int)underTest.getBin(0,0));
		Assert.assertEquals(7, (int)underTest.getBin(1,1));
	}

	@Test
	public void testSimpleTwoLevels () {
		SubTileDataView<Integer> underTest = SubTileDataView.fromSourceAbsolute(source16, new TileIndex(2, 1, 1));

		Assert.assertEquals(2, underTest.getDefinition().getLevel());
		Assert.assertEquals(1, underTest.getDefinition().getX());
		Assert.assertEquals(1, underTest.getDefinition().getY());
		Assert.assertEquals(1, underTest.getDefinition().getXBins());
		Assert.assertEquals(1, underTest.getDefinition().getYBins());

		Assert.assertEquals(9, (int)underTest.getBin(0,0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadRelativeLevel() {
		TileData<Integer> source = new DenseTileData<>(new TileIndex(3, 0, 0));
		SubTileDataView.fromSourceAbsolute(source, new TileIndex(2, 1, 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadRelativeIndex () {
		TileData<Integer> source = new DenseTileData<>(new TileIndex(1, 0, 0));
		SubTileDataView.fromSourceAbsolute(source, new TileIndex(2, 2, 1));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutOfBoundsXBin () {
		SubTileDataView<Integer> underTest = SubTileDataView.fromSourceAbsolute(source16, new TileIndex(1, 1, 1));
		underTest.getBin(2,1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutOfBoundsYBin () {
		SubTileDataView<Integer> underTest = SubTileDataView.fromSourceAbsolute(source16, new TileIndex(1, 1, 1));
		underTest.getBin(1,2);
	}
}
