google.load('visualization', '1.0', {'packages':['corechart','calendar','annotationchart']});
google.setOnLoadCallback(drawChart);

function drawChart() {
    $.ajax({url: "http://localhost:8080/githistory/commits/date-wise",
              dataType:"json",
              }).done(drawDateWiseCommitChart);

    $.ajax({url: "http://localhost:8080/githistory/commits/month-wise",
              dataType:"json",
              }).done(drawMonthWiseCommitChart);

    $.ajax({url: "http://localhost:8080/githistory/commits/day-wise",
              dataType:"json",
              }).done(drawDayWiseCommitChart);

    $.ajax({url: "http://localhost:8080/githistory/commits/date-wise-stats",
            dataType:"json",
            }).done(updateDateWiseStats);

}

function updateDateWiseStats(jsonData){
    $("#total-commits").text(jsonData.total);
}

function drawMonthWiseCommitChart(jsonData){
}

function drawDayWiseCommitChart(jsonData){
}

function drawDateWiseCommitChart(jsonData){
    var data = new google.visualization.DataTable(jsonData);
    data.addColumn('date', 'Date');
    data.addColumn('number', 'Count');
    $.each(jsonData,function(k,v){
       var m=moment(k,'YYYY-MM-DD');
       data.addRow([m.toDate(),v]);
    });
   var chart = new google.visualization.AnnotationChart(document.getElementById('commit-chart-date-wise-calendar'));
   chart.draw(data);
}
