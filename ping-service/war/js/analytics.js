$(document).ready(function() {
	$(".c-hb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			hideSummaryCharts();
			showPrimaryChart();
			plotHistogramChart($(this), "#chart", "Response Time for Period, ms");
		});
	});
	$(".c-pb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			hideSummaryCharts();
			showPrimaryChart();
			plotPieChart($(this), "#chart", "Availability Chart for Period");
		});
	});
	$(".c-m").click(function() {
		hideSummaryCharts();
		showPrimaryChart();
		plotLineChart($(this), "#chart");
	});
});

function plotHistogramChart($this, chartId, chartTitle) {
	setChartTitle(chartId, chartTitle);
	var jsonData = eval('(' + $this.parent().attr("data-json") + ')');
		var d = [];
		var ticks = [];
		
		var intIdx;
		
		ticks.push([0, "0"]);
		for (var idx in jsonData.keys) {
			intIdx = parseInt(idx);
			d.push([intIdx, jsonData.values[intIdx]]);
			var key = jsonData.keys[intIdx];
			ticks.push([intIdx + 1, key.substring(key.indexOf(";") + 1, key.lastIndexOf("."))]);
		}
		
		intIdx++;
		
		d.push([intIdx, jsonData.others]);
		ticks.push([intIdx + 1, "others"]);
		
		$.plot($(chartId), [{ 
			data: d, 
			bars: { show: true }
		}], 
		{
			xaxis: {
				ticks: ticks,
				autoscaleMargin: 0.02
			}
		});
}

function plotPieChart($this, chartId, chartTitle) {
	setChartTitle(chartId, chartTitle);
	
	var jsonData = eval('(' + $this.parent().attr("data-json") + ')');

	var intIdx;
	var key;

	var pieLabels = [];
	pieLabels[1]  = 'No data';
	pieLabels[2]  = 'Okay';
	pieLabels[4]  = 'Timeouts';
	pieLabels[8]  = 'HTTP Errors';
	pieLabels[16] = 'Regexp Failures';
	
	var pieColors = [];
	pieColors[1]  = '#999';
	pieColors[2]  = '#4da74d';
	pieColors[4]  = '#edc240';
	pieColors[8]  = '#cb4b4b';
	pieColors[16] = '#afd8f8';

	var actualColors = [];
	
	var data = [];
		for (var idx in jsonData.keys)
	{
		intIdx = parseInt(idx);
		key = parseInt(jsonData.keys[intIdx]);
		data[data.length] = { label: pieLabels[key], data: jsonData.values[intIdx] };
		actualColors[actualColors.length] = pieColors[key];
	}

	$.plot($(chartId), data,
		{
			colors: actualColors,
	        series: {
	            pie: {
	                show: true
	            }
	        },
	        legend: {
	            show: false
	        }
		});
}

function plotLineChart($this, chartId, chartTitle) {
	$(".c-sm").removeClass("c-sm");
	$(".c-sh").removeClass("c-sh");
	$(".c-sd").removeClass("c-sd");
	
	if (!$this.is(".c-n")) {
		return;	//	not a numeric measure
	}

	var classes = $this.attr("class").split(' ');
	
	var thisClass = classes[0];

	var parts = thisClass.split('-');

	var columnClass = classes[classes.length - 3];

	var parentClass = classes[1];
	var offset = 0;
	if (parentClass[0] == "x") {
		if (parts.length - parentClass.split('-').length == 1) {
			offset = 1;
		}
	}
	
	var $dimension = $($("." + thisClass).parent().parent().children().children().get(parts.length-2+offset));
	$dimension.addClass("c-sh");

	var $aggregateLabel = $(".a" + columnClass);
	var $measureLabel = $("#" + $aggregateLabel.attr("class").split(' ')[1]);

	$aggregateLabel.addClass("c-sh");
	$measureLabel.addClass("c-sh");
	
	if (!chartTitle) {
		var aggregateLabel = $aggregateLabel.html();
		aggregateLabel = aggregateLabel[0].toUpperCase() + aggregateLabel.substring(1);
		
		chartTitle = aggregateLabel + " of " + $measureLabel.html() + " for Period by " + $dimension.html();
	}
	setChartTitle(chartId, chartTitle);

	var colspan = $($this.parent().children().get(0)).attr("colspan");
	
	$("." + columnClass).each(function() {
		var $this = $(this);
		var classes = $this.attr("class").split(' ');
		var _colspan = $($this.parent().children().get(0)).attr("colspan");
		var dimensionClass = classes[classes.length-2];
		
		if (colspan == _colspan) {
			$this.addClass("c-sm");
			$("#" + dimensionClass).addClass("c-sd");
		}
	});
	
	var d = [];
	var ticks = [];
	
	$dimensions = $(".c-sd");
	$values = $(".c-sm");
	
	for (var i = 0; i < $dimensions.length; i++) {
		ticks.push([i, $($dimensions.get(i)).html()]);
		d.push([i, parseFloat($($values.get(i)).html().replace(',', '.'))]);
	}

	$.plot($(chartId), [{ 
		data: d,
		lines: { show: true },
		points: { show: true }
	}], 
	{
		xaxis: {
			ticks: ticks,
			autoscaleMargin: 0.02
		}
	});
}

function setChartTitle(chartId, chartTitle) {
	var $chartTitle = $(chartId).parent().children().filter(".chartTitle");
	$chartTitle.text(chartTitle);
}