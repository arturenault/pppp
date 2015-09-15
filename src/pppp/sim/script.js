function draw_plaza(ratio)
{
	// clear and check canvas
	var canvas = document.getElementById("canvas");
	var ctx = canvas.getContext("2d");
	ctx.clearRect(0, 0, canvas.width, canvas.height);
	if (canvas.width !== canvas.height)
		throw "Canvas is not a square: " + canvas.width + " x " + canvas.height;
	// compute sizes of square
	var outer_side = canvas.width / (ratio + 2.0);
	var inner_side = canvas.width - outer_side * 2.0;
	if (inner_side < outer_side)
		throw "Invalid inner to outer square ratio: " + ratio;
	var door_length = outer_side * 0.2;
	// corner rectangles
	var O = outer_side;
	var S = inner_side + outer_side;
	ctx.fillStyle = "black";
	ctx.fillRect(0, 0, O, O);
	ctx.fillRect(0, S, O, O);
	ctx.fillRect(S, 0, O, O);
	ctx.fillRect(S, S, O, O);
	// lines for entrances
	var A = outer_side;
	var B = outer_side + (inner_side - door_length) * 0.5;
	var C = outer_side + (inner_side + door_length) * 0.5;
	var D = outer_side + inner_side;
	var X = outer_side - 1.5;
	var Y = outer_side + 1.5 + inner_side;
	var L = [[X, A, X, B], [X, C, X, D],
			 [Y, A, Y, B], [Y, C, Y, D],
			 [A, X, B, X], [C, X, D, X],
			 [A, Y, B, Y], [C, Y, D, Y]];
	for (var i = 0 ; i != 8 ; ++i) {
		ctx.beginPath();
		ctx.moveTo(L[i][0], L[i][1]);
		ctx.lineTo(L[i][2], L[i][3]);
		ctx.closePath();
		ctx.lineWidth = 3;
		ctx.strokeStyle = "black";
		ctx.stroke();
	}
}

function draw_groups(ratio, groups)
{
	// check arguments
	if (groups.length != 4)
		throw "Not four groups: " + groups.length;
	// check canvas
	var canvas = document.getElementById("canvas");
	if (canvas.width != canvas.height)
		throw "Canvas is not a square: " + canvas.width + " x " + canvas.height;
	var ctx = canvas.getContext("2d");
	// compute sizes of square
	var outer_side = canvas.width / (ratio + 2.0);
	var inner_side = canvas.width - outer_side * 2.0;
	if (inner_side < outer_side)
		throw "Invalid inner to outer square ratio: " + ratio;
	// render group score
	var colors = ["red", "green", "blue", "purple"];
	var A = outer_side * 0.5;
	var B = outer_side * 1.5;
	var C = outer_side * 1.5 + inner_side;
	var P = [[B, A], [C, B], [B, C], [A, B]];
	for (var i = 0 ; i != 4 ; ++i) {
		if (groups[i].length != 2)
			 throw "Invalid group array length: " + groups[i].length;
		var x = P[i][0];
		var y = P[i][1];
		var name = groups[i][0];
		var score = Math.round(groups[i][1]);
		ctx.font = "16px Arial";
		ctx.textAlign = "center";
		ctx.strokeStyle = "black";
		ctx.strokeText(name + ": " + score, x, y);
		ctx.fillStyle = colors[i];
		ctx.fillText(name + ": " + score, x, y);
	}
}

