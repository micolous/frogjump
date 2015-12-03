// https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Math/round#Decimal_rounding

(function() {
	/**
	 * Decimal adjustment of a number.
	 *
	 * @param {String}	type	The type of adjustment.
	 * @param {Number}	value The number.
	 * @param {Integer} exp	 The exponent (the 10 logarithm of the adjustment base).
	 * @returns {Number} The adjusted value.
	 */
	function decimalAdjust(type, value, exp) {
		// If the exp is undefined or zero...
		if (typeof exp === 'undefined' || +exp === 0) {
			return Math[type](value);
		}
		value = +value;
		exp = +exp;
		// If the value is not a number or the exp is not an integer...
		if (isNaN(value) || !(typeof exp === 'number' && exp % 1 === 0)) {
			return NaN;
		}
		// Shift
		value = value.toString().split('e');
		value = Math[type](+(value[0] + 'e' + (value[1] ? (+value[1] - exp) : -exp)));
		// Shift back
		value = value.toString().split('e');
		return +(value[0] + 'e' + (value[1] ? (+value[1] + exp) : exp));
	}

	// Decimal round
	if (!Math.round10) {
		Math.round10 = function(value, exp) {
			return decimalAdjust('round', value, exp);
		};
	}
})();

var map = L.map('map');
var mkrHere = null;
var mkrPicker = null;
var cclHere = null;

map.attributionControl.setPrefix('');

L.tileLayer("http://otile{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png", {
	subdomains: "1234",
	attribution: "&copy; <a href='http://www.openstreetmap.org/'>OpenStreetMap</a> and contributors, under an <a href='http://www.openstreetmap.org/copyright' title='ODbL'>open license</a>. Tiles Courtesy of <a href='http://www.mapquest.com/'>MapQuest</a> <img src='http://developer.mapquest.com/content/osm/mq_logo.png'>"
}).addTo(map);

function onMarkerClick(e) {
	window.location = 'geo:' + Math.round10(e.latlng.lat, -6) + ',' + Math.round10(e.latlng.lng, -6);
}

function onLocationFound(e) {
	if (mkrHere == null) {
		mkrHere = L.marker(e.latlng)
			.addTo(map)
			.on('click', onMarkerClick);
		cclHere = L.circle(e.latlng, radius).addTo(map);

		// This is the first time, stop panning after this.
		map.stopLocate();
		map.locate({maxZoom: 16, watch: true});
	} else {
		mkrHere.setLatLng(e.latlng).update();
		cclHere.setLatLng(e.latlng).setRadius(radius).update();
	}
}

function onLocationError(e) {
	console.log(e.message);
}

function onClick(e) {
	if (mkrPicker == null) {
		mkrPicker = L.marker(e.latlng)
			.addTo(map)
			.on('click', onMarkerClick);
	} else {
		mkrPicker.setLatLng(e.latlng).update();
	}
}

map.on('locationfound', onLocationFound);
map.on('locationerror', onLocationError);
map.on('click', onClick);

map.locate({setView: true, maxZoom: 16});

