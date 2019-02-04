module.exports = {
  legend: {
    display: false, 
    position: 'bottom'
  },
  scales: {
    yAxes: [
      {
        type: 'linear',
        stacked: true,
        display: true,
        position: 'left',
        id: 'y-axis-bar',
        gridLines: {
          display: false
        },
        labels: {
          show: true
        },
        scaleLabel: {
          display: true,
          labelString: 'Throughput'
        }
      },
      {
        type: 'linear',
        display: true,
        position: 'right',
        id: 'y-axis-line',
        gridLines: {
          display: false
        },
        labels: {
          show: true
        },
        scaleLabel: {
          display: true,
          labelString: 'Mean Completion Time (ms)'
        }
      }
    ],
    xAxes: [{
      stacked: true,
      ticks: {
        beginAtZero: true
      }
    }]
  }
};