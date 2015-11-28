google.load('visualization', '1.1', {'packages':['corechart','scatter','annotationchart','treemap']});
google.load('visualization', '1', {'packages':['bar']});
google.setOnLoadCallback(drawChart);

function drawChart() {
    $.ajax({url: "/githistory/commits/date-wise",
              dataType:"json",
              }).done(drawDateWiseCommitChart);

    $.ajax({url: "/githistory/commits/time-wise",
            dataType:"json",
            }).done(drawTimeWiseCommitChart);

    $.ajax({url: "/githistory/commits/date-wise-stats",
                dataType:"json",
                }).done(updateDateWiseStats);

    $.ajax({url: "/githistory/commits/project-wise",
                    dataType:"json",
                    }).done(updateProjectWiseStats);

    $.ajax({url: "/githistory/commits/reverts",
                    dataType:"json",
                    }).done(updateRevertStats);

    $.ajax({url: "/githistory/commits/size-wise",
                        dataType:"json",
                        }).done(updateSizeWiseStats);

    $.ajax({url: "/githistory/commits/developers",
                            dataType:"json",
                            }).done(updateDeveloperStats);


    $.ajax({url: "/githistory/commits/reviewers",
                                dataType:"json",
                                }).done(updateReviewerStats);

    $.ajax({url: "/githistory/commits/developer-day-of-week",
                                dataType:"json",
                                }).done(updateDeveloperDayPrefStats);

    $.ajax({url: "/githistory/commits/release-composition",
                                    dataType:"json",
                                    }).done(updateReleaseComposition);
}

function updateReleaseComposition(jsonData){
        var data = new google.visualization.DataTable();
        data.addColumn('string','Release');
        $.each(jsonData.header,function(i,v){
             data.addColumn('number',v);
        });

        $.each(jsonData,function(k,v){
            if(k !=='header'){
                v.unshift(k);
                data.addRow(v);
            }
        });

        var options = {width: 1200, height:500,isStacked: true,
        legend: { position: 'none' },hAxis:{slantedText:true},
                vAxis:{title: 'Commits By Project'}};

       var chart=new google.visualization.ColumnChart(document.getElementById('release-composition'));
       chart.draw(data,options);
}

function updateDeveloperDayPrefStats(jsonData){
    var data = new google.visualization.DataTable();
    data.addColumn('string','Day of Week');
    data.addColumn('string','Developers');
    $.each(jsonData,function(k,v){
           data.addRow([k,v]);
      });

    var cssClassNames = {
        'headerRow': 'italic-darkblue-font large-font bold-font',
        'tableRow': '',
        'oddTableRow': 'beige-background',
        'selectedTableRow': 'orange-background large-font',
        'hoverTableRow': '',
        'headerCell': 'gold-border',
        'tableCell': ''};

    var options = {'showRowNumber': true, 'allowHtml': true, 'cssClassNames': cssClassNames};
    var chart=new google.visualization.Table(document.getElementById('commit-dev-day'));
    chart.draw(data,options);
}

function updateDeveloperStats(jsonData){
    var words=[];
    $.each(jsonData,function(k,v){
       words.push({text: k, weight:v});
    });
    $("a[href='#developers']").on('shown.bs.tab', function(){
        $('#commit-developers').jQCloud('destroy');
        $('#commit-developers').jQCloud(words,{autoResize:true,height:500});
    });
}

function updateReviewerStats(jsonData){
    var words=[];
    $.each(jsonData,function(k,v){
       words.push({text: k, weight:v});
    });

     $("a[href='#developers']").on('shown.bs.tab', function(){
            $('#commit-reviewers').jQCloud('destroy');
            $('#commit-reviewers').jQCloud(words,{autoResize:true,height:500});
        });
}

function updateDateWiseStats(jsonData){
    $("#total-commits").text(jsonData.total);
    $("#total-reverts").text(jsonData.reverts);
    $("#git-branch").text(jsonData.branch);
    $("#date-range").text(jsonData.dateRange);
    $("#team-name").text(jsonData.team);
}

function updateProjectWiseStats(jsonData){
    var data = new google.visualization.DataTable();
    data.addColumn('string','Project');
    data.addColumn('number','Count');

    $.each(jsonData,function(k,v){
        data.addRow([k,v]);
    });

    var options = {height: 500, width: 500,
    legend: { position: "none" }};

   var chart=new google.visualization.BarChart(document.getElementById('commit-chart-project-wise'));
   chart.draw(data,options);
}

function updateRevertStats(jsonData){
    var data = new google.visualization.DataTable();
    data.addColumn('string','Project');
    data.addColumn('number','Revert Commits');

    $.each(jsonData,function(k,v){
        data.addRow([k,v]);
    });

    var options = {legend: { position: "none" }};

   var chart=new google.visualization.Table(document.getElementById('commit-reverts'));
   chart.draw(data);
}


function updateSizeWiseStats(jsonData){
    var data = new google.visualization.DataTable();
    data.addColumn('string','Ticket');
    data.addColumn('number','Count');

    $.each(jsonData,function(k,v){
        data.addRow([k,v]);
    });

    var options = {width: 600, height: 500,
    legend: { position: "none" },histogram: { lastBucketPercentile: 5 },hAxis:{showTextEvery: 2,
    title: 'File Modification Count(Add,Del,Change)'},
                vAxis: { title: 'Commits'}};

   var chart=new google.visualization.Histogram(document.getElementById('commit-chart-size-wise'));
   chart.draw(data,options);
}

function drawTimeWiseCommitChart(jsonData){
    var data = new google.visualization.DataTable();
    data.addColumn('string','Commit');
    data.addColumn('string','Parent');
    data.addColumn('number','Value');

    for	(index = 0; index < jsonData.length; index++) {
        data.addRow([jsonData[index][0],jsonData[index][1],parseInt(jsonData[index][2])]);
    }

    var options = {width: 1000, height: 500,
                 maxDepth: 1,maxPostDepth: 2, fontColor: '#ffff00',fontSize: '16',
                 minColor: '#009688',midColor: '#f7f7f7',maxColor: '#ee8100', showTitle: false,
                 headerHeight: 0,showScale: true,showToolTip:false,
                 useWeightedAverageForAggregation: true,
                 generateTooltip: function(row,size,value){return '<div style="background:#fd9; padding:10px; border-style:solid">' +
                                                                             'Total Commits: '+size+'</div>'}};

   var chart=new google.visualization.TreeMap(document.getElementById('commit-chart-daytime-table'));
   chart.draw(data,options);
}


function drawDateWiseCommitChart(jsonData){
    var data = new google.visualization.DataTable(jsonData);
    data.addColumn('date', 'Date');
    data.addColumn('number', 'Commits');
    data.addColumn('string', 'Tag');
    $.each(jsonData,function(k,v){
       var m=moment(k,'YYYY-MM-DD');
       v=v.split(":");
       data.addRow([m.toDate(),parseInt(v[0]),v[1]]);
    });
    var options = {
              displayAnnotations: true,
              width: 1200,
              height: 500
            };
   var chart = new google.visualization.AnnotationChart(document.getElementById('commit-chart-date-wise-annotation'));
   chart.draw(data,options);
}