function draw_pipers(ratio, pipers)
{
	// check argument
	if (pipers.length != 4)
		throw "Not four groups: " + pipers.length;
	// check canvas
	var canvas = document.getElementById("canvas");
	if (canvas.width != canvas.height)
		throw "Canvas is not a square: " + canvas.width + " x " + canvas.height;
	var ctx = canvas.getContext("2d");
	// compute sizes of square
	var outer_side = canvas.width / (ratio + 2.0);
	var inner_side = canvas.width - outer_side * 2.0;
	if (inner_side < outer_side)
		throw "Invalid inner to outer square ratio: " + ratio;
	// render pipers
	var colors = ["red", "green", "blue", "purple"];
	var inner_radius = outer_side * 0.2;
	var outer_radius = outer_side;
	for (var i = 0 ; i != 4 ; ++i) {
		if (pipers[i].length == 0)
			throw "Invalid pipers array length: " + pipers[i].length;
		for (var j = 0 ; j != pipers[i].length ; ++j) {
			if (pipers[i][j].length != 3)
				throw "Invalid piper array length: " + pipers[i][j].length;
			// piper location and music indicator
			var x = pipers[i][j][0];
			var y = pipers[i][j][1];
			var z = pipers[i][j][2];
			if (x < -1.0 || x > 1.0)
				throw "Invalid piper x coordinate: " + x;
			if (y < -1.0 || y > 1.0)
				throw "Invalid piper y coordinate: " + y;
			x = (1.0 + x) * canvas.width * 0.5;
			y = (1.0 - y) * canvas.width * 0.5;
			// draw a circle and fill with group color
			ctx.beginPath();
			ctx.arc(x, y, 4, 0, Math.PI * 2.0);
			ctx.closePath();
			ctx.lineWidth = 3;
			ctx.strokeStyle = "black";
			ctx.stroke();
			ctx.fillStyle = colors[i];
			ctx.fill();
			if (z != 0) {
				// draw inner circle
				ctx.beginPath();
				ctx.arc(x, y, inner_radius, 0, Math.PI * 2.0);
				ctx.closePath();
				ctx.lineWidth = 1;
				ctx.strokeStyle = colors[i];
				ctx.stroke();
				// draw outer circle
				ctx.beginPath();
				ctx.arc(x, y, outer_radius, 0, Math.PI * 2.0);
				ctx.closePath();
				ctx.lineWidth = 1;
				ctx.strokeStyle = colors[i];
				ctx.stroke();
			}
		}
	}
}

function draw_rats(rats)
{
	// check canvas
	var canvas = document.getElementById("canvas");
	if (canvas.width != canvas.height)
		throw "Canvas is not a square: " + canvas.width + " x " + canvas.height;
	var ctx = canvas.getContext("2d");
	// render rats
	var colors = ["red", "green", "blue", "purple"];
	for (var i = 0 ; i != rats.length ; ++i) {
		if (rats[i].length != 4)
			throw "Invalid rat array length: " + rats[i].length;
		// rat location and dominant tune
		var x = rats[i][0];
		var y = rats[i][1];
		var z = rats[i][2];
		var w = rats[i][3];
		if (x < -1.0 || x > 1.0)
			throw "Invalid rat x coordinate: " + x;
		if (y < -1.0 || y > 1.0)
			throw "Invalid rat y coordinate: " + y;
		if (z < 0.0 || z >= Math.PI * 2.0)
			throw "Invalid rat angle: " + z;
		x = (1.0 + x) * canvas.width * 0.5;
		y = (1.0 - y) * canvas.width * 0.5;
		// find moving coordinates using angle
		var dx = +Math.cos(z);
		var dy = -Math.sin(z);
		var color = null;
		if (w == 0 || w == 1 || w == 2 || w == 3)
			color = colors[Math.round(w)];
		// draw circles for body of rat
		ctx.beginPath();
		ctx.arc(x, y, 2.5, 0, Math.PI * 2.0);
		ctx.closePath();
		ctx.lineWidth = 2;
		ctx.strokeStyle = "black";
		ctx.stroke();
		if (color != null) {
			ctx.fillStyle = color;
			ctx.fill();
		}
		// draw circle for head of rat
		ctx.beginPath();
		ctx.arc(x + dx * 4.5, y + dy * 4.5, 1, 0, Math.PI * 2.0);
		ctx.closePath();
		ctx.lineWidth = 2;
		ctx.strokeStyle = "black";
		ctx.stroke();
		if (color != null) {
			ctx.fillStyle = color;
			ctx.fill();
		}
		// draw line for tail of rat
		ctx.beginPath();
		ctx.moveTo(x - dx * 3.0, y - dy * 3.0);
		ctx.lineTo(x - dx * 9.5, y - dy * 9.5);
		ctx.closePath();
		ctx.lineWidth = 0.75;
		ctx.strokeStyle = "black";
		ctx.stroke();
	}
}

