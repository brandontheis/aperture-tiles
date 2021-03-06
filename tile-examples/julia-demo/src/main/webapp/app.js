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

var appStart = function() {

    "use strict";

    // request layers from server
    tiles.LayerService.getLayers( function( layers ) {

        // parse layers into nicer format
        layers = tiles.LayerUtil.parse( layers.layers );

        var map,
            axis0,
            axis1,
            baseLayer,
            serverLayer;

        baseLayer = new tiles.BaseLayer({
            options: {
                color: "#000"
            }
        });

        serverLayer = new tiles.ServerLayer({
            source: layers["julia-set"]
        });

        axis0 = new tiles.Axis({
            title: 'X',
            position: 'bottom',
            enabled: true,
            intervals: {
                type: 'fixed',
                increment: 86400000,
                scaleByZoom: false,
                minPixelWidth: 80
            },
            units: {
                type: 'time'
            }
        });

        /**
         * if full marker range is less than X, double it,
         *
         *
         */

        axis1 = new tiles.Axis({
            title: 'Y',
            position: 'left',
            enabled: true,
            intervals: {
                increment: 20,
                pivot: 0
            },
            units: {
                type: 'decimal'
            }
        });

        map = new tiles.Map( "map", {
            pyramid : {
                type : "AreaOfInterest",
                minX: 1417392000000,
				maxX: 1426377600000,
				minY: -2,
				maxY: 2
            }
        });
        map.add( axis0 );
        map.add( axis1 );
        map.add( serverLayer );
        map.add( baseLayer );

        $( '.controls' ).append( tiles.LayerControls.create( [ serverLayer ] ) );
    });
}