function parse_number(x)
{
	if (isNaN(parseFloat(x)) || !isFinite(x))
		throw "Invalid number: " + x;
	return +x;
}

function parse_pipers(data)
{
	if ((data.length - 2) % 3 != 0)
		throw "Invalid piper data length: " + data.length;
	var pipers = new Array((data.length - 2) / 3);
	for (var i = 0 ; i != pipers.length ; ++i) {
		var x = parse_number(data[i * 3 + 2]);
		var y = parse_number(data[i * 3 + 3]);
		var z = parse_number(data[i * 3 + 4]);
		pipers[i] = [x, y, z];
	}
	return pipers;
}

function parse_rats(data)
{
	if (data.length % 4 != 0) {
		if (data.length == 1 && !data[0]) return [];
		throw "Invalid rat data length: " + data.length;
	}
	var rats = new Array(data.length / 4);
	for (var i = 0 ; i != rats.length ; ++i) {
		var x = parse_number(data[i * 4 + 0]);
		var y = parse_number(data[i * 4 + 1]);
		var z = parse_number(data[i * 4 + 2]);
		var w = parse_number(data[i * 4 + 3]);
		rats[i] = [x, y, z, w];
	}
	return rats;
}

function process(data)
{
	data = data.split("\n");
	if (data.length != 7)
		throw "Invalid data format: " + data.length;
	for (var i = 1 ; i != 6 ; ++i)
		data[i] = data[i].split(",");
	var ratio = parse_number(data[0]);
	var groups = [[data[1][0].trim(), parse_number(data[1][1])],
	              [data[2][0].trim(), parse_number(data[2][1])],
	              [data[3][0].trim(), parse_number(data[3][1])],
	              [data[4][0].trim(), parse_number(data[4][1])]];
	var pipers = [parse_pipers(data[1]),
	              parse_pipers(data[2]),
	              parse_pipers(data[3]),
	              parse_pipers(data[4])];
	var rats = parse_rats(data[5]);
	var refresh = parse_number(data[6]);
	if (refresh < 0.0) refresh = -1;
	else refresh = Math.round(refresh);
	draw_plaza(ratio);
	draw_groups(ratio, groups);
	draw_pipers(ratio, pipers);
	draw_rats(rats);
	return refresh;
}

function ajax(version)
{
	var xhr = new XMLHttpRequest();
	xhr.onload = (function() {
		var refresh = -1;
		try {
			if (xhr.readyState != 4)
				throw "Incomplete HTTP request: " + xhr.readyState;
			if (xhr.status != 200)
				throw "Invalid HTTP status: " + xhr.status;
			if (xhr.responseText)
				refresh = process(xhr.responseText);
			else
				console.log("Ignoring AJAX retry: " + version);
		} catch (message) {
			console.log("Application error: " + message);
			console.log(xhr.responeText);
			alert(message);
		}
		if (refresh >= 0)
			setTimeout(function() { ajax(version + 1); }, refresh);
	});
	xhr.onabort = (function() {
		console.log("AJAX abort: " + version);
		ajax(version);
	});
	xhr.onerror = (function() {
		console.log("AJAX error: " + version);
		ajax(version);
	});
	xhr.ontimeout = (function() {
		console.log("AJAX timeout: " + version);
		ajax(version);
	});
	xhr.open("GET", version + ".dat", true);
	xhr.responseType = "text";
	xhr.timeout = 100;
	xhr.send();
}

ajax(0);
